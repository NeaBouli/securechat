/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ContactKeyEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChatSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "contact_id")
    val contactId: String,
    @ColumnInfo(name = "root_key", typeAffinity = ColumnInfo.BLOB)
    val rootKey: ByteArray,
    @ColumnInfo(name = "send_chain_key", typeAffinity = ColumnInfo.BLOB)
    val sendChainKey: ByteArray,
    @ColumnInfo(name = "send_dh_public", typeAffinity = ColumnInfo.BLOB)
    val sendDhPublic: ByteArray,
    @ColumnInfo(name = "send_dh_private", typeAffinity = ColumnInfo.BLOB)
    val sendDhPrivate: ByteArray,
    @ColumnInfo(name = "send_counter")
    val sendCounter: Int,
    @ColumnInfo(name = "receive_root_key", typeAffinity = ColumnInfo.BLOB)
    val receiveRootKey: ByteArray? = null,
    @ColumnInfo(name = "receive_chain_key", typeAffinity = ColumnInfo.BLOB)
    val receiveChainKey: ByteArray? = null,
    @ColumnInfo(name = "receive_dh_public", typeAffinity = ColumnInfo.BLOB)
    val receiveDhPublic: ByteArray? = null,
    @ColumnInfo(name = "receive_counter")
    val receiveCounter: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatSessionEntity) return false
        return contactId == other.contactId
    }

    override fun hashCode(): Int = contactId.hashCode()
}
