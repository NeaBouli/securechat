/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** SCT-02 scan finding: consent must bind to the displayed candidate. Synthetic keys only. */
class LegacyConsentGateTest {
    private val keyA = ByteArray(32) { 1 }
    private val keyB = ByteArray(32) { 2 }
    private val id = "sx_LegacyAbc"

    @Test
    @DisplayName("same sxId with a different key cannot replace the pending candidate")
    fun secondCandidateRefused() {
        val gate = LegacyConsentGate()
        val a = gate.offer(id, keyA, "content-A")
        assertNotNull(a)
        assertNull(gate.offer(id, keyB, "content-B"), "B must not replace A while A is pending")
        val claimed = gate.claim(a!!.generation)
        assertEquals("content-A", claimed?.content)
        assertEquals(LegacyConsentGate.fingerprint(keyA), claimed?.keyFingerprint)
    }

    @Test
    @DisplayName("confirm binds to the displayed generation; stale or replayed confirms grant nothing")
    fun confirmBindsGeneration() {
        val gate = LegacyConsentGate()
        val a = gate.offer(id, keyA, "content-A")!!
        assertNull(gate.claim(a.generation + 1), "wrong generation")
        assertNotNull(gate.claim(a.generation))
        assertNull(gate.claim(a.generation), "replayed confirm")
    }

    @Test
    @DisplayName("cancel binds to the generation; a late confirm after cancel grants nothing")
    fun cancelThenLateConfirm() {
        val gate = LegacyConsentGate()
        val a = gate.offer(id, keyA, "content-A")!!
        assertFalse(gate.cancel(a.generation + 1))
        assertTrue(gate.cancel(a.generation))
        assertNull(gate.claim(a.generation), "late confirm after cancel")
        assertFalse(gate.isPending())
    }

    @Test
    @DisplayName("a new candidate after cancel gets a new generation; the old one cannot confirm it")
    fun oldGenerationCannotConfirmNewCandidate() {
        val gate = LegacyConsentGate()
        val a = gate.offer(id, keyA, "content-A")!!
        gate.cancel(a.generation)
        val b = gate.offer(id, keyB, "content-B")!!
        assertNotEquals(a.generation, b.generation)
        assertNull(gate.claim(a.generation))
        assertEquals("content-B", gate.claim(b.generation)?.content)
    }

    @Test
    @DisplayName("fingerprint distinguishes keys and is a stable 4x4 hex grouping")
    fun fingerprintFormat() {
        val fa = LegacyConsentGate.fingerprint(keyA)
        assertNotEquals(fa, LegacyConsentGate.fingerprint(keyB))
        assertEquals(fa, LegacyConsentGate.fingerprint(keyA.copyOf()))
        assertTrue(Regex("^[0-9A-F]{4}( [0-9A-F]{4}){3}$").matches(fa), fa)
    }

    @Test
    @DisplayName("current() exposes the pending candidate until it is claimed or cancelled")
    fun currentTracksPending() {
        val gate = LegacyConsentGate()
        assertNull(gate.current())
        val a = gate.offer(id, keyA, "content-A")!!
        gate.offer(id, keyB, "content-B")
        assertEquals(a, gate.current(), "a refused second candidate must not change what is shown")
        gate.claim(a.generation)
        assertNull(gate.current())
    }
}
