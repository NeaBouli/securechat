/*
 * SecureChat — Key Exchange Manager
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Creates and imports PublicKeyBundles via QR/NFC.
 * Computes X25519 ECDH session keys and Safety Numbers.
 */
package com.stealthx.domain.keyexchange

import com.stealthx.shared.model.PublicKeyBundle
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Safety Number: 6 groups of 4 digits — for manual verification
 * between two contacts. Format: "12345 67890 12345 67890 12345 67890"
 */
data class SafetyNumber(val groups: List<String>) {
    val displayString: String get() = groups.joinToString(" ")
    override fun toString(): String = displayString
}

interface KeyExchangeProvider {
    fun getOwnSxId(): String
    fun getOwnCustomHandle(): String?
    fun getOwnX25519PublicKey(): ByteArray
    fun getOwnEd25519PublicKey(): ByteArray
    fun signWithEd25519(data: ByteArray): ByteArray
    fun verifyEd25519(data: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
    fun deriveX25519Shared(ownPrivateX25519: ByteArray, peerPublicX25519: ByteArray): ByteArray
    fun getOwnX25519PrivateKey(): ByteArray
}

@Singleton
class KeyExchangeManager @Inject constructor(
    private val provider: KeyExchangeProvider
) {

    /**
     * Creates a signed PublicKeyBundle to share with contacts.
     */
    fun createPublicBundle(): PublicKeyBundle {
        val sxId = provider.getOwnSxId()
        val handle = provider.getOwnCustomHandle()
        val x25519 = provider.getOwnX25519PublicKey()
        val ed25519 = provider.getOwnEd25519PublicKey()
        val createdAt = System.currentTimeMillis()

        // Sign: sxId || handle || x25519 || ed25519 || createdAt
        val payload = buildSignPayload(sxId, handle, x25519, ed25519, createdAt)
        val signature = provider.signWithEd25519(payload)

        return PublicKeyBundle(
            sxId = sxId,
            customHandle = handle,
            x25519PublicKey = x25519,
            ed25519PublicKey = ed25519,
            signature = signature,
            version = 1,
            createdAt = createdAt
        )
    }

    /**
     * Verifies a received PublicKeyBundle.
     * Returns the verified bundle or failure with reason.
     */
    fun verifyBundle(bundle: PublicKeyBundle): Result<PublicKeyBundle> {
        if (!bundle.sxId.startsWith("sx_")) {
            return Result.failure(IllegalArgumentException("Invalid sx_ ID format"))
        }
        if (bundle.x25519PublicKey.size != 32) {
            return Result.failure(IllegalArgumentException("Invalid X25519 key length"))
        }
        if (bundle.ed25519PublicKey.size != 32) {
            return Result.failure(IllegalArgumentException("Invalid Ed25519 key length"))
        }

        val payload = buildSignPayload(
            bundle.sxId,
            bundle.customHandle,
            bundle.x25519PublicKey,
            bundle.ed25519PublicKey,
            bundle.createdAt
        )
        val valid = provider.verifyEd25519(payload, bundle.signature, bundle.ed25519PublicKey)
        return if (valid) {
            Result.success(bundle)
        } else {
            Result.failure(SecurityException("Ed25519 signature verification failed"))
        }
    }

    /**
     * Computes X25519 ECDH session key for a contact.
     */
    fun computeSessionKey(contactX25519PublicKey: ByteArray): ByteArray {
        val ownPrivate = provider.getOwnX25519PrivateKey()
        return provider.deriveX25519Shared(ownPrivate, contactX25519PublicKey)
    }

    /**
     * Safety Number: SHA-256 hash of both X25519 public keys,
     * formatted as 6 groups of 4 digits for manual comparison.
     */
    fun computeSafetyNumber(ownX25519: ByteArray, contactX25519: ByteArray): SafetyNumber {
        // Order keys deterministically (lower first)
        val (first, second) = if (compareBytes(ownX25519, contactX25519) < 0) {
            ownX25519 to contactX25519
        } else {
            contactX25519 to ownX25519
        }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(first)
        digest.update(second)
        val hash = digest.digest()

        // Take 12 bytes → 24 digits → format as 6x4
        val number = hash.take(12).joinToString("") { b ->
            "%02d".format((b.toInt() and 0xFF) % 100)
        }.take(24)

        val groups = (0 until 6).map { number.substring(it * 4, (it + 1) * 4) }
        return SafetyNumber(groups)
    }

    private fun buildSignPayload(
        sxId: String,
        handle: String?,
        x25519: ByteArray,
        ed25519: ByteArray,
        createdAt: Long
    ): ByteArray {
        val builder = StringBuilder()
        builder.append(sxId)
        builder.append("|")
        builder.append(handle ?: "")
        builder.append("|")
        builder.append(x25519.joinToString("") { "%02x".format(it) })
        builder.append("|")
        builder.append(ed25519.joinToString("") { "%02x".format(it) })
        builder.append("|")
        builder.append(createdAt.toString())
        return builder.toString().toByteArray(Charsets.UTF_8)
    }

    private fun compareBytes(a: ByteArray, b: ByteArray): Int {
        val minLen = minOf(a.size, b.size)
        for (i in 0 until minLen) {
            val diff = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (diff != 0) return diff
        }
        return a.size - b.size
    }
}
