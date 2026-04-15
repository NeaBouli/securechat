/*
 * Chameleon — Crypto Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.crypto

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName

@DisplayName("ChameleonCrypto — XChaCha20-Poly1305")
class ChameleonCryptoTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            // Note: On JVM test runner, SodiumInitializer uses desktop libsodium.
            // On Android Robolectric, it uses SodiumAndroid.
            // Claude Code: ensure lazysodium-java is on testRuntimeClasspath for JVM tests.
        }
    }

    @Test
    @DisplayName("Encrypt then decrypt returns original plaintext")
    fun `encrypt decrypt roundtrip`() {
        val key       = ChameleonCrypto.randomKey()
        val plaintext = "Hello, Chameleon!".toByteArray()
        val aad       = "com.whatsapp".toByteArray()

        val payload   = ChameleonCrypto.encrypt(plaintext, key, aad)
        val decrypted = ChameleonCrypto.decrypt(payload, key)

        assertArrayEquals(plaintext, decrypted)
        ChameleonCrypto.wipeBytes(key)
    }

    @Test
    @DisplayName("Same plaintext produces different ciphertext (nonce freshness)")
    fun `nonce is unique per encryption`() {
        val key       = ChameleonCrypto.randomKey()
        val plaintext = "same message".toByteArray()

        val payload1  = ChameleonCrypto.encrypt(plaintext, key)
        val payload2  = ChameleonCrypto.encrypt(plaintext, key)

        assertFalse(payload1.nonce.contentEquals(payload2.nonce),
            "Nonces must be unique — reuse is catastrophic for XChaCha20")
        assertFalse(payload1.ciphertext.contentEquals(payload2.ciphertext),
            "Ciphertexts must differ when nonces differ")

        ChameleonCrypto.wipeBytes(key)
    }

    @Test
    @DisplayName("Tampered ciphertext throws SecurityException")
    fun `tampered ciphertext fails authentication`() {
        val key       = ChameleonCrypto.randomKey()
        val plaintext = "secret data".toByteArray()
        val payload   = ChameleonCrypto.encrypt(plaintext, key)

        // Flip a bit in ciphertext
        val tampered = payload.copy(
            ciphertext = payload.ciphertext.copyOf().also { it[5] = (it[5].toInt() xor 0xFF).toByte() }
        )

        assertThrows(SecurityException::class.java) {
            ChameleonCrypto.decrypt(tampered, key)
        }

        ChameleonCrypto.wipeBytes(key)
    }

    @Test
    @DisplayName("Wrong key throws SecurityException")
    fun `wrong key fails decryption`() {
        val key1      = ChameleonCrypto.randomKey()
        val key2      = ChameleonCrypto.randomKey()
        val plaintext = "classified".toByteArray()
        val payload   = ChameleonCrypto.encrypt(plaintext, key1)

        assertThrows(SecurityException::class.java) {
            ChameleonCrypto.decrypt(payload, key2)
        }
    }

    @Test
    @DisplayName("Message padding rounds up to BLOCK_SIZE boundary")
    fun `padding is correct`() {
        assertEquals(256, ChameleonCrypto.padToBlock(ByteArray(1)).size)
        assertEquals(256, ChameleonCrypto.padToBlock(ByteArray(255)).size)
        assertEquals(512, ChameleonCrypto.padToBlock(ByteArray(256)).size)
        assertEquals(512, ChameleonCrypto.padToBlock(ByteArray(257)).size)
        assertEquals(1024, ChameleonCrypto.padToBlock(ByteArray(768)).size)
    }

    @Test
    @DisplayName("wipeBytes zeroes all bytes")
    fun `wipeBytes zeroes array`() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        ChameleonCrypto.wipeBytes(data)
        assertTrue(data.all { it == 0.toByte() }, "All bytes must be zero after wipe")
    }

    @Test
    @DisplayName("wipeChars zeroes all chars")
    fun `wipeChars zeroes array`() {
        val pass = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
        ChameleonCrypto.wipeChars(pass)
        assertTrue(pass.all { it == '\u0000' }, "All chars must be null after wipe")
    }

    @Test
    @DisplayName("X25519 key exchange produces identical shared secrets")
    fun `x25519 shared secret is symmetric`() {
        val (alicePub, alicePriv) = ChameleonCrypto.generateX25519KeyPair()
        val (bobPub,   bobPriv)   = ChameleonCrypto.generateX25519KeyPair()

        val aliceShared = ChameleonCrypto.computeSharedSecret(alicePriv, bobPub)
        val bobShared   = ChameleonCrypto.computeSharedSecret(bobPriv, alicePub)

        assertArrayEquals(aliceShared, bobShared,
            "X25519 shared secrets must be equal for both parties")

        ChameleonCrypto.wipeBytes(alicePriv)
        ChameleonCrypto.wipeBytes(bobPriv)
        ChameleonCrypto.wipeBytes(aliceShared)
        ChameleonCrypto.wipeBytes(bobShared)
    }

    @Test
    @DisplayName("Ed25519 sign and verify roundtrip")
    fun `ed25519 signature valid`() {
        val (pub, priv) = ChameleonCrypto.generateSigningKeyPair()
        val message     = "key bundle for contact exchange".toByteArray()
        val signature   = ChameleonCrypto.sign(message, priv)

        assertTrue(ChameleonCrypto.verify(message, signature, pub))
    }

    @Test
    @DisplayName("Ed25519 signature fails for tampered message")
    fun `ed25519 signature invalid on tampered message`() {
        val (pub, priv)  = ChameleonCrypto.generateSigningKeyPair()
        val message      = "authentic".toByteArray()
        val signature    = ChameleonCrypto.sign(message, priv)
        val tampered     = "tampered".toByteArray()

        assertFalse(ChameleonCrypto.verify(tampered, signature, pub))
    }

    @Test
    @DisplayName("1000 encryptions produce 1000 unique nonces")
    fun `nonce uniqueness at scale`() {
        val key    = ChameleonCrypto.randomKey()
        val nonces = mutableSetOf<String>()
        repeat(1000) {
            val payload = ChameleonCrypto.encrypt("test".toByteArray(), key)
            nonces.add(payload.nonce.contentHashCode().toString() + payload.nonce.size)
        }
        assertEquals(1000, nonces.size, "All 1000 nonces must be unique")
        ChameleonCrypto.wipeBytes(key)
    }
}
