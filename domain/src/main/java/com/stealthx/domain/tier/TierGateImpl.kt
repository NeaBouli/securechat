/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.tier

import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.shared.model.IfrTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TierGate implementation — THE ONLY place for tier access control.
 *
 * Reads from IfrTierRepository (which validates HMAC internally).
 * On HMAC mismatch or expired cache → IfrTier.FREE.
 *
 * ╔══════════════════════════════════════════════════════════╗
 * ║  NO other class may check IFR tiers directly.           ║
 * ║  NO if(isPro) or if(isElite) in feature code.          ║
 * ║  ALL feature access MUST go through TierGate.           ║
 * ╚══════════════════════════════════════════════════════════╝
 */
class TierGateImpl(
    private val tierRepository: IfrTierRepository
) : TierGate {

    private val _currentTier = MutableStateFlow(IfrTier.FREE)

    override val currentTier: Flow<IfrTier> = _currentTier.asStateFlow()

    override fun getTierSync(): IfrTier = _currentTier.value

    override suspend fun getTier(): IfrTier {
        val tier = tierRepository.getCachedTier()
        _currentTier.value = tier
        return tier
    }

    override suspend fun isCacheValid(): Boolean {
        return tierRepository.isCacheValid()
    }

    override suspend fun invalidateCache() {
        tierRepository.invalidateCache()
        _currentTier.value = IfrTier.FREE
    }
}
