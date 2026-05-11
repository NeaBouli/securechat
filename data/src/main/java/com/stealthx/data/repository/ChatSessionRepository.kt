/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import android.content.Context
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.data.dao.ChatSessionDao
import com.stealthx.data.entity.ChatSessionEntity
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.shared.model.RatchetMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class OutboundRatchetPayload(
    val dhPublicKey: ByteArray,
    val counter: Int,
    val message: RatchetMessage
)

@Singleton
class ChatSessionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
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

    suspend fun decryptIncoming(contact: ContactKeyEntity, message: RatchetMessage): ByteArray {
        val session = chatSessionDao.get(contact.id) ?: createSession(contact)
        val receiveSession = if (session.receiveDhPublic?.contentEquals(message.dhPublicKey) == true) {
            session
        } else {
            session.withReceiveChain(message.dhPublicKey)
        }
        val chainKey = receiveSession.receiveChainKey
            ?: throw SecurityException("Receive chain not initialized")
        val counter = receiveSession.receiveCounter
        require(message.counter >= counter) { "Old or duplicate ratchet message" }
        require(message.counter - counter <= MAX_SKIP) { "Too many skipped ratchet messages" }

        var currentChain = chainKey
        var currentCounter = counter
        while (currentCounter < message.counter) {
            val skipped = kdfChain(currentChain, receiveSession.receiveRootKey)
            ChameleonCrypto.wipeBytes(skipped.messageKey)
            ChameleonCrypto.wipeBytes(currentChain)
            currentChain = skipped.nextChainKey
            currentCounter++
        }

        val target = kdfChain(currentChain, receiveSession.receiveRootKey)
        val plaintext = try {
            ChameleonCrypto.decrypt(message.payload, target.messageKey)
        } finally {
            ChameleonCrypto.wipeBytes(target.messageKey)
            ChameleonCrypto.wipeBytes(currentChain)
        }

        chatSessionDao.upsert(
            receiveSession.copy(
                receiveChainKey = target.nextChainKey,
                receiveCounter = message.counter + 1,
                updatedAt = System.currentTimeMillis()
            )
        )
        return plaintext
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

    private suspend fun ChatSessionEntity.withReceiveChain(senderDhPublic: ByteArray): ChatSessionEntity {
        val ownKeyPair = StealthXIdentity.getX25519KeyPair(context)
        val receiveRootKey = ownRootKey(ownKeyPair.sxId, ownKeyPair.publicKey)
        val sharedSecret = ChameleonCrypto.computeSharedSecret(ownKeyPair.privateKey, senderDhPublic)
        val receiveChainKey = ChameleonCrypto.hkdf(
            ikm = sharedSecret,
            salt = receiveRootKey,
            info = INFO_CHAIN_KEY,
            length = ChameleonCrypto.KEY_BYTES
        )
        ChameleonCrypto.wipeBytes(ownKeyPair.privateKey)
        ChameleonCrypto.wipeBytes(sharedSecret)

        return copy(
            receiveRootKey = receiveRootKey,
            receiveChainKey = receiveChainKey,
            receiveDhPublic = senderDhPublic.copyOf(),
            receiveCounter = 0,
            updatedAt = System.currentTimeMillis()
        ).also { chatSessionDao.upsert(it) }
    }

    private fun ownRootKey(sxId: String, ownDhPublic: ByteArray): ByteArray {
        val identity = StealthXIdentity.createPublicKeyBundle(context)
        val seed = identity.ed25519PublicKey + ownDhPublic + sxId.toByteArray(Charsets.UTF_8)
        return ChameleonCrypto.hkdf(
            ikm = seed,
            salt = null,
            info = INFO_ROOT_KEY,
            length = ChameleonCrypto.KEY_BYTES
        )
    }

    private fun kdfChain(chainKey: ByteArray, rootKey: ByteArray?): ChainStep {
        val messageKey = ChameleonCrypto.hkdf(
            ikm = chainKey,
            salt = rootKey,
            info = INFO_MESSAGE_KEY,
            length = ChameleonCrypto.KEY_BYTES
        )
        val nextChainKey = ChameleonCrypto.hkdf(
            ikm = chainKey,
            salt = rootKey,
            info = INFO_CHAIN_KEY,
            length = ChameleonCrypto.KEY_BYTES
        )
        return ChainStep(messageKey, nextChainKey)
    }

    private companion object {
        const val MAX_SKIP = 100
        val INFO_ROOT_KEY = "SecureChatRatchetRoot:v1".toByteArray(Charsets.UTF_8)
        val INFO_CHAIN_KEY = "SecureChatRatchetChain:v1".toByteArray(Charsets.UTF_8)
        val INFO_MESSAGE_KEY = "SecureChatRatchetMessage:v1".toByteArray(Charsets.UTF_8)
    }
}

private data class ChainStep(
    val messageKey: ByteArray,
    val nextChainKey: ByteArray
)
