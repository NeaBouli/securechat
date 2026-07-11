package com.stealthx.securechat

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stealthx.data.activation.ActivationCodeClient
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.domain.repository.AccessTierRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class EntitlementRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun preferences(): AppPreferences
        fun tierRepository(): AccessTierRepository
    }

    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        val token = dependencies.preferences().entitlementToken ?: return Result.success()
        val outcome = suspendCancellableCoroutine<Pair<ActivationCodeClient.VerifiedActivation?, String?>> { continuation ->
            ActivationCodeClient.refresh(applicationContext, token) { activation, error ->
                if (continuation.isActive) continuation.resume(activation to error)
            }
        }
        val activation = outcome.first
        if (activation != null) {
            dependencies.preferences().entitlementToken = activation.entitlementToken
            dependencies.tierRepository().saveTierResult(
                sourceId = "fiat_entitlement:${activation.productId}",
                accessWeight = 0L,
                tier = activation.tier,
                expiresAtEpochSeconds = activation.expiresAtEpochSeconds
            )
            return Result.success()
        }
        return when (outcome.second) {
            "network_error" -> Result.retry()
            "entitlement_revoked", "invalid_entitlement", "entitlement_invalid" -> {
                dependencies.preferences().entitlementToken = null
                dependencies.tierRepository().invalidateCache()
                Result.success()
            }
            else -> Result.failure()
        }
    }
}
