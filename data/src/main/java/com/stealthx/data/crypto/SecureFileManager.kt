/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.crypto

import android.content.Context
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.shared.model.EncryptedPayload
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encrypted file storage.
 *
 * Each file is encrypted with XChaCha20-Poly1305 via ChameleonCrypto.
 * File names are hashed (SHA-256 hex) to prevent information leakage.
 * Encryption key is per-file, derived from a master key.
 *
 * SECURITY:
 * - All files encrypted with XChaCha20-Poly1305 (via :stealthx-crypto)
 * - File names are SHA-256 hashed — no plaintext filenames on disk
 * - No network calls — local filesystem only
 * - Files stored in app-private directory (no external storage)
 */
@Singleton
class SecureFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val secureDir: File by lazy {
        File(context.filesDir, "secure_vault").also { it.mkdirs() }
    }

    /**
     * Write encrypted data to a named file.
     *
     * @param name  Logical file name (will be hashed for storage)
     * @param data  Plaintext data to encrypt
     * @param key   32-byte encryption key
     */
    fun writeEncrypted(name: String, data: ByteArray, key: ByteArray) {
        val payload = ChameleonCrypto.encrypt(data, key, name.toByteArray())
        val file = resolveFile(name)

        FileOutputStream(file).use { fos ->
            // Write format: [4B nonceLen][nonce][4B ciphertextLen][ciphertext][4B paddedLen]
            fos.write(intToBytes(payload.nonce.size))
            fos.write(payload.nonce)
            fos.write(intToBytes(payload.ciphertext.size))
            fos.write(payload.ciphertext)
            fos.write(intToBytes(payload.paddedLength))
        }
    }

    /**
     * Read and decrypt a named file.
     *
     * @param name  Logical file name
     * @param key   32-byte decryption key
     * @return      Decrypted plaintext
     * @throws SecurityException if tampered or wrong key
     */
    fun readEncrypted(name: String, key: ByteArray): ByteArray {
        val file = resolveFile(name)
        if (!file.exists()) throw IllegalStateException("File not found: $name")

        val raw = FileInputStream(file).use { it.readBytes() }
        var offset = 0

        val nonceLen = bytesToInt(raw, offset); offset += 4
        val nonce = raw.copyOfRange(offset, offset + nonceLen); offset += nonceLen
        val ctLen = bytesToInt(raw, offset); offset += 4
        val ciphertext = raw.copyOfRange(offset, offset + ctLen); offset += ctLen
        val paddedLen = bytesToInt(raw, offset)

        val payload = EncryptedPayload(
            ciphertext = ciphertext,
            nonce = nonce,
            paddedLength = paddedLen,
            aad = name.toByteArray(),
            algorithm = "XChaCha20-Poly1305",
            version = 1
        )

        return ChameleonCrypto.decrypt(payload, key)
    }

    /**
     * Check if an encrypted file exists.
     */
    fun exists(name: String): Boolean = resolveFile(name).exists()

    /**
     * Delete an encrypted file.
     */
    fun delete(name: String): Boolean = resolveFile(name).delete()

    /**
     * List all encrypted file hashes.
     */
    fun listFiles(): List<String> = secureDir.list()?.toList() ?: emptyList()

    private fun resolveFile(name: String): File {
        val hash = hashFileName(name)
        return File(secureDir, hash)
    }

    private fun hashFileName(name: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(name.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }

    private fun bytesToInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
               ((data[offset + 1].toInt() and 0xFF) shl 16) or
               ((data[offset + 2].toInt() and 0xFF) shl 8) or
               (data[offset + 3].toInt() and 0xFF)
    }
}
