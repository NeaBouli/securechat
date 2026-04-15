/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crypto_keys")
data class CryptoKeyEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "key_type")
    val keyType: String,
    val algorithm: String,
    @ColumnInfo(name = "public_key", typeAffinity = ColumnInfo.BLOB)
    val publicKey: ByteArray?,
    @ColumnInfo(name = "keystore_alias")
    val keystoreAlias: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long?,
    @ColumnInfo(name = "is_revoked")
    val isRevoked: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CryptoKeyEntity) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}
