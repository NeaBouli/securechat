/*
 * SecureChat — Onion Relay Transport (Phase 3 — Q4 2026)
 *
 * 3-hop internal onion routing (Briar-style).
 * Pro tier = 2 hops, Elite tier = 3 hops.
 * Cover traffic (dummy packets) against Global Passive Adversary.
 * Pluggable Transports (obfs4/Snowflake) for censored regions.
 */
package com.stealthx.transport

import com.stealthx.shared.model.RatchetMessage

class OnionRelayTransport : RelayTransport {

    override val type: TransportType = TransportType.ONION_RELAY
    override val isAvailable: Boolean = false // Phase 3 not yet implemented

    override suspend fun send(
        recipientSxId: String,
        message: RatchetMessage
    ): TransportResult = TransportResult.Failed(
        messageId = "onion-unavailable",
        reason = "Onion relay transport is not available in this release"
    )

    override suspend fun connect(): Boolean = false

    override suspend fun disconnect() = Unit
}
