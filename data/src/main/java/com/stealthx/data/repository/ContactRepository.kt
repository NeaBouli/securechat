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
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.domain.tier.TierGate
import com.stealthx.domain.tier.TierLimitException
import com.stealthx.shared.model.AccessTier
import com.stealthx.shared.model.PublicKeyBundle
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository internal constructor(
    private val contactKeyDao: ContactKeyDao,
    private val tierGate: TierGate,
    private val verifySignature: (message: ByteArray, signature: ByteArray, publicKey: ByteArray) -> Boolean
) {
    @Inject
    constructor(contactKeyDao: ContactKeyDao, tierGate: TierGate) :
        this(contactKeyDao, tierGate, ChameleonCrypto::verify)

    companion object {
        const val FREE_CONTACT_LIMIT = 10
    }

    /**
     * Add a contact. Throws [TierLimitException] if FREE tier and limit reached.
     * Count + insert are atomic — no TOCTOU race condition.
     */
    suspend fun addContact(contact: ContactKeyEntity) {
        if (tierGate.getTier() < AccessTier.PRO) {
            val inserted = contactKeyDao.insertIfUnderLimit(contact, FREE_CONTACT_LIMIT)
            if (!inserted) {
                val count = contactKeyDao.count()
                throw TierLimitException(
                    "Contact limit reached ($count/$FREE_CONTACT_LIMIT). Pro access is required for unlimited contacts."
                )
            }
        } else {
            contactKeyDao.insert(contact)
        }
    }

    /**
     * Import a verified contact bundle (SCT-02, interim option B).
     *
     * - [ImportProvenance.REMOTE] (relay, deep link, pasted text): the sxId must be derived
     *   from the bundle's Ed25519 key. Unbound legacy IDs fail closed.
     * - [ImportProvenance.IN_PERSON] (camera QR scan, NFC tap): an unbound legacy ID is only
     *   saved after explicit [legacyConsent]; without it nothing is written.
     * - A known contact's Ed25519 identity key is pinned: the same key is an idempotent
     *   no-op, a different key is rejected on every path, with or without consent.
     *
     * A valid signature only proves the bundle is self-consistent, not who owns the sxId.
     */
    suspend fun addContactBundle(
        bundle: PublicKeyBundle,
        provenance: ImportProvenance,
        legacyConsent: Boolean = false
    ): ContactImportResult {
        validateBundle(bundle)
        val bound = StealthXIdentity.isIdBoundToPublicKey(bundle.sxId, bundle.ed25519PublicKey)

        getById(bundle.sxId)?.let { return pinnedResult(it, bundle) }

        if (!bound) {
            if (provenance == ImportProvenance.REMOTE) throw UnboundIdentityException()
            if (!legacyConsent) throw LegacyConsentRequiredException()
        }

        try {
            addContact(
                ContactKeyEntity(
                    id = bundle.sxId,
                    displayName = bundle.customHandle ?: bundle.sxId,
                    identityKey = bundle.ed25519PublicKey,
                    dhPublicKey = bundle.x25519PublicKey,
                    signature = bundle.signature,
                    isVerified = bound,
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = null
                )
            )
        } catch (e: SQLiteConstraintException) {
            // A concurrent first import won the primary key; never overwrite its pin.
            val winner = getById(bundle.sxId) ?: throw IllegalArgumentException("Contact already exists", e)
            return pinnedResult(winner, bundle)
        }
        return if (bound) ContactImportResult.ADDED else ContactImportResult.ADDED_LEGACY
    }

    private fun pinnedResult(existing: ContactKeyEntity, bundle: PublicKeyBundle): ContactImportResult {
        if (!existing.identityKey.contentEquals(bundle.ed25519PublicKey)) throw IdentityKeyChangedException()
        return ContactImportResult.ALREADY_KNOWN
    }

    fun observeAll(): Flow<List<ContactKeyEntity>> = contactKeyDao.observeAll()

    suspend fun getById(id: String): ContactKeyEntity? = contactKeyDao.getById(id)

    suspend fun deleteById(id: String) = contactKeyDao.deleteById(id)

    suspend fun renameContact(id: String, newName: String) {
        require(newName.isNotBlank()) { "Name cannot be blank" }
        contactKeyDao.updateDisplayName(id, newName.trim())
    }

    suspend fun count(): Int = contactKeyDao.count()

    private fun validateBundle(bundle: PublicKeyBundle) {
        require(bundle.sxId.matches(Regex("^sx_[1-9A-HJ-NP-Za-km-z]{9}\$"))) {
            "Invalid sx_ID format"
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
        check(verifySignature(payload, bundle.signature, bundle.ed25519PublicKey)) {
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
            append(x25519.joinToString("") { b -> "%02x".format(b.toInt() and 0xFF) })
            append("|")
            append(ed25519.joinToString("") { b -> "%02x".format(b.toInt() and 0xFF) })
            append("|")
            append(createdAt.toString())
        }.toByteArray(Charsets.UTF_8)
    }
}
