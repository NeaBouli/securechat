/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.engine

import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.shared.model.EncryptedPayload

/**
 * XChaCha20-Poly1305 implementation of EncryptionEngine.
 *
 * Delegates ALL crypto to ChameleonCrypto in :stealthx-crypto.
 * AAD = packageName (binds ciphertext to app context).
 * Padding applied before encryption (256-byte boundary) by ChameleonCrypto.
 */
class XChaCha20EncryptionEngine : EncryptionEngine {

    override fun encryptText(plaintext: String, key: ByteArray, packageName: String): EncryptedPayload {
        return ChameleonCrypto.encrypt(
            plaintext = plaintext.toByteArray(Charsets.UTF_8),
            key = key,
            aad = packageName.toByteArray(Charsets.UTF_8)
        )
    }

    override fun decryptText(payload: EncryptedPayload, key: ByteArray): String {
        val plainBytes = ChameleonCrypto.decrypt(payload, key)
        return String(plainBytes, Charsets.UTF_8)
    }

    override fun encryptFile(data: ByteArray, key: ByteArray, fileName: String): EncryptedPayload {
        return ChameleonCrypto.encrypt(
            plaintext = data,
            key = key,
            aad = fileName.toByteArray(Charsets.UTF_8)
        )
    }

    override fun decryptFile(payload: EncryptedPayload, key: ByteArray): ByteArray {
        return ChameleonCrypto.decrypt(payload, key)
    }
}
