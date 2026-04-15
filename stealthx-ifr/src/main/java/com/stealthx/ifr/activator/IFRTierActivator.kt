/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr.activator

import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.ifr.IFRConstants
import com.stealthx.ifr.verifier.IFRLockVerifier
import com.stealthx.shared.model.IfrTier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IFR Tier Activator — verifies on-chain IFR lock and activates tier.
 *
 * Flow:
 * 1. Call IFRLockVerifier.getLockedAmount(wallet) — eth_call
 * 2. Compute tier via IFRConstants.tierFromAmount()
 * 3. Save via IfrTierRepository.saveTierResult() — HMAC computed internally
 *
 * Error handling:
 * - Network error + valid cache → keep cache, no error
 * - Network error + no cache → IfrTier.FREE
 * - HMAC is computed by IfrTierRepositoryImpl, not here
 */
@Singleton
class IFRTierActivator @Inject constructor(
    private val verifier: IFRLockVerifier,
    private val tierRepository: IfrTierRepository
) {

    data class ActivationResult(
        val tier: IfrTier,
        val lockedAmount: Long,
        val walletAddress: String,
        val fromCache: Boolean,
        val error: String?
    )

    /**
     * Verify IFR lock for a wallet and activate the corresponding tier.
     *
     * @param walletAddress  EIP-55 Ethereum address
     * @return ActivationResult with tier and status
     */
    suspend fun activate(walletAddress: String): ActivationResult {
        return try {
            val lockedAmount = verifier.getLockedAmount(walletAddress)
            val tier = IFRConstants.tierFromAmount(lockedAmount)

            // Save to cache (HMAC computed by repository)
            tierRepository.saveTierResult(
                walletAddress = walletAddress,
                lockedAmount = lockedAmount.toLong(),
                tier = tier
            )

            ActivationResult(
                tier = tier,
                lockedAmount = lockedAmount.toLong(),
                walletAddress = walletAddress,
                fromCache = false,
                error = null
            )
        } catch (e: Exception) {
            // Network error — try cache
            handleNetworkError(walletAddress, e.message ?: "Unknown error")
        }
    }

    /**
     * Re-verify an existing cached wallet (e.g. on app startup).
     * If network fails and cache is still valid, keep the cache.
     */
    suspend fun reverify(): ActivationResult {
        val cached = tierRepository.getCachedResult()
            ?: return ActivationResult(IfrTier.FREE, 0, "", false, null)

        return activate(cached.walletAddress)
    }

    private suspend fun handleNetworkError(walletAddress: String, errorMsg: String): ActivationResult {
        // Check if we have a valid cache
        if (tierRepository.isCacheValid()) {
            val cached = tierRepository.getCachedResult()
            if (cached != null) {
                return ActivationResult(
                    tier = cached.tier,
                    lockedAmount = cached.lockedAmount,
                    walletAddress = cached.walletAddress,
                    fromCache = true,
                    error = null
                )
            }
        }

        // No valid cache — return FREE
        return ActivationResult(
            tier = IfrTier.FREE,
            lockedAmount = 0,
            walletAddress = walletAddress,
            fromCache = false,
            error = errorMsg
        )
    }
}
