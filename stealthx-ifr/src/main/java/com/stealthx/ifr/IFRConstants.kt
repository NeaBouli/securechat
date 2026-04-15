/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr

import com.stealthx.shared.model.IfrTier
import java.math.BigInteger

/**
 * IFR Token constants — hardcoded, never from remote config.
 *
 * Contract addresses are Ethereum Mainnet — verified on Etherscan.
 * NEVER change these without a public, audited commit.
 */
object IFRConstants {

    // ── Contract Addresses (Ethereum Mainnet) ─────────────────
    const val IFR_LOCK_ADDRESS        = "0x769928aBDfc949D0718d8766a1C2d7dBb63954Eb"
    const val BUILDER_REGISTRY_ADDRESS = "0xdfe6636DA47F8949330697e1dC5391267CEf0EE3"
    const val IFR_TOKEN_ADDRESS        = "0x77e99917Eca8539c62F509ED1193ac36580A6e7B"

    // ── Network ───────────────────────────────────────────────
    const val CHAIN_ID     = 1L         // Ethereum Mainnet
    const val TOKEN_DECIMALS = 9        // IFR has 9 decimals

    // ── Tier Thresholds (raw amount × 10^9) ──────────────────
    val PRO_THRESHOLD:   BigInteger = BigInteger("2000000000000")   // 2,000 IFR
    val ELITE_THRESHOLD: BigInteger = BigInteger("6000000000000")   // 6,000 IFR

    // ── Cache ─────────────────────────────────────────────────
    const val CACHE_VALIDITY_DAYS = 30L

    // ── Keystore alias for HMAC key ───────────────────────────
    const val HMAC_KEY_ALIAS = "ifr_tier_hmac_v1"

    // ── Ethereum RPC (public read-only) ───────────────────────
    // Fallback public endpoints — no API key required for eth_call
    val RPC_ENDPOINTS = listOf(
        "https://eth.llamarpc.com",
        "https://rpc.ankr.com/eth",
        "https://cloudflare-eth.com"
    )

    // ── IFRLock ABI fragment (isLocked function only) ─────────
    val IFRLOCK_ABI = """
        [
          {
            "inputs": [
              {"internalType": "address", "name": "user",      "type": "address"},
              {"internalType": "uint256", "name": "minAmount", "type": "uint256"}
            ],
            "name": "isLocked",
            "outputs": [{"internalType": "bool", "name": "", "type": "bool"}],
            "stateMutability": "view",
            "type": "function"
          },
          {
            "inputs": [{"internalType": "address", "name": "user", "type": "address"}],
            "name": "lockedAmount",
            "outputs": [{"internalType": "uint256", "name": "", "type": "uint256"}],
            "stateMutability": "view",
            "type": "function"
          }
        ]
    """.trimIndent()

    /**
     * Determine tier from raw locked amount.
     */
    fun tierFromAmount(amount: BigInteger): IfrTier = when {
        amount >= ELITE_THRESHOLD -> IfrTier.ELITE
        amount >= PRO_THRESHOLD   -> IfrTier.PRO
        else                      -> IfrTier.FREE
    }
}
