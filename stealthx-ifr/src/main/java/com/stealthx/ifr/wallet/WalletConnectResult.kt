/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr.wallet

sealed class WalletConnectResult {
    data class Success(val walletAddress: String, val signature: String?) : WalletConnectResult()
    data object Cancelled : WalletConnectResult()
    data class Error(val message: String) : WalletConnectResult()
}
