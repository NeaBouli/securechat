/*
 * SecureChat — Tor Relay Transport (Phase 2 — Q3 2026)
 *
 * Messages routed through Kaspa-incentivized relay nodes
 * running as Tor Hidden Services (.onion addresses).
 * Node registry on Kaspa BlockDAG via OP_RETURN.
 * 2-hop onion routing for Pro tier.
 */
package com.stealthx.transport

import com.stealthx.shared.model.RatchetMessage

class TorRelayTransport : RelayTransport {

    override val type: TransportType = TransportType.TOR_RELAY
    override val isAvailable: Boolean = false // Phase 2 not yet implemented

    override suspend fun send(
        recipientSxId: String,
        message: RatchetMessage
    ): TransportResult = TransportResult.Failed(
        messageId = "tor-unavailable",
        reason = "Tor relay transport is not available in this release"
    )

    override suspend fun connect(): Boolean = false

    override suspend fun disconnect() = Unit
}
