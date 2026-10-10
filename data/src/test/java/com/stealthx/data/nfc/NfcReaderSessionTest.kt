/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.nfc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class NfcReaderSessionTest {

    @Test
    @DisplayName("an armed session accepts exactly one event")
    fun singleEvent() {
        val s = NfcReaderSession(); val g = s.arm()
        assertTrue(s.isCurrent(g))
        assertTrue(s.consume(g))
        assertFalse(s.consume(g), "replayed event must not be accepted")
        assertFalse(s.isCurrent(g))
    }

    @Test
    @DisplayName("pause/exit (disarm) rejects delayed callbacks")
    fun disarmRejectsDelayed() {
        val s = NfcReaderSession(); val g = s.arm()
        s.disarm()
        assertFalse(s.isCurrent(g)); assertFalse(s.consume(g))
    }

    @Test
    @DisplayName("re-arming makes callbacks of the previous session stale")
    fun rearmInvalidatesOld() {
        val s = NfcReaderSession(); val old = s.arm(); val new = s.arm()
        assertFalse(s.consume(old)); assertTrue(s.consume(new))
    }

    @Test
    @DisplayName("generation 0 and never-armed sessions grant nothing")
    fun neverArmed() {
        val s = NfcReaderSession()
        assertFalse(s.isCurrent(0)); assertFalse(s.consume(0)); assertFalse(s.consume(1))
    }

    @Test
    @DisplayName("concurrent callbacks for one session yield a single import")
    fun concurrentConsume() {
        val s = NfcReaderSession(); val g = s.arm()
        val pool = Executors.newFixedThreadPool(8); val start = CountDownLatch(1); val wins = AtomicInteger()
        repeat(32) { pool.submit { start.await(); if (s.consume(g)) wins.incrementAndGet() } }
        start.countDown(); pool.shutdown(); pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(1, wins.get())
    }

    @Test
    @DisplayName("only a bounded stealthx://add/ record is selected")
    fun selectUri() {
        val link = "stealthx://add/sx_LegacyAbc?x=a&e=b&s=c&c=1"
        assertEquals(link, NfcContactUri.select(listOf(null, "https://example.org", link)))
        assertNull(NfcContactUri.select(listOf("stealthx://other/x", null)))
        assertNull(NfcContactUri.select(listOf("stealthx://add/" + "a".repeat(3000))))
        assertNull(NfcContactUri.select(emptyList()))
    }
}
