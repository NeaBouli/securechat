/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App tier cache — stores verified access results with HMAC protection.
 *
 * SECURITY:
 * - sourceId: activation-code or internal source identifier
 * - hmac: HMAC-SHA256 over (sourceId, accessWeight, tier, verifiedAt, expiresAt)
 * - HMAC key from KeystoreManager.getOrCreateHmacKey()
 * - On HMAC mismatch → return FREE, NEVER higher
 * - expiresAt = verifiedAt + 30 days (not from app start)
 */
@Entity(tableName = "access_tier_cache")
data class AccessTierCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "source_id")
    val sourceId: String,

    @ColumnInfo(name = "access_weight")
    val accessWeight: Long,

    val tier: String,

    @ColumnInfo(name = "verified_at")
    val verifiedAt: Long,

    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val hmac: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccessTierCacheEntity) return false
        return sourceId == other.sourceId
    }
    override fun hashCode(): Int = sourceId.hashCode()
}
