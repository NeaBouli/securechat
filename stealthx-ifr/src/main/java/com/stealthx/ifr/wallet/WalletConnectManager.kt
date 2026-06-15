/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr.wallet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wallet manager — opens external wallet apps via deep links.
 *
 * CRITICAL: Chameleon makes NO direct HTTP calls for WalletConnect.
 * The external wallet app (MetaMask, Trust Wallet, etc.) handles all RPC.
 * We only send Intent.ACTION_VIEW with wallet browser deep links.
 *
 * Flow:
 * 1. User taps "Open Wallet"
 * 2. We open the SecureCall SIWE page inside the installed wallet browser
 * 3. The wallet signs the challenge and redirects to https://stealthx.tech/return/securechat
 * 4. SecureChat verifies held IFR balance via web3j eth_call
 */
@Singleton
class WalletConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val METAMASK_PACKAGE = "io.metamask"
        private const val TRUST_PACKAGE = "com.wallet.crypto.trustapp"
        private const val RAINBOW_PACKAGE = "me.rainbow"
        private const val COINBASE_PACKAGE = "org.toshi"
        private const val TAG = "WalletConnect"
        private const val BACKEND_URL = "https://api.stealthx.tech"
        private val ETH_ADDRESS_REGEX = Regex("0x[a-fA-F0-9]{40}")
        private val ETH_SIGNATURE_REGEX = Regex("0x[a-fA-F0-9]{130}")
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val _walletCallbacks = MutableSharedFlow<WalletConnectResult>(replay = 1)
    val walletCallbacks = _walletCallbacks.asSharedFlow()

    /**
     * Build the WalletConnect intent used by ActivityResult launchers.
     */
    fun createWalletConnectIntent(wcUri: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(wcUri))
    }

    fun createWalletOpenIntent(): Intent {
        val preferredPackage = listOf(
            METAMASK_PACKAGE,
            TRUST_PACKAGE,
            RAINBOW_PACKAGE,
            COINBASE_PACKAGE
        ).firstOrNull(::isPackageInstalled)

        val deviceId = "securechat:${context.packageName}"
        val challenge = fetchChallenge(deviceId)
        val nonce = challenge?.first.orEmpty()
        val message = challenge?.second ?: (
            "SecureChat wants you to verify your Ethereum wallet.\n\n" +
                "Purpose: IFR hold verification\n" +
                "Issued At: ${System.currentTimeMillis()}"
            )
        val pageUrl = Uri.Builder()
            .scheme("https")
            .authority("stealthx.tech")
            .path("siwe.html")
            .appendQueryParameter("nonce", nonce)
            .appendQueryParameter("deviceId", deviceId)
            .appendQueryParameter("message", message)
            .appendQueryParameter("returnScheme", "securechat")
            .appendQueryParameter("returnHost", "wc")
            .appendQueryParameter("returnPackage", context.packageName)
            .appendQueryParameter("ts", System.currentTimeMillis().toString())
            .build()
            .toString()
        val dappPath = pageUrl.removePrefix("https://")

        val uri = when (preferredPackage) {
            METAMASK_PACKAGE -> "https://metamask.app.link/dapp/$dappPath"
            TRUST_PACKAGE -> "https://link.trustwallet.com/open_url?coin_id=60&url=${Uri.encode(pageUrl)}"
            else -> pageUrl
        }

        Log.d(TAG, "Opening SIWE wallet browser via ${preferredPackage ?: "default"}; nonce=${nonce.take(12)}")
        return Intent(Intent.ACTION_VIEW, Uri.parse(uri))
    }

    /**
     * Launch wallet app via deep link for connection.
     *
     * @param wcUri  WalletConnect URI (wc:...)
     * @return true if intent was sent, false if no wallet app found
     */
    fun launchWalletConnect(wcUri: String): Boolean {
        val intent = createWalletConnectIntent(wcUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Launch MetaMask specifically via deep link.
     */
    fun launchMetaMask(wcUri: String): Boolean {
        val metamaskUri = wcUri.replace("wc:", "metamask://wc?uri=")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(metamaskUri)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a compatible wallet app is installed.
     */
    fun isWalletAppInstalled(): Boolean {
        return listOf(
            METAMASK_PACKAGE,
            TRUST_PACKAGE,
            RAINBOW_PACKAGE,
            COINBASE_PACKAGE
        ).any(::isPackageInstalled)
    }

    /**
     * Validate EIP-55 checksum address format.
     */
    fun isValidAddress(address: String): Boolean {
        if (!address.startsWith("0x") || address.length != 42) return false
        val hexPart = address.substring(2)
        return hexPart.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    /**
     * Process data returned by a wallet ActivityResult callback.
     *
     * Wallet apps vary: some return extras, some return a data URI. Accept the
     * common keys and fall back to extracting the first Ethereum address.
     */
    fun processActivityResult(resultCode: Int, data: Intent?): WalletConnectResult {
        if (resultCode != Activity.RESULT_OK) return WalletConnectResult.Cancelled
        if (data == null) return WalletConnectResult.Error("Wallet did not return connection data")

        val candidates = buildList {
            listOf("walletAddress", "address", "account", "accounts", "selectedAddress").forEach { key ->
                data.getStringExtra(key)?.let(::add)
                data.getStringArrayExtra(key)?.forEach(::add)
                data.getStringArrayListExtra(key)?.forEach(::add)
            }
            data.dataString?.let(::add)
            data.data?.let { uri ->
                listOf("walletAddress", "address", "account", "accounts", "selectedAddress").forEach { key ->
                    uri.getQueryParameter(key)?.let(::add)
                }
            }
        }

        val address = candidates
            .asSequence()
            .mapNotNull(::extractAddress)
            .firstOrNull()
            ?: return WalletConnectResult.Error("Wallet did not return a valid Ethereum address")

        return WalletConnectResult.Success(
            walletAddress = address,
            signature = data.getStringExtra("signature")
        )
    }

    fun handleDeepLink(uri: Uri?): Boolean {
        if (uri == null) return false
        val isCustomCallback = uri.scheme == "securechat" && uri.host == "wc"
        val isHttpsCallback = uri.scheme == "https" && uri.host == "stealthx.tech" &&
            ((uri.path?.startsWith("/return/securechat") == true &&
                (uri.getQueryParameter("app") ?: "securechat") == "securechat") ||
                (uri.path?.startsWith("/return") == true &&
                    uri.getQueryParameter("app") == "securechat"))
        if (!isCustomCallback && !isHttpsCallback) return false

        val address = uri.getQueryParameter("address")
            ?: uri.getQueryParameter("walletAddress")
            ?: uri.getQueryParameter("addr")
        val signature = uri.getQueryParameter("signature")
            ?: uri.getQueryParameter("sig")
        val result = when {
            address == null || !isValidAddress(address) ->
                WalletConnectResult.Error("Wallet did not return a valid Ethereum address")
            signature == null || !ETH_SIGNATURE_REGEX.matches(signature) ->
                WalletConnectResult.Error("Wallet did not return a valid signature")
            else -> WalletConnectResult.Success(address, signature)
        }
        _walletCallbacks.tryEmit(result)
        Log.d(TAG, "Wallet callback handled: ${result::class.java.simpleName}")
        return true
    }

    private fun fetchChallenge(deviceId: String): Pair<String, String>? {
        return try {
            val url = "$BACKEND_URL/siwe/challenge?deviceId=${Uri.encode(deviceId)}"
            val response = httpClient.newCall(Request.Builder().url(url).get().build()).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "SIWE challenge failed: HTTP ${response.code}")
                return null
            }
            val json = JSONObject(response.body?.string() ?: return null)
            json.getString("nonce") to json.getString("message")
        } catch (e: Throwable) {
            Log.w(TAG, "SIWE challenge failed: ${e.message}")
            null
        }
    }

    private fun extractAddress(value: String): String? {
        val direct = value.trim()
        if (isValidAddress(direct)) return direct
        return ETH_ADDRESS_REGEX.find(value)?.value?.takeIf(::isValidAddress)
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
