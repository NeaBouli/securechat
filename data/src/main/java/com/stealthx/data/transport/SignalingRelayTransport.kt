/*
 * SecureChat — Signaling WebSocket Relay Transport
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Phase 2 transport: routes encrypted ratchet messages over the existing
 * authenticated signaling WebSocket connection. Reuses ContactExchangeManager's
 * persistent, cert-pinned, identified WS — no second connection needed.
 */
package com.stealthx.data.transport

import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.identity.RatchetMessageQr
import com.stealthx.shared.model.RatchetMessage
import com.stealthx.transport.RelayTransport
import com.stealthx.transport.TransportResult
import com.stealthx.transport.TransportType
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalingRelayTransport @Inject constructor(
    private val exchangeManager: ContactExchangeManager
) : RelayTransport {

    override val type: TransportType = TransportType.SIGNALING_RELAY

    override val isAvailable: Boolean get() = exchangeManager.isIdentified

    override suspend fun send(recipientSxId: String, message: RatchetMessage): TransportResult {
        val messageId = UUID.randomUUID().toString()
        if (!exchangeManager.isIdentified) {
            return TransportResult.Failed(messageId, "Signaling relay is not identified")
        }
        val payload = RatchetMessageQr.toQrContent(message)
        val json = JSONObject().apply {
            put("type", "MESSAGE")
            put("to", recipientSxId)
            put("payload", payload)
        }.toString()
        val sent = exchangeManager.sendRaw(json)
        return if (sent) TransportResult.Queued(messageId)
               else TransportResult.Failed(messageId, "WS send failed")
    }

    override suspend fun connect(): Boolean = true
    override suspend fun disconnect() {}
}
