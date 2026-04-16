/*
 * SecureChat — Emergency Broadcast System
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Sends an encrypted message to ALL contacts at once.
 * Use cases: emergency alerts, group warnings, time-critical notifications.
 *
 * Encryption: XChaCha20-Poly1305 per recipient (no shared key).
 * Each recipient gets an individually encrypted copy — no group key
 * that could be leaked to compromise all members.
 *
 * Tier: Elite (>=6.000 IFR)
 */
package com.stealthx.features.broadcast

/**
 * Interface for emergency broadcast operations.
 * Implementation comes in Phase 2 (Q3 2026) with relay transport.
 */
interface BroadcastManager {

    /**
     * Sends an encrypted broadcast to all contacts.
     * Each recipient receives individually encrypted content.
     * No group key — if one recipient's key is compromised,
     * other recipients' messages remain secure.
     */
    suspend fun sendBroadcast(message: String): BroadcastResult

    /**
     * Returns broadcast history (locally stored, encrypted at rest).
     */
    suspend fun getBroadcastHistory(): List<BroadcastRecord>
}

sealed class BroadcastResult {
    data class Success(val sentTo: Int) : BroadcastResult()
    data class PartialSuccess(val sent: Int, val failed: Int) : BroadcastResult()
    data class Failure(val reason: String) : BroadcastResult()
}

data class BroadcastRecord(
    val id: String,
    val message: String,     // stored encrypted via SQLCipher
    val sentAt: Long,
    val recipientCount: Int,
    val status: BroadcastResult
)
