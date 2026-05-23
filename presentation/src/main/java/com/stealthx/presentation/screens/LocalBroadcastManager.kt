/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screens

import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.repository.ContactRepository
import com.stealthx.data.repository.MessageRepository
import com.stealthx.domain.tier.TierGate
import com.stealthx.features.broadcast.BroadcastManager
import com.stealthx.features.broadcast.BroadcastRecord
import com.stealthx.features.broadcast.BroadcastResult
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

@Singleton
class LocalBroadcastManager @Inject constructor(
    private val tierGate: TierGate,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val contactExchangeManager: ContactExchangeManager
) : BroadcastManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val history = mutableListOf<BroadcastRecord>()

    override suspend fun sendBroadcast(message: String): BroadcastResult {
        if (!tierGate.requiresElite()) {
            return BroadcastResult.Failure("Emergency Broadcast requires Elite tier.")
        }

        if (!contactExchangeManager.isConnected) {
            return BroadcastResult.Failure("Signaling offline — reconnect and try again. Messages are NOT sent while offline.")
        }

        val contacts = contactRepository.observeAll().first()
        if (contacts.isEmpty()) return BroadcastResult.Failure("No contacts available.")

        var sent = 0
        var failed = 0
        contacts.forEach { contact ->
            runCatching { messageRepository.sendLocalMessage(contact.id, message) }
                .onSuccess { sent++ }
                .onFailure { failed++ }
        }

        val result = when {
            failed == 0 -> BroadcastResult.Success(sent)
            sent > 0 -> BroadcastResult.PartialSuccess(sent, failed)
            else -> BroadcastResult.Failure("All $failed sends failed")
        }

        history += BroadcastRecord(
            id = UUID.randomUUID().toString(),
            message = message,
            sentAt = System.currentTimeMillis(),
            recipientCount = sent,
            status = result
        )
        return result
    }

    override suspend fun getBroadcastHistory(): List<BroadcastRecord> = history.toList()
}
