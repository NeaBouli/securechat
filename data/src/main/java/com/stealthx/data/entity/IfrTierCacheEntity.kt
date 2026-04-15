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
 * IFR Tier Cache — stores verified IFR lock results with HMAC protection.
 *
 * SECURITY:
 * - walletAddress: EIP-55 checksum format (0x + mixed-case hex)
 * - hmac: HMAC-SHA256 over (walletAddress, lockedAmount, tier, verifiedAt, expiresAt)
 * - HMAC key from KeystoreManager.getOrCreateHmacKey()
 * - On HMAC mismatch → return FREE, NEVER higher
 * - expiresAt = verifiedAt + 30 days (not from app start)
 */
@Entity(tableName = "ifr_tier_cache")
data class IfrTierCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "wallet_address")
    val walletAddress: String,

    @ColumnInfo(name = "locked_amount")
    val lockedAmount: Long,

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
        if (other !is IfrTierCacheEntity) return false
        return walletAddress == other.walletAddress
    }
    override fun hashCode(): Int = walletAddress.hashCode()
}
