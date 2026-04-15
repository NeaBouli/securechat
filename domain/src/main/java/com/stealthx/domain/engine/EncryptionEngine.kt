/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.engine

import com.stealthx.shared.model.EncryptedPayload

/**
 * EncryptionEngine — domain interface for text/file encryption.
 *
 * Implementation delegates to :stealthx-crypto ChameleonCrypto.
 * NO crypto code in this interface or its domain callers.
 */
interface EncryptionEngine {

    fun encryptText(plaintext: String, key: ByteArray, packageName: String): EncryptedPayload

    fun decryptText(payload: EncryptedPayload, key: ByteArray): String

    fun encryptFile(data: ByteArray, key: ByteArray, fileName: String): EncryptedPayload

    fun decryptFile(payload: EncryptedPayload, key: ByteArray): ByteArray
}
