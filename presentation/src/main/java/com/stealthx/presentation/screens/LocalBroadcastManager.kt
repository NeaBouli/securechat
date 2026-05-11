/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screens

import com.stealthx.data.repository.ContactRepository
import com.stealthx.domain.tier.TierGate
import com.stealthx.features.broadcast.BroadcastManager
import com.stealthx.features.broadcast.BroadcastRecord
import com.stealthx.features.broadcast.BroadcastResult
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBroadcastManager @Inject constructor(
    private val tierGate: TierGate,
    private val contactRepository: ContactRepository
) : BroadcastManager {

    private val history = mutableListOf<BroadcastRecord>()

    override suspend fun sendBroadcast(message: String): BroadcastResult {
        if (!tierGate.requiresElite()) {
            return BroadcastResult.Failure("Emergency Broadcast requires Elite tier.")
        }

        val recipients = contactRepository.count()
        val result = if (recipients == 0) {
            BroadcastResult.Failure("No contacts available for broadcast.")
        } else {
            BroadcastResult.Success(recipients)
        }

        history += BroadcastRecord(
            id = UUID.randomUUID().toString(),
            message = message,
            sentAt = System.currentTimeMillis(),
            recipientCount = recipients,
            status = result
        )
        return result
    }

    override suspend fun getBroadcastHistory(): List<BroadcastRecord> = history.toList()
}
