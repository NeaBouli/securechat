/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.tier

import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.shared.model.IfrTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * TierGate implementation — THE ONLY place for tier access control.
 *
 * Loads cached tier from DB immediately on construction so that
 * currentTier never stays stuck at FREE when a valid cache exists.
 */
class TierGateImpl(
    private val tierRepository: IfrTierRepository,
    private val initScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : TierGate {

    private val _currentTier = MutableStateFlow(IfrTier.FREE)

    override val currentTier: Flow<IfrTier> = _currentTier.asStateFlow()

    init {
        initScope.launch {
            _currentTier.value = tierRepository.getCachedTier()
        }
    }

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
