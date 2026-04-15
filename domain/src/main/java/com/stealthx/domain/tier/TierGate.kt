/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.tier

import com.stealthx.shared.model.IfrTier
import kotlinx.coroutines.flow.Flow

/**
 * TierGate — THE ONLY place where IFR tier access control is enforced.
 *
 * ╔══════════════════════════════════════════════════════════╗
 * ║  NO other class may check IFR tiers directly.           ║
 * ║  NO if(isPro) or if(isElite) in feature code.          ║
 * ║  ALL feature access MUST go through this class.         ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * Tier resolution order:
 * 1. Read cached IfrTierResult from IfrTierRepository
 * 2. Validate HMAC — if invalid, return FREE immediately
 * 3. Check expiry — if expired, return FREE (triggers re-verification)
 * 4. Return verified tier
 *
 * The implementation reads from IfrTierRepository (interface in :domain,
 * implemented in :data). TierGate itself has NO network calls.
 */
interface TierGate {

    /**
     * Current tier as a reactive Flow.
     * Emits whenever the cached tier changes (re-verification, expiry).
     */
    val currentTier: Flow<IfrTier>

    /**
     * Synchronous tier check — uses last known cached value.
     * Safe to call from any thread (reads memory cache).
     */
    fun getTierSync(): IfrTier

    /**
     * Suspend check — reads from DB, validates HMAC.
     * Use in coroutines for accurate tier state.
     */
    suspend fun getTier(): IfrTier

    /**
     * Returns true if current tier is PRO or ELITE.
     * Convenience wrapper around getTier().
     */
    suspend fun requiresPro(): Boolean = getTier() >= IfrTier.PRO

    /**
     * Returns true if current tier is ELITE.
     */
    suspend fun requiresElite(): Boolean = getTier() == IfrTier.ELITE

    /**
     * Returns true if the cache is still valid (not expired, HMAC ok).
     */
    suspend fun isCacheValid(): Boolean

    /**
     * Invalidate local cache — triggers re-verification on next access.
     * Call after user unlocks or revokes wallet.
     */
    suspend fun invalidateCache()
}
