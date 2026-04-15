/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.repository

import com.stealthx.domain.rules.SecureRule
import kotlinx.coroutines.flow.Flow

interface SecureRuleRepository {
    fun observeEnabled(): Flow<List<SecureRule>>
    fun observeAll(): Flow<List<SecureRule>>
    suspend fun getById(id: String): SecureRule?
    suspend fun save(rule: SecureRule)
    suspend fun delete(rule: SecureRule)
    suspend fun deleteAll()
    suspend fun recordTrigger(ruleId: String, timestamp: Long)
}
