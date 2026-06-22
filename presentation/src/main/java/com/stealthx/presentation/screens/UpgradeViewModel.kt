package com.stealthx.presentation.screens

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.stealthx.domain.repository.AccessTierRepository
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.AccessTier
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayBillingProduct(
    val productId: String,
    val title: String,
    val price: String
)

data class UpgradeState(
    val currentTier: AccessTier = AccessTier.FREE,
    val products: Map<String, PlayBillingProduct> = emptyMap(),
    val status: String = "Connect to Google Play to buy Pro or Elite.",
    val isConnecting: Boolean = false
)

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tierRepository: AccessTierRepository,
    private val tierGate: TierGate
) : ViewModel(), PurchasesUpdatedListener {

    private val _state = MutableStateFlow(UpgradeState())
    val state: StateFlow<UpgradeState> = _state.asStateFlow()

    private var billingClient: BillingClient? = null
    private var productDetailsById: Map<String, ProductDetails> = emptyMap()

    init {
        viewModelScope.launch {
            tierGate.currentTier.collect { tier ->
                _state.update { it.copy(currentTier = tier) }
            }
        }
    }

    fun connect() {
        if (billingClient != null) return
        _state.update { it.copy(isConnecting = true, status = "Connecting to Google Play...") }
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    restorePurchases()
                } else {
                    _state.update {
                        it.copy(isConnecting = false, status = "Google Play Billing unavailable: ${result.debugMessage}")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.update { it.copy(status = "Google Play Billing disconnected.") }
            }
        })
    }

    fun buy(activity: Activity?, productId: String) {
        if (activity == null) {
            _state.update { it.copy(status = "Purchase unavailable from this screen.") }
            return
        }
        val details = productDetailsById[productId]
        if (details == null) {
            _state.update { it.copy(status = "Product not available in Google Play yet.") }
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                _state.update { it.copy(status = "No Google Play offer available for this product.") }
                return
            }
            productParams.setOfferToken(offerToken)
        }

        val result = billingClient?.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams.build()))
                .build()
        )
        if (result?.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update { it.copy(status = result?.debugMessage ?: "Google Play Billing is not connected.") }
        }
    }

    fun restorePurchases() {
        val client = billingClient ?: return
        _state.update { it.copy(status = "Restoring Google Play purchases...") }
        queryPurchases(client, BillingClient.ProductType.SUBS)
        queryPurchases(client, BillingClient.ProductType.INAPP)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::handlePurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update { it.copy(status = "Purchase canceled.") }
            }
            else -> _state.update { it.copy(status = "Purchase failed: ${result.debugMessage}") }
        }
    }

    private fun queryProducts() {
        productDetailsById = emptyMap()
        queryProductDetails(subscriptionSkus, BillingClient.ProductType.SUBS)
        queryProductDetails(lifetimeSkus, BillingClient.ProductType.INAPP)
    }

    private fun queryProductDetails(productIds: List<String>, productType: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build()
            })
            .build()

        billingClient?.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.update { it.copy(isConnecting = false, status = "Product query failed: ${result.debugMessage}") }
                return@queryProductDetailsAsync
            }

            val merged = productDetailsById + queryResult.productDetailsList.associateBy { it.productId }
            productDetailsById = merged
            _state.update {
                it.copy(
                    isConnecting = false,
                    products = merged.mapValues { (_, details) ->
                        PlayBillingProduct(
                            productId = details.productId,
                            title = productTitle(details.productId),
                            price = details.displayPrice()
                        )
                    },
                    status = if (merged.isEmpty()) "No Google Play products configured yet." else "Google Play products loaded."
                )
            }
        }
    }

    private fun queryPurchases(client: BillingClient, productType: String) {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(productType).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::handlePurchase)
                if (purchases.isEmpty()) {
                    _state.update { it.copy(status = "No previous $productType purchases found.") }
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            _state.update { it.copy(status = "Purchase is pending.") }
            return
        }
        val productId = purchase.products.firstOrNull() ?: return
        val tier = tierForProduct(productId) ?: return

        if (!purchase.isAcknowledged) {
            billingClient?.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            ) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    _state.update { it.copy(status = "Purchase acknowledgement failed: ${result.debugMessage}") }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            tierRepository.saveTierResult("google_play:$productId", tier.rank, tier)
            tierGate.getTier()
            _state.update { it.copy(status = "${tier.name} unlocked through Google Play.") }
        }
    }

    private fun ProductDetails.displayPrice(): String {
        return if (productType == BillingClient.ProductType.SUBS) {
            subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()
                ?.formattedPrice
        } else {
            oneTimePurchaseOfferDetails?.formattedPrice
        } ?: "Google Play"
    }

    override fun onCleared() {
        billingClient?.endConnection()
        billingClient = null
        super.onCleared()
    }

    private companion object {
        val subscriptionSkus = listOf(
            "securechat_pro_monthly",
            "securechat_pro_yearly",
            "securechat_elite_monthly",
            "securechat_elite_yearly"
        )
        val lifetimeSkus = listOf(
            "securechat_pro_lifetime",
            "securechat_elite_lifetime",
            "securechat_elite_activation_code"
        )

        fun tierForProduct(productId: String): AccessTier? = when {
            productId.contains("_pro_") -> AccessTier.PRO
            productId.contains("_elite_") -> AccessTier.ELITE
            else -> null
        }

        fun productTitle(productId: String): String = when (productId) {
            "securechat_pro_monthly" -> "Pro Monthly"
            "securechat_pro_yearly" -> "Pro Yearly"
            "securechat_elite_monthly" -> "Elite Monthly"
            "securechat_elite_yearly" -> "Elite Yearly"
            "securechat_pro_lifetime" -> "Pro Lifetime"
            "securechat_elite_lifetime" -> "Elite Lifetime"
            "securechat_elite_activation_code" -> "Elite Activation Code"
            else -> productId
        }
    }
}
