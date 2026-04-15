/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secure_rules")
data class SecureRuleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "trigger_type")
    val triggerType: String,
    @ColumnInfo(name = "trigger_value")
    val triggerValue: String,
    @ColumnInfo(name = "action_type")
    val actionType: String,
    @ColumnInfo(name = "security_level")
    val securityLevel: String,
    val priority: Int,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "last_triggered")
    val lastTriggered: Long?,
    @ColumnInfo(name = "trigger_count")
    val triggerCount: Int
)
