/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ContactKeyEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contact_id", "sent_at"]),
        Index(value = ["delivery_status"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "contact_id")
    val contactId: String,
    @ColumnInfo(name = "direction")
    val direction: String,
    @ColumnInfo(name = "ciphertext", typeAffinity = ColumnInfo.BLOB)
    val ciphertext: ByteArray,
    @ColumnInfo(name = "nonce", typeAffinity = ColumnInfo.BLOB)
    val nonce: ByteArray,
    @ColumnInfo(name = "aad", typeAffinity = ColumnInfo.BLOB)
    val aad: ByteArray,
    @ColumnInfo(name = "padded_length")
    val paddedLength: Int,
    @ColumnInfo(name = "algorithm")
    val algorithm: String,
    @ColumnInfo(name = "payload_version")
    val payloadVersion: Int,
    @ColumnInfo(name = "sent_at")
    val sentAt: Long,
    @ColumnInfo(name = "delivery_status")
    val deliveryStatus: String,
    @ColumnInfo(name = "ratchet_dh_public", typeAffinity = ColumnInfo.BLOB)
    val ratchetDhPublic: ByteArray? = null,
    @ColumnInfo(name = "ratchet_counter")
    val ratchetCounter: Int? = null,
    @ColumnInfo(name = "ratchet_prev_counter")
    val ratchetPrevCounter: Int? = null,
    @ColumnInfo(name = "ratchet_ciphertext", typeAffinity = ColumnInfo.BLOB)
    val ratchetCiphertext: ByteArray? = null,
    @ColumnInfo(name = "ratchet_nonce", typeAffinity = ColumnInfo.BLOB)
    val ratchetNonce: ByteArray? = null,
    @ColumnInfo(name = "ratchet_aad", typeAffinity = ColumnInfo.BLOB)
    val ratchetAad: ByteArray? = null,
    @ColumnInfo(name = "ratchet_padded_length")
    val ratchetPaddedLength: Int? = null,
    @ColumnInfo(name = "ratchet_algorithm")
    val ratchetAlgorithm: String? = null,
    @ColumnInfo(name = "ratchet_payload_version")
    val ratchetPayloadVersion: Int? = null,
    @ColumnInfo(name = "expires_at", defaultValue = "NULL")
    val expiresAt: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
