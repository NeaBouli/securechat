/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WalletConnect Manager — connects to external wallet apps via deep links.
 *
 * CRITICAL: Chameleon makes NO direct HTTP calls for WalletConnect.
 * The external wallet app (MetaMask, Trust Wallet, etc.) handles all RPC.
 * We only send Intent.ACTION_VIEW with wc:// or metamask:// deep links.
 *
 * Flow:
 * 1. User taps "Connect Wallet"
 * 2. We open deep link to wallet app
 * 3. User approves connection in wallet
 * 4. Wallet returns wallet address via callback/activity result
 * 5. We verify IFR lock amount via web3j eth_call (separate from WC)
 */
@Singleton
class WalletConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val METAMASK_DEEP_LINK = "metamask://wc"
        private const val TRUST_DEEP_LINK = "trust://wc"
        private const val GENERIC_WC_SCHEME = "wc:"
    }

    /**
     * Launch wallet app via deep link for connection.
     *
     * @param wcUri  WalletConnect URI (wc:...)
     * @return true if intent was sent, false if no wallet app found
     */
    fun launchWalletConnect(wcUri: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wcUri)).apply {
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
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("wc:"))
        val resolvedApps = context.packageManager.queryIntentActivities(intent, 0)
        return resolvedApps.isNotEmpty()
    }

    /**
     * Validate EIP-55 checksum address format.
     */
    fun isValidAddress(address: String): Boolean {
        if (!address.startsWith("0x") || address.length != 42) return false
        val hexPart = address.substring(2)
        return hexPart.all { it.isLetterOrDigit() }
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
}
