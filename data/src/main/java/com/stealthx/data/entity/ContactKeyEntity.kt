/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_keys")
data class ContactKeyEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "identity_key", typeAffinity = ColumnInfo.BLOB)
    val identityKey: ByteArray,
    @ColumnInfo(name = "dh_public_key", typeAffinity = ColumnInfo.BLOB)
    val dhPublicKey: ByteArray,
    @ColumnInfo(name = "signature", typeAffinity = ColumnInfo.BLOB)
    val signature: ByteArray,
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContactKeyEntity) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}
