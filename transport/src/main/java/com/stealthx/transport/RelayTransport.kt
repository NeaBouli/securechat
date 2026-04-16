/*
 * SecureChat — Relay Transport Interface
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Abstract transport layer — messages are routed via different
 * transports depending on user tier and network availability.
 *
 * Phase 1 (current): LocalTransport — direct QR/NFC exchange, no network
 * Phase 2 (Q3 2026): TorRelayTransport — 2-hop onion via Tor Hidden Services
 * Phase 3 (Q4 2026): OnionRelayTransport — 3-hop, cover traffic (Elite)
 */
package com.stealthx.transport

import com.stealthx.shared.model.EncryptedPayload

sealed class TransportResult {
    data class Delivered(val messageId: String, val transportType: TransportType) : TransportResult()
    data class Queued(val messageId: String) : TransportResult()
    data class Failed(val messageId: String, val reason: String) : TransportResult()
}

enum class TransportType {
    LOCAL,        // Phase 1: direct QR/NFC
    TOR_RELAY,    // Phase 2: Tor Hidden Services
    ONION_RELAY,  // Phase 3: multi-hop onion routing
    NYM_MIXNET    // Research: Nym timing-resistant
}

interface RelayTransport {
    val type: TransportType
    val isAvailable: Boolean

    suspend fun send(
        recipientSxId: String,
        payload: EncryptedPayload
    ): TransportResult

    suspend fun connect(): Boolean
    suspend fun disconnect()
}
