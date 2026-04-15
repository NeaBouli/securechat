/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.repository

import com.stealthx.shared.model.SecurityLevel
import kotlinx.coroutines.flow.Flow

data class AuditEntry(
    val id: Long,
    val timestamp: Long,
    val action: String,
    val packageName: String?,
    val securityLevel: SecurityLevel,
    val details: String?
)

interface AuditLogRepository {
    fun observeRecent(limit: Int = 100): Flow<List<AuditEntry>>
    suspend fun log(action: String, packageName: String?, securityLevel: SecurityLevel, details: String? = null)
    suspend fun deleteOlderThan(beforeTimestamp: Long)
    suspend fun count(): Int
}
