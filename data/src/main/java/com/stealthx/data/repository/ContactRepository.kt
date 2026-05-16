/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.domain.tier.TierGate
import com.stealthx.domain.tier.TierLimitException
import com.stealthx.shared.model.IfrTier
import com.stealthx.shared.model.PublicKeyBundle
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val contactKeyDao: ContactKeyDao,
    private val tierGate: TierGate
) {
    companion object {
        const val FREE_CONTACT_LIMIT = 10
    }

    /**
     * Add a contact. Throws [TierLimitException] if FREE tier and limit reached.
     * Count + insert are atomic — no TOCTOU race condition.
     */
    suspend fun addContact(contact: ContactKeyEntity) {
        if (tierGate.getTier() < IfrTier.PRO) {
            val inserted = contactKeyDao.insertIfUnderLimit(contact, FREE_CONTACT_LIMIT)
            if (!inserted) {
                val count = contactKeyDao.count()
                throw TierLimitException(
                    "Contact limit reached ($count/$FREE_CONTACT_LIMIT). Upgrade to Pro for unlimited contacts."
                )
            }
        } else {
            contactKeyDao.insert(contact)
        }
    }

    suspend fun addContactBundle(bundle: PublicKeyBundle) {
        validateBundle(bundle)
        require(getById(bundle.sxId) == null) { "Contact already exists" }

        try {
            addContact(
                ContactKeyEntity(
                    id = bundle.sxId,
                    displayName = bundle.customHandle ?: bundle.sxId,
                    identityKey = bundle.ed25519PublicKey,
                    dhPublicKey = bundle.x25519PublicKey,
                    signature = bundle.signature,
                    isVerified = true,
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = null
                )
            )
        } catch (e: SQLiteConstraintException) {
            throw IllegalArgumentException("Contact already exists", e)
        }
    }

    fun observeAll(): Flow<List<ContactKeyEntity>> = contactKeyDao.observeAll()

    suspend fun getById(id: String): ContactKeyEntity? = contactKeyDao.getById(id)

    suspend fun deleteById(id: String) = contactKeyDao.deleteById(id)

    suspend fun count(): Int = contactKeyDao.count()

    private fun validateBundle(bundle: PublicKeyBundle) {
        require(bundle.sxId.startsWith("sx_") && bundle.sxId.length >= 10) {
            "Invalid sx_ID"
        }
        require(bundle.x25519PublicKey.size == 32) { "Invalid X25519 public key" }
        require(bundle.ed25519PublicKey.size == 32) { "Invalid Ed25519 public key" }
        require(bundle.signature.size == 64) { "Invalid Ed25519 signature" }

        val payload = buildSignPayload(
            sxId = bundle.sxId,
            handle = bundle.customHandle,
            x25519 = bundle.x25519PublicKey,
            ed25519 = bundle.ed25519PublicKey,
            createdAt = bundle.createdAt
        )
        check(ChameleonCrypto.verify(payload, bundle.signature, bundle.ed25519PublicKey)) {
            "Ed25519 signature verification failed"
        }
    }

    private fun buildSignPayload(
        sxId: String,
        handle: String?,
        x25519: ByteArray,
        ed25519: ByteArray,
        createdAt: Long
    ): ByteArray {
        return buildString {
            append(sxId)
            append("|")
            append(handle ?: "")
            append("|")
            append(x25519.joinToString("") { "%02x".format(it) })
            append("|")
            append(ed25519.joinToString("") { "%02x".format(it) })
            append("|")
            append(createdAt.toString())
        }.toByteArray(Charsets.UTF_8)
    }
}
