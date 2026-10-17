/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.AccessTier
import com.stealthx.shared.model.PublicKeyBundle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** SCT-02 option B: synthetic keys only; signature validity is controlled per test. */
class ContactImportTest {

    private val dao = mockk<ContactKeyDao>(relaxed = true)
    private val tierGate = mockk<TierGate>()
    private var signatureValid = true
    private val repo = ContactRepository(dao, tierGate) { _, _, _ -> signatureValid }

    @BeforeEach
    fun setUp() {
        coEvery { tierGate.getTier() } returns AccessTier.PRO
        coEvery { dao.getById(any()) } returns null
    }

    private fun key(seed: Int) = ByteArray(32) { (it * 7 + seed).toByte() }

    private fun boundId(ed25519: ByteArray): String =
        "sx_" + StealthXIdentity.deriveShortId(ed25519.joinToString("") { "%02x".format(it.toInt() and 0xFF) })

    private fun bundle(sxId: String, ed25519: ByteArray, x25519: ByteArray = key(99)) = PublicKeyBundle(
        sxId = sxId, customHandle = null, x25519PublicKey = x25519, ed25519PublicKey = ed25519,
        signature = ByteArray(64) { 1 }, createdAt = 1_700_000_000_000L
    )

    private fun stored(b: PublicKeyBundle, verified: Boolean = true) = ContactKeyEntity(
        id = b.sxId, displayName = b.sxId, identityKey = b.ed25519PublicKey, dhPublicKey = b.x25519PublicKey,
        signature = b.signature, isVerified = verified, createdAt = 1L, lastUsedAt = null
    )

    private val legacyId = "sx_LegacyAbc" // valid format, not derived from any test key

    @Test
    @DisplayName("bound bundle via relay is added and marked verified")
    fun boundRelayAdded() = runTest {
        val k = key(1)
        val saved = slot<ContactKeyEntity>()
        coEvery { dao.insert(capture(saved)) } returns Unit
        assertEquals(ContactImportResult.ADDED, repo.addContactBundle(bundle(boundId(k), k), ImportProvenance.REMOTE))
        assertTrue(saved.captured.isVerified)
    }

    @Test
    @DisplayName("duplicate import with the same identity key is an idempotent no-op on every path")
    fun duplicateSameKey() = runTest {
        val k = key(2); val b = bundle(boundId(k), k)
        coEvery { dao.getById(b.sxId) } returns stored(b)
        for (p in ImportProvenance.values()) {
            assertEquals(ContactImportResult.ALREADY_KNOWN, repo.addContactBundle(b, p))
        }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    @DisplayName("signed relay bundle carrying another key's sxId (impersonation) is rejected")
    fun relayMismatchedIdRejected() = runTest {
        val victim = key(3); val attacker = key(4)
        assertThrows<UnboundIdentityException> {
            repo.addContactBundle(bundle(boundId(victim), attacker), ImportProvenance.REMOTE)
        }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    @DisplayName("unbound legacy ID via relay fails closed, consent flag cannot bypass it")
    fun relayUnboundRejected() = runTest {
        for (consent in listOf(false, true)) {
            assertThrows<UnboundIdentityException> {
                repo.addContactBundle(bundle(legacyId, key(5)), ImportProvenance.REMOTE, legacyConsent = consent)
            }
        }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    @DisplayName("in-person legacy import without consent writes nothing")
    fun inPersonLegacyNeedsConsent() = runTest {
        assertThrows<LegacyConsentRequiredException> {
            repo.addContactBundle(bundle(legacyId, key(6)), ImportProvenance.IN_PERSON)
        }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    @DisplayName("in-person legacy import with explicit consent is saved as not verified")
    fun inPersonLegacyWithConsent() = runTest {
        val saved = slot<ContactKeyEntity>()
        coEvery { dao.insert(capture(saved)) } returns Unit
        assertEquals(
            ContactImportResult.ADDED_LEGACY,
            repo.addContactBundle(bundle(legacyId, key(7)), ImportProvenance.IN_PERSON, legacyConsent = true)
        )
        assertFalse(saved.captured.isVerified)
    }

    @Test
    @DisplayName("invalid signature is rejected on every path, even with consent")
    fun invalidSignatureRejected() = runTest {
        signatureValid = false
        val k = key(8)
        for (b in listOf(bundle(boundId(k), k), bundle(legacyId, k))) {
            for (p in ImportProvenance.values()) {
                assertThrows<IllegalStateException> { repo.addContactBundle(b, p, legacyConsent = true) }
            }
        }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    @DisplayName("malformed key sizes are rejected before any lookup")
    fun invalidKeyRejected() = runTest {
        assertThrows<IllegalArgumentException> {
            repo.addContactBundle(bundle(legacyId, ByteArray(31)), ImportProvenance.IN_PERSON, legacyConsent = true)
        }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    @DisplayName("known contact with a changed identity key is rejected on every path, even with consent")
    fun pinnedKeyChangeRejected() = runTest {
        val old = key(9); val new = key(10)
        coEvery { dao.getById(legacyId) } returns stored(bundle(legacyId, old), verified = false)
        for (p in ImportProvenance.values()) {
            for (consent in listOf(false, true)) {
                assertThrows<IdentityKeyChangedException> {
                    repo.addContactBundle(bundle(legacyId, new), p, legacyConsent = consent)
                }
            }
        }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    @DisplayName("concurrent first import: losing writer never overwrites the established pin")
    fun concurrentFirstImport() = runTest {
        val k = key(11); val other = key(12); val id = boundId(k)
        coEvery { dao.insert(any()) } throws mockk<SQLiteConstraintException>(relaxed = true)
        coEvery { dao.getById(id) } returnsMany listOf(null, stored(bundle(id, other)))
        assertThrows<IdentityKeyChangedException> { repo.addContactBundle(bundle(id, k), ImportProvenance.REMOTE) }

        coEvery { dao.getById(id) } returnsMany listOf(null, stored(bundle(id, k)))
        assertEquals(ContactImportResult.ALREADY_KNOWN, repo.addContactBundle(bundle(id, k), ImportProvenance.REMOTE))
        coVerify(exactly = 0) { dao.deleteById(any()) }
    }

    @Test
    @DisplayName("stored legacy contacts are preserved and a rotated session key is not a rejection")
    fun storedLegacyPreservedAndSessionKeyRotation() = runTest {
        val identity = key(13)
        coEvery { dao.getById(legacyId) } returns stored(bundle(legacyId, identity, x25519 = key(14)), verified = false)
        assertEquals(
            ContactImportResult.ALREADY_KNOWN,
            repo.addContactBundle(bundle(legacyId, identity, x25519 = key(15)), ImportProvenance.REMOTE)
        )
        coVerify(exactly = 0) { dao.insert(any()) }
        coVerify(exactly = 0) { dao.deleteById(any()) }
    }

    @Test
    @DisplayName("isIdBoundToPublicKey accepts the derived ID and rejects other IDs and bad sizes")
    fun bindingCheck() {
        val k = key(16)
        assertTrue(StealthXIdentity.isIdBoundToPublicKey(boundId(k), k))
        assertFalse(StealthXIdentity.isIdBoundToPublicKey(boundId(key(17)), k))
        assertFalse(StealthXIdentity.isIdBoundToPublicKey(legacyId, k))
        assertFalse(StealthXIdentity.isIdBoundToPublicKey(boundId(k), ByteArray(31)))
    }
}
