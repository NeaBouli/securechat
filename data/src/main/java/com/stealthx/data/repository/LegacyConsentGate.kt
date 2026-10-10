/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import java.security.MessageDigest

/**
 * Binds a legacy-contact approval to exactly the candidate the user is shown (SCT-02 scan finding).
 *
 * One immutable candidate (generation, sxId, key fingerprint, content) is frozen when the warning
 * opens. While it is pending, further candidates are refused rather than replacing it, and
 * confirm/cancel only act on the matching generation, so a stale, replayed or second candidate
 * can never receive the consent that was given for the displayed one.
 */
class LegacyConsentGate {

    data class Candidate(
        val generation: Int,
        val sxId: String,
        val keyFingerprint: String,
        val content: String
    )

    private var nextGeneration = 0
    private var pending: Candidate? = null

    @Synchronized
    fun isPending(): Boolean = pending != null

    /** The candidate currently awaiting a decision, if any (drives the visible dialog). */
    @Synchronized
    fun current(): Candidate? = pending

    /** Freezes a new candidate, or returns null if another candidate is still pending. */
    @Synchronized
    fun offer(sxId: String, ed25519PublicKey: ByteArray, content: String): Candidate? {
        if (pending != null) return null
        nextGeneration += 1
        return Candidate(nextGeneration, sxId, fingerprint(ed25519PublicKey), content).also { pending = it }
    }

    /** Releases the pending candidate for a consented import only if [generation] matches. */
    @Synchronized
    fun claim(generation: Int): Candidate? {
        val current = pending ?: return null
        if (current.generation != generation) return null
        pending = null
        return current
    }

    /** Cancels only the matching candidate; nothing is written for it. */
    @Synchronized
    fun cancel(generation: Int): Boolean {
        if (pending?.generation != generation) return false
        pending = null
        return true
    }

    companion object {
        /** Short, human-comparable SHA-256 fingerprint of the Ed25519 identity key. */
        fun fingerprint(ed25519PublicKey: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(ed25519PublicKey)
                .take(8)
                .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
                .chunked(4)
                .joinToString(" ")
    }
}
