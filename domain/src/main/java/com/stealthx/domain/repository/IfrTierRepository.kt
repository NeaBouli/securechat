/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.repository

import com.stealthx.shared.model.CachedTierResult
import com.stealthx.shared.model.IfrTier

/**
 * Repository for IFR tier cache.
 * Interface in :domain, implementation in :data.
 *
 * SECURITY RULES:
 * - getCachedTier() validates HMAC before returning
 * - On HMAC mismatch → returns FREE, NEVER higher
 * - expiresAt = verifiedAt + 30 days
 */
interface IfrTierRepository {
    suspend fun getCachedTier(): IfrTier
    suspend fun getCachedResult(): CachedTierResult?
    suspend fun saveTierResult(walletAddress: String, lockedAmount: Long, tier: IfrTier)
    suspend fun invalidateCache()
    suspend fun isCacheValid(): Boolean
}
