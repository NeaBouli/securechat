/*
 * SecureChat — Local Transport (Phase 1)
 *
 * No network. Messages are written to a local outbox and
 * delivered manually via QR code or NFC tap.
 * This is the bootstrap transport until Phase 2 Kaspa relay
 * nodes are live.
 */
package com.stealthx.transport

import com.stealthx.shared.model.RatchetMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTransport @Inject constructor() : RelayTransport {

    override val type: TransportType = TransportType.LOCAL
    override val isAvailable: Boolean = true

    private val outbox = mutableMapOf<String, List<RatchetMessage>>()

    override suspend fun send(
        recipientSxId: String,
        message: RatchetMessage
    ): TransportResult {
        // Phase 1: queue in outbox for manual delivery
        val existing = outbox[recipientSxId] ?: emptyList()
        outbox[recipientSxId] = existing + message
        return TransportResult.Queued("local-${System.currentTimeMillis()}")
    }

    fun getOutbox(recipientSxId: String): List<RatchetMessage> {
        return outbox[recipientSxId] ?: emptyList()
    }

    fun clearOutbox(recipientSxId: String) {
        outbox.remove(recipientSxId)
    }

    override suspend fun connect(): Boolean = true
    override suspend fun disconnect() {}
}
