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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wallet manager — opens external wallet apps via deep links.
 *
 * CRITICAL: Chameleon makes NO direct HTTP calls for WalletConnect.
 * The external wallet app (MetaMask, Trust Wallet, etc.) handles all RPC.
 * We only send Intent.ACTION_VIEW with wc:// or metamask:// deep links.
 *
 * Flow:
 * 1. User taps "Open Wallet"
 * 2. We open an installed wallet app
 * 3. User copies their public Ethereum address
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
        private const val METAMASK_DEEP_LINK = "metamask://"
        private const val TRUST_DEEP_LINK = "trust://"
        private val ETH_ADDRESS_REGEX = Regex("0x[a-fA-F0-9]{40}")
    }

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

        val uri = when (preferredPackage) {
            METAMASK_PACKAGE -> METAMASK_DEEP_LINK
            TRUST_PACKAGE -> TRUST_DEEP_LINK
            else -> "https://metamask.app.link/"
        }

        return Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            preferredPackage?.let { setPackage(it) }
        }
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
     * Process a manually entered wallet address.
     * Validates format and returns result.
     */
    fun processManualAddress(address: String): WalletConnectResult {
        return if (isValidAddress(address)) {
            WalletConnectResult.Success(walletAddress = address, signature = null)
        } else {
            WalletConnectResult.Error("Invalid Ethereum address format")
        }
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
