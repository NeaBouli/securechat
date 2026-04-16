/*
 * SecureChat — Message Router
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Routes messages through the best available transport.
 * Phase 1: Always LocalTransport.
 * Phase 2+: Auto-selects based on tier + network availability.
 */
package com.stealthx.domain.transport

import com.stealthx.shared.model.EncryptedPayload
import com.stealthx.transport.RelayTransport
import com.stealthx.transport.TransportResult
import com.stealthx.transport.TransportType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRouter @Inject constructor(
    private val transports: Map<TransportType, @JvmSuppressWildcards RelayTransport>
) {

    /**
     * Sends a message through the best available transport.
     * Selection order: ONION_RELAY > TOR_RELAY > LOCAL
     * (higher anonymity preferred when available)
     */
    suspend fun send(
        recipientSxId: String,
        payload: EncryptedPayload
    ): TransportResult {
        val transport = selectTransport()
            ?: return TransportResult.Failed(
                messageId = "no-transport",
                reason = "No available transport"
            )
        return transport.send(recipientSxId, payload)
    }

    fun getActiveTransportType(): TransportType? = selectTransport()?.type

    private fun selectTransport(): RelayTransport? {
        // Preference: Onion > Tor > Local (most → least anonymous)
        return transports[TransportType.ONION_RELAY]?.takeIf { it.isAvailable }
            ?: transports[TransportType.TOR_RELAY]?.takeIf { it.isAvailable }
            ?: transports[TransportType.LOCAL]?.takeIf { it.isAvailable }
    }
}
