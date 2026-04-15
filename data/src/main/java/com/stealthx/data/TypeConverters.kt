/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data

import androidx.room.TypeConverter
import com.stealthx.domain.rules.ActionType
import com.stealthx.domain.rules.TriggerType
import com.stealthx.shared.model.IfrTier
import com.stealthx.shared.model.SecurityLevel
import java.time.Instant

class ChameleonTypeConverters {

    // Instant ↔ Long (epoch seconds)
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.epochSecond

    @TypeConverter
    fun toInstant(epochSecond: Long?): Instant? = epochSecond?.let { Instant.ofEpochSecond(it) }

    // SecurityLevel ↔ String
    @TypeConverter
    fun fromSecurityLevel(level: SecurityLevel): String = level.name

    @TypeConverter
    fun toSecurityLevel(name: String): SecurityLevel = SecurityLevel.valueOf(name)

    // IfrTier ↔ String
    @TypeConverter
    fun fromIfrTier(tier: IfrTier): String = tier.name

    @TypeConverter
    fun toIfrTier(name: String): IfrTier = IfrTier.valueOf(name)

    // TriggerType ↔ String
    @TypeConverter
    fun fromTriggerType(type: TriggerType): String = type.name

    @TypeConverter
    fun toTriggerType(name: String): TriggerType = TriggerType.valueOf(name)

    // ActionType ↔ String
    @TypeConverter
    fun fromActionType(type: ActionType): String = type.name

    @TypeConverter
    fun toActionType(name: String): ActionType = ActionType.valueOf(name)
}
