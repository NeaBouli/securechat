/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screens

import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.identity.PublicKeyBundleQr
import com.stealthx.data.repository.ContactImportResult
import com.stealthx.data.repository.ContactRepository
import com.stealthx.data.repository.ImportProvenance
import com.stealthx.data.repository.LegacyConsentGate
import com.stealthx.data.repository.LegacyConsentRequiredException
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.AccessTier
import com.stealthx.shared.model.PublicKeyBundle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SCT-02: deterministic overlap of contact imports and the legacy-consent dialog.
 * The repository is mocked and suspends on demand, so every interleaving is exact. Synthetic keys only.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewContactViewModelConcurrencyTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = mockk<ContactRepository>()
    private val exchange = mockk<ContactExchangeManager>(relaxed = true)
    private val tierGate = mockk<TierGate>()

    private val legacyA = qr("sx_LegacyAbc", key(1))
    private val legacyB = qr("sx_LegacyAbc", key(2))   // same SecureID, different key
    private val bound = qr("sx_BoundAbcd", key(3))

    private fun key(seed: Int) = ByteArray(32) { (it + seed).toByte() }
    private fun qr(id: String, ed: ByteArray) = PublicKeyBundleQr.toQrContent(
        PublicKeyBundle(id, null, ByteArray(32) { 9 }, ed, ByteArray(64) { 1 }, createdAt = 1L)
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { tierGate.currentTier } returns flowOf(AccessTier.PRO)
        every { repo.observeAll() } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = NewContactViewModel(repo, tierGate, exchange)

    private fun legacyNeedsConsent(content: String) {
        coEvery {
            repo.addContactBundle(match { it.sxId == "sx_LegacyAbc" && it.ed25519PublicKey.contentEquals(PublicKeyBundleQr.fromQrContent(content).getOrThrow().ed25519PublicKey) }, ImportProvenance.IN_PERSON, false)
        } throws LegacyConsentRequiredException()
    }

    @Test
    @DisplayName("a parallel import finishing after the dialog opened keeps the dialog and does not leave the screen")
    fun parallelSuccessDoesNotNavigateAway() = runTest(dispatcher) {
        val boundDone = CompletableDeferred<ContactImportResult>()
        coEvery { repo.addContactBundle(match { it.sxId == "sx_BoundAbcd" }, any(), any()) } coAnswers { boundDone.await() }
        legacyNeedsConsent(legacyA)
        val vm = vm()

        vm.addFromQrContent(bound, ImportProvenance.REMOTE)      // starts, suspends in the repository
        advanceUntilIdle()
        vm.addFromQrContent(legacyA, ImportProvenance.IN_PERSON) // dialog opens
        advanceUntilIdle()
        val shown = assertNotNullAndGet(vm.uiState.value.legacyCandidate)

        boundDone.complete(ContactImportResult.ADDED)             // the earlier import finishes now
        advanceUntilIdle()
        assertEquals(shown, vm.uiState.value.legacyCandidate, "dialog must stay visible")
        assertFalse(vm.uiState.value.contactAdded, "screen must not navigate away while a decision is open")
    }

    @Test
    @DisplayName("same SecureID with a different key cannot replace the shown candidate; confirm imports A")
    fun replacementRefusedConfirmImportsShown() = runTest(dispatcher) {
        legacyNeedsConsent(legacyA)
        legacyNeedsConsent(legacyB)
        coEvery { repo.addContactBundle(any(), ImportProvenance.IN_PERSON, true) } returns ContactImportResult.ADDED_LEGACY
        val vm = vm()

        vm.addFromQrContent(legacyA, ImportProvenance.IN_PERSON); advanceUntilIdle()
        val a = assertNotNullAndGet(vm.uiState.value.legacyCandidate)
        vm.addFromQrContent(legacyB, ImportProvenance.IN_PERSON); advanceUntilIdle()
        assertEquals(a, vm.uiState.value.legacyCandidate, "B must not replace A")

        vm.confirmLegacyImport(a.generation); advanceUntilIdle()
        coVerify(exactly = 1) {
            repo.addContactBundle(match { it.ed25519PublicKey.contentEquals(key(1)) }, ImportProvenance.IN_PERSON, true)
        }
        coVerify(exactly = 0) {
            repo.addContactBundle(match { it.ed25519PublicKey.contentEquals(key(2)) }, any(), true)
        }
        assertNull(vm.uiState.value.legacyCandidate)
        assertTrue(vm.uiState.value.contactAdded)
    }

    @Test
    @DisplayName("stale or replayed confirm and cancel grant nothing")
    fun staleConfirmAndCancel() = runTest(dispatcher) {
        legacyNeedsConsent(legacyA)
        coEvery { repo.addContactBundle(any(), ImportProvenance.IN_PERSON, true) } returns ContactImportResult.ADDED_LEGACY
        val vm = vm()
        vm.addFromQrContent(legacyA, ImportProvenance.IN_PERSON); advanceUntilIdle()
        val a = assertNotNullAndGet(vm.uiState.value.legacyCandidate)

        vm.confirmLegacyImport(a.generation + 1); advanceUntilIdle()
        vm.cancelLegacyImport(a.generation + 1); advanceUntilIdle()
        assertEquals(a, vm.uiState.value.legacyCandidate)
        coVerify(exactly = 0) { repo.addContactBundle(any(), any(), true) }

        vm.cancelLegacyImport(a.generation); advanceUntilIdle()
        vm.confirmLegacyImport(a.generation); advanceUntilIdle()   // late confirm after cancel
        coVerify(exactly = 0) { repo.addContactBundle(any(), any(), true) }
        assertNull(vm.uiState.value.legacyCandidate)
        assertFalse(vm.uiState.value.contactAdded)
    }

    @Test
    @DisplayName("after cancel a new import works normally (gate not stuck)")
    fun cancelUnblocksNextImport() = runTest(dispatcher) {
        legacyNeedsConsent(legacyA)
        coEvery { repo.addContactBundle(match { it.sxId == "sx_BoundAbcd" }, any(), any()) } returns ContactImportResult.ADDED
        val vm = vm()
        vm.addFromQrContent(legacyA, ImportProvenance.IN_PERSON); advanceUntilIdle()
        vm.addFromQrContent(bound, ImportProvenance.REMOTE); advanceUntilIdle()
        coVerify(exactly = 0) { repo.addContactBundle(match { it.sxId == "sx_BoundAbcd" }, any(), any()) }

        vm.cancelLegacyImport(vm.uiState.value.legacyCandidate!!.generation); advanceUntilIdle()
        vm.addFromQrContent(bound, ImportProvenance.REMOTE); advanceUntilIdle()
        coVerify(exactly = 1) { repo.addContactBundle(match { it.sxId == "sx_BoundAbcd" }, ImportProvenance.REMOTE, false) }
        assertTrue(vm.uiState.value.contactAdded)
    }

    @Test
    @DisplayName("the dialog shows the fingerprint of the key that will be imported")
    fun fingerprintMatchesImportedKey() = runTest(dispatcher) {
        legacyNeedsConsent(legacyA)
        val vm = vm()
        vm.addFromQrContent(legacyA, ImportProvenance.IN_PERSON); advanceUntilIdle()
        assertEquals(LegacyConsentGate.fingerprint(key(1)), vm.uiState.value.legacyCandidate?.keyFingerprint)
    }

    private fun <T> assertNotNullAndGet(value: T?): T {
        assertNotNull(value); return value!!
    }
}
