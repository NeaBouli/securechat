/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.crypto

import com.goterl.lazysodium.interfaces.AEAD
import com.goterl.lazysodium.interfaces.PwHash
import com.goterl.lazysodium.interfaces.Sign
import com.goterl.lazysodium.interfaces.KeyExchange
// SodiumInitializer is in this package — no import needed
import com.stealthx.shared.model.EncryptedPayload
import java.util.Arrays

/**
 * Primary cryptographic engine for Chameleon.
 *
 * Algorithm: XChaCha20-Poly1305 (IETF variant)
 * Nonce: 24 bytes, always fresh via SecureRandom
 * Key: 32 bytes (256-bit)
 * AAD: Caller-provided context (e.g. packageName) for binding
 *
 * RULES:
 * - This is the ONLY place crypto happens in Chameleon.
 * - NEVER use AES-GCM. NEVER use BouncyCastle.
 * - All keys must be wiped after use via wipeBytes().
 */
object ChameleonCrypto {

    private val sodium get() = SodiumInitializer.sodium

    // ── Constants ────────────────────────────────────────────────
    const val KEY_BYTES      = 32  // 256-bit key
    const val NONCE_BYTES    = 24  // XChaCha20 nonce (not 12! not AES-GCM!)
    const val TAG_BYTES      = 16  // Poly1305 auth tag
    const val BLOCK_SIZE     = 256 // Message padding block size in bytes
    const val ARGON2_MEM_KB  = 65536L  // 64 MB
    const val ARGON2_OPS     = 3L
    const val ARGON2_THREADS = 4
    const val SALT_BYTES     = 32

    // ── XChaCha20-Poly1305 Encryption ───────────────────────────

    /**
     * Encrypt plaintext with XChaCha20-Poly1305.
     * Message is padded to BLOCK_SIZE boundary before encryption.
     * AAD binds ciphertext to context (e.g. packageName).
     *
     * @param plaintext  Raw bytes to encrypt
     * @param key        32-byte symmetric key (wiped after use by caller)
     * @param aad        Additional authenticated data (not encrypted, but authenticated)
     * @return           EncryptedPayload with ciphertext, nonce, padded length
     */
    fun encrypt(plaintext: ByteArray, key: ByteArray, aad: ByteArray = ByteArray(0)): EncryptedPayload {
        require(key.size == KEY_BYTES) { "Key must be $KEY_BYTES bytes, got ${key.size}" }

        // 1. Pad to block boundary BEFORE encryption (prevents size analysis attacks)
        val padded = padToBlock(plaintext)

        // 2. Fresh nonce — NEVER reuse
        val nonce = sodium.randomBytesBuf(NONCE_BYTES)

        // 3. Encrypt
        val ciphertext = ByteArray(padded.size + TAG_BYTES)
        val ciphertextLen = LongArray(1)

        val success = sodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(
            ciphertext, ciphertextLen,
            padded, padded.size.toLong(),
            aad, aad.size.toLong(),
            null, // nsec — not used
            nonce, key
        )

        check(success) { "XChaCha20-Poly1305 encryption failed" }

        // 4. Wipe padded plaintext from memory
        wipeBytes(padded)

        return EncryptedPayload(
            ciphertext    = ciphertext,
            nonce         = nonce,
            paddedLength  = padded.size,
            aad           = aad,
            algorithm     = "XChaCha20-Poly1305",
            version       = 1
        )
    }

    /**
     * Decrypt an EncryptedPayload.
     * Verifies authentication tag — throws if tampered.
     *
     * @param payload    EncryptedPayload from encrypt()
     * @param key        32-byte symmetric key
     * @return           Original plaintext (unpadded)
     * @throws SecurityException if authentication fails (tampered ciphertext)
     */
    fun decrypt(payload: EncryptedPayload, key: ByteArray): ByteArray {
        require(key.size == KEY_BYTES) { "Key must be $KEY_BYTES bytes" }
        require(payload.algorithm == "XChaCha20-Poly1305") {
            "Unsupported algorithm: ${payload.algorithm}"
        }

        val decrypted = ByteArray(payload.ciphertext.size - TAG_BYTES)
        val decryptedLen = LongArray(1)

        val success = sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
            decrypted, decryptedLen,
            null, // nsec — not used
            payload.ciphertext, payload.ciphertext.size.toLong(),
            payload.aad, payload.aad.size.toLong(),
            payload.nonce, key
        )

        if (!success) {
            wipeBytes(decrypted)
            throw SecurityException(
                "XChaCha20-Poly1305 decryption failed — authentication tag mismatch. " +
                "Ciphertext may be tampered."
            )
        }

        // Remove padding — return original plaintext
        return unpad(decrypted, decryptedLen[0].toInt())
    }

    // ── Argon2id Key Derivation ──────────────────────────────────

    /**
     * Derive a cryptographic key from a password using Argon2id.
     * Memory-hard: protects against GPU/ASIC brute force.
     *
     * Parameters (from architecture plan):
     *   Memory: 64 MB (65536 KB)
     *   Iterations: 3
     *   Parallelism: 4
     *
     * CRITICAL: password char array is wiped after use.
     *
     * @param password   User password as char array (wiped after derivation)
     * @param salt       32-byte random salt (generate with generateSalt())
     * @param keyLength  Output key length in bytes (default: KEY_BYTES = 32)
     * @return           Derived key bytes
     */
    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        keyLength: Int = KEY_BYTES
    ): ByteArray {
        require(salt.size == SALT_BYTES) { "Salt must be $SALT_BYTES bytes" }
        require(keyLength in 16..64) { "Key length must be 16–64 bytes" }

        val key = ByteArray(keyLength)

        // Convert CharArray to ByteArray for lazysodium API
        val passwordBytes = String(password).toByteArray(Charsets.UTF_8)

        val success = sodium.cryptoPwHash(
            key, keyLength,
            passwordBytes, passwordBytes.size,
            salt,
            ARGON2_OPS,
            com.sun.jna.NativeLong(ARGON2_MEM_KB * 1024L),
            PwHash.Alg.PWHASH_ALG_ARGON2ID13
        )

        // CRITICAL: Wipe password from memory immediately
        wipeBytes(passwordBytes)
        wipeChars(password)

        check(success) { "Argon2id key derivation failed" }

        return key
    }

    /**
     * Generate a cryptographically secure random salt for Argon2id.
     */
    fun generateSalt(): ByteArray = sodium.randomBytesBuf(SALT_BYTES)

    // ── X25519 Key Exchange ──────────────────────────────────────

    /**
     * Generate an X25519 key pair for Diffie-Hellman key exchange.
     * @return Pair(publicKey, privateKey) — each 32 bytes
     */
    fun generateX25519KeyPair(): Pair<ByteArray, ByteArray> {
        val pubKey = ByteArray(KeyExchange.PUBLICKEYBYTES)
        val secKey = ByteArray(KeyExchange.SECRETKEYBYTES)
        sodium.cryptoKxKeypair(pubKey, secKey)
        return Pair(pubKey, secKey)
    }

    /**
     * Compute X25519 shared secret.
     * @param myPrivateKey  Our private key (32 bytes)
     * @param theirPublicKey  Their public key (32 bytes)
     * @return 32-byte shared secret (wipe after use)
     */
    fun computeSharedSecret(myPrivateKey: ByteArray, theirPublicKey: ByteArray): ByteArray {
        val sharedSecret = ByteArray(32)
        sodium.cryptoScalarMult(sharedSecret, myPrivateKey, theirPublicKey)
        return sharedSecret
    }

    // ── Ed25519 Signing ──────────────────────────────────────────

    /**
     * Generate Ed25519 signing key pair.
     * @return Pair(publicKey 32B, privateKey 64B)
     */
    fun generateSigningKeyPair(): Pair<ByteArray, ByteArray> {
        val pubKey = ByteArray(Sign.PUBLICKEYBYTES)
        val secKey = ByteArray(Sign.SECRETKEYBYTES)
        sodium.cryptoSignKeypair(pubKey, secKey)
        return Pair(pubKey, secKey)
    }

    /**
     * Sign a message with Ed25519.
     * @return 64-byte signature
     */
    fun sign(message: ByteArray, privateKey: ByteArray): ByteArray {
        val signature = ByteArray(Sign.BYTES)
        sodium.cryptoSignDetached(signature, message, message.size.toLong(), privateKey)
        return signature
    }

    /**
     * Verify an Ed25519 signature.
     * @return true if valid, false if tampered or invalid
     */
    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        return sodium.cryptoSignVerifyDetached(signature, message, message.size, publicKey)
    }

    // ── HKDF-SHA256 ──────────────────────────────────────────────

    /**
     * HKDF-SHA256 Extract + Expand (RFC 5869).
     * Used by Double Ratchet for root key and chain key derivation.
     *
     * @param ikm   Input keying material
     * @param salt  Optional salt (if null, uses zero-filled key)
     * @param info  Context info string
     * @param length Output length in bytes (max 255 * 32)
     * @return Derived key bytes
     */
    fun hkdf(ikm: ByteArray, salt: ByteArray?, info: ByteArray, length: Int = KEY_BYTES): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")

        // Extract: PRK = HMAC-SHA256(salt, IKM)
        val extractKey = javax.crypto.spec.SecretKeySpec(
            salt ?: ByteArray(32), "HmacSHA256"
        )
        mac.init(extractKey)
        val prk = mac.doFinal(ikm)

        // Expand: OKM = T(1) || T(2) || ...
        val output = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var counter: Byte = 1

        while (offset < length) {
            val expandKey = javax.crypto.spec.SecretKeySpec(prk, "HmacSHA256")
            mac.init(expandKey)
            mac.update(t)
            mac.update(info)
            mac.update(byteArrayOf(counter))
            t = mac.doFinal()

            val copyLen = minOf(t.size, length - offset)
            System.arraycopy(t, 0, output, offset, copyLen)
            offset += copyLen
            counter++
        }

        wipeBytes(prk)
        return output
    }

    // ── Random ───────────────────────────────────────────────────

    fun randomBytes(size: Int): ByteArray = sodium.randomBytesBuf(size)

    fun randomKey(): ByteArray = randomBytes(KEY_BYTES)

    // ── Padding ───────────────────────────────────────────────────

    /**
     * Pad plaintext to the next BLOCK_SIZE boundary.
     * Prevents message size analysis attacks.
     */
    internal fun padToBlock(data: ByteArray): ByteArray {
        val targetLen = ((data.size / BLOCK_SIZE) + 1) * BLOCK_SIZE
        return data.copyOf(targetLen) // zero-padded by JVM
    }

    /**
     * Remove padding — return only the original data.
     */
    internal fun unpad(padded: ByteArray, originalLength: Int): ByteArray {
        return padded.copyOf(originalLength)
    }

    // ── Memory Wiping ─────────────────────────────────────────────

    /**
     * Overwrite a byte array with zeros.
     * Call on all key material after use.
     */
    fun wipeBytes(data: ByteArray) {
        Arrays.fill(data, 0.toByte())
    }

    /**
     * Overwrite a char array with null chars.
     * Call on all password material after use.
     */
    fun wipeChars(data: CharArray) {
        Arrays.fill(data, '\u0000')
    }
}
