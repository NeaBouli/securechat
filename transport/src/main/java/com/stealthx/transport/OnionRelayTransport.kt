/*
 * SecureChat — Onion Relay Transport (Phase 3 — Q4 2026)
 *
 * 3-hop internal onion routing (Briar-style).
 * Pro tier = 2 hops, Elite tier = 3 hops.
 * Cover traffic (dummy packets) against Global Passive Adversary.
 * Pluggable Transports (obfs4/Snowflake) for censored regions.
 */
package com.stealthx.transport

import com.stealthx.shared.model.EncryptedPayload

class OnionRelayTransport : RelayTransport {

    override val type: TransportType = TransportType.ONION_RELAY
    override val isAvailable: Boolean = false // Phase 3 not yet implemented

    override suspend fun send(
        recipientSxId: String,
        payload: EncryptedPayload
    ): TransportResult {
        TODO("Phase 3 — Q4 2026: 3-hop onion routing + cover traffic")
    }

    override suspend fun connect(): Boolean {
        TODO("Phase 3 — Q4 2026")
    }

    override suspend fun disconnect() {
        TODO("Phase 3 — Q4 2026")
    }
}
