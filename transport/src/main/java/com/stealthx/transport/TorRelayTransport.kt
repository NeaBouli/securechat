/*
 * SecureChat — Tor Relay Transport (Phase 2 — Q3 2026)
 *
 * Messages routed through Kaspa-incentivized relay nodes
 * running as Tor Hidden Services (.onion addresses).
 * Node registry on Kaspa BlockDAG via OP_RETURN.
 * 2-hop onion routing for Pro tier.
 */
package com.stealthx.transport

import com.stealthx.shared.model.EncryptedPayload

class TorRelayTransport : RelayTransport {

    override val type: TransportType = TransportType.TOR_RELAY
    override val isAvailable: Boolean = false // Phase 2 not yet implemented

    override suspend fun send(
        recipientSxId: String,
        payload: EncryptedPayload
    ): TransportResult {
        TODO("Phase 2 — Q3 2026: Tor Hidden Services + Kaspa relay nodes")
    }

    override suspend fun connect(): Boolean {
        TODO("Phase 2 — Q3 2026: tor-android library integration")
    }

    override suspend fun disconnect() {
        TODO("Phase 2 — Q3 2026")
    }
}
