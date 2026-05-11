/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.data.dao.MessageDao
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.entity.MessageEntity
import com.stealthx.shared.model.EncryptedPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class DecryptedMessage(
    val id: String,
    val contactId: String,
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: Long,
    val deliveryStatus: String
)

data class ConversationSummary(
    val contactId: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int
)

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val contactRepository: ContactRepository
) {
    fun observeMessages(contactId: String): Flow<List<DecryptedMessage>> =
        messageDao.observeForContact(contactId).map { messages ->
            val contact = contactRepository.getById(contactId)
            messages.map { entity -> entity.toDecrypted(contact) }
        }

    fun observeConversationSummaries(contacts: List<ContactKeyEntity>): Flow<List<ConversationSummary>> =
        messageDao.observeLatestPerContact().combine(unreadCounts(contacts)) { latest, unread ->
            val contactById = contacts.associateBy { it.id }
            latest
                .filter { it.contactId in contactById }
                .map { entity ->
                    val text = entity.toDecrypted(contactById[entity.contactId]).text
                    ConversationSummary(
                        contactId = entity.contactId,
                        lastMessage = text,
                        timestamp = entity.sentAt,
                        unreadCount = unread[entity.contactId] ?: 0
                    )
                }
        }

    suspend fun sendLocalMessage(contactId: String, plaintext: String): DecryptedMessage {
        val contact = contactRepository.getById(contactId)
            ?: throw IllegalArgumentException("Contact not found: $contactId")
        val sentAt = System.currentTimeMillis()
        val aad = aadFor(contactId, sentAt)
        val key = localMessageKey(contact)
        val payload = ChameleonCrypto.encrypt(plaintext.toByteArray(Charsets.UTF_8), key, aad)
        ChameleonCrypto.wipeBytes(key)

        val entity = MessageEntity(
            id = UUID.randomUUID().toString(),
            contactId = contactId,
            direction = DIRECTION_OUTGOING,
            ciphertext = payload.ciphertext,
            nonce = payload.nonce,
            aad = payload.aad,
            paddedLength = payload.paddedLength,
            algorithm = payload.algorithm,
            payloadVersion = payload.version,
            sentAt = sentAt,
            deliveryStatus = STATUS_QUEUED
        )
        messageDao.insert(entity)
        return entity.toDecrypted(contact)
    }

    suspend fun markRead(contactId: String) {
        messageDao.markRead(contactId)
    }

    private fun MessageEntity.toDecrypted(contact: ContactKeyEntity?): DecryptedMessage {
        val text = if (contact == null) {
            "Encrypted message"
        } else {
            decryptText(this, contact)
        }
        return DecryptedMessage(
            id = id,
            contactId = contactId,
            text = text,
            isOutgoing = direction == DIRECTION_OUTGOING,
            timestamp = sentAt,
            deliveryStatus = deliveryStatus
        )
    }

    private fun decryptText(entity: MessageEntity, contact: ContactKeyEntity): String {
        val key = localMessageKey(contact)
        return try {
            val payload = EncryptedPayload(
                ciphertext = entity.ciphertext,
                nonce = entity.nonce,
                paddedLength = entity.paddedLength,
                aad = entity.aad,
                algorithm = entity.algorithm,
                version = entity.payloadVersion
            )
            ChameleonCrypto.decrypt(payload, key).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            "Encrypted message"
        } finally {
            ChameleonCrypto.wipeBytes(key)
        }
    }

    private fun unreadCounts(contacts: List<ContactKeyEntity>): Flow<Map<String, Int>> {
        if (contacts.isEmpty()) {
            return kotlinx.coroutines.flow.flowOf(emptyMap())
        }
        val flows = contacts.map { contact ->
            messageDao.observeUnreadCount(contact.id).map { contact.id to it }
        }
        return combine(flows) { pairs -> pairs.toMap() }
    }

    private fun localMessageKey(contact: ContactKeyEntity): ByteArray {
        val ikm = contact.identityKey + contact.dhPublicKey + contact.id.toByteArray(Charsets.UTF_8)
        return ChameleonCrypto.hkdf(
            ikm = ikm,
            salt = null,
            info = "SecureChatLocalMessageKey:v1".toByteArray(Charsets.UTF_8)
        )
    }

    private fun aadFor(contactId: String, sentAt: Long): ByteArray =
        "securechat-msg:v1:$contactId:$sentAt".toByteArray(Charsets.UTF_8)

    private companion object {
        const val DIRECTION_OUTGOING = "OUTGOING"
        const val STATUS_QUEUED = "QUEUED"
    }
}
