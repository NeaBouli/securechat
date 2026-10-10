/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.nfc

import java.util.concurrent.atomic.AtomicInteger

/**
 * One in-app NFC reader session (SCT-02). IN_PERSON provenance for NFC may only come from a
 * ReaderCallback event whose generation is still the armed one; pausing, leaving the screen or
 * re-arming invalidates older callbacks, and each arming accepts at most one tag event.
 */
class NfcReaderSession {
    private val generation = AtomicInteger(0)
    private val active = AtomicInteger(NONE)

    /** Arms a new session and returns its generation; any older generation becomes stale. */
    fun arm(): Int {
        val next = generation.incrementAndGet()
        active.set(next)
        return next
    }

    fun isCurrent(gen: Int): Boolean = gen != NONE && active.get() == gen

    /** Claims the single event of [gen]; false if stale, disarmed or already consumed. */
    fun consume(gen: Int): Boolean = gen != NONE && active.compareAndSet(gen, NONE)

    fun disarm() = active.set(NONE)

    private companion object {
        const val NONE = 0
    }
}

/** Picks the contact link from the URI records of one NDEF message read by the reader callback. */
object NfcContactUri {
    private const val PREFIX = "stealthx://add/"
    private const val MAX_LENGTH = 2048

    fun select(recordUris: List<String?>): String? =
        recordUris.firstOrNull { it != null && it.startsWith(PREFIX) && it.length <= MAX_LENGTH }
}
