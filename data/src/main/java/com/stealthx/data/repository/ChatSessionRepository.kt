/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.data.dao.ChatSessionDao
import com.stealthx.data.entity.ChatSessionEntity
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.shared.model.RatchetMessage
import javax.inject.Inject
import javax.inject.Singleton

data class OutboundRatchetPayload(
    val dhPublicKey: ByteArray,
    val counter: Int,
    val message: RatchetMessage
)

@Singleton
class ChatSessionRepository @Inject constructor(
    private val chatSessionDao: ChatSessionDao
) {
    suspend fun encryptForSend(contact: ContactKeyEntity, plaintext: ByteArray, aad: ByteArray): OutboundRatchetPayload {
        val session = chatSessionDao.get(contact.id) ?: createSession(contact)
        val messageKey = ChameleonCrypto.hkdf(
            ikm = session.sendChainKey,
            salt = session.rootKey,
            info = INFO_MESSAGE_KEY,
            length = ChameleonCrypto.KEY_BYTES
        )
        val nextChainKey = ChameleonCrypto.hkdf(
            ikm = session.sendChainKey,
            salt = session.rootKey,
            info = INFO_CHAIN_KEY,
            length = ChameleonCrypto.KEY_BYTES
        )
        val payload = ChameleonCrypto.encrypt(plaintext, messageKey, aad)
        ChameleonCrypto.wipeBytes(messageKey)

        chatSessionDao.upsert(
            session.copy(
                sendChainKey = nextChainKey,
                sendCounter = session.sendCounter + 1,
                updatedAt = System.currentTimeMillis()
            )
        )

        return OutboundRatchetPayload(
            dhPublicKey = session.sendDhPublic,
            counter = session.sendCounter,
            message = RatchetMessage(
                dhPublicKey = session.sendDhPublic,
                counter = session.sendCounter,
                prevCounter = 0,
                payload = payload
            )
        )
    }

    private suspend fun createSession(contact: ContactKeyEntity): ChatSessionEntity {
        val (dhPublic, dhPrivate) = ChameleonCrypto.generateX25519KeyPair()
        val seed = contact.identityKey + contact.dhPublicKey + contact.id.toByteArray(Charsets.UTF_8)
        val rootKey = ChameleonCrypto.hkdf(
            ikm = seed,
            salt = null,
            info = INFO_ROOT_KEY,
            length = ChameleonCrypto.KEY_BYTES
        )
        val sendChainKey = ChameleonCrypto.hkdf(
            ikm = dhPrivate + contact.dhPublicKey,
            salt = rootKey,
            info = INFO_CHAIN_KEY,
            length = ChameleonCrypto.KEY_BYTES
        )
        val now = System.currentTimeMillis()
        val session = ChatSessionEntity(
            contactId = contact.id,
            rootKey = rootKey,
            sendChainKey = sendChainKey,
            sendDhPublic = dhPublic,
            sendDhPrivate = dhPrivate,
            sendCounter = 0,
            createdAt = now,
            updatedAt = now
        )
        chatSessionDao.upsert(session)
        return session
    }

    private companion object {
        val INFO_ROOT_KEY = "SecureChatRatchetRoot:v1".toByteArray(Charsets.UTF_8)
        val INFO_CHAIN_KEY = "SecureChatRatchetChain:v1".toByteArray(Charsets.UTF_8)
        val INFO_MESSAGE_KEY = "SecureChatRatchetMessage:v1".toByteArray(Charsets.UTF_8)
    }
}
