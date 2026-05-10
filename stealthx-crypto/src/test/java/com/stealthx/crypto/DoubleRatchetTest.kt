/*
 * Chameleon — Double Ratchet Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DoubleRatchet — Signal Protocol")
class DoubleRatchetTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            SodiumInitializer.ensureInit()
        }

        // Establishes a shared secret + initialized ratchets for Alice and Bob.
        private fun createSession(): Pair<DoubleRatchet, DoubleRatchet> {
            val (alicePub, alicePriv) = ChameleonCrypto.generateX25519KeyPair()
            val (bobPub,   bobPriv)   = ChameleonCrypto.generateX25519KeyPair()
            val sharedSecret = ChameleonCrypto.computeSharedSecret(alicePriv, bobPub)

            val alice = DoubleRatchet.initSender(sharedSecret.copyOf(), bobPub)
            val bob   = DoubleRatchet.initReceiver(sharedSecret.copyOf(), Pair(bobPub, bobPriv))

            ChameleonCrypto.wipeBytes(sharedSecret)
            ChameleonCrypto.wipeBytes(alicePriv)
            ChameleonCrypto.wipeBytes(bobPriv)
            return Pair(alice, bob)
        }
    }

    @Test
    @DisplayName("Alice sends, Bob decrypts — basic roundtrip")
    fun `alice to bob basic roundtrip`() {
        val (alice, bob) = createSession()
        val plaintext = "Hello Bob!".toByteArray()

        val msg       = alice.encrypt(plaintext)
        val decrypted = bob.decrypt(msg)

        assertArrayEquals(plaintext, decrypted)
        alice.destroy()
        bob.destroy()
    }

    @Test
    @DisplayName("Bob replies to Alice — DH ratchet step")
    fun `bob to alice reply roundtrip`() {
        val (alice, bob) = createSession()

        // Step 1: Alice → Bob (triggers DH ratchet on Bob's side)
        bob.decrypt(alice.encrypt("Hi".toByteArray()))

        // Step 2: Bob → Alice
        val reply    = "Hi back!".toByteArray()
        val replyMsg = bob.encrypt(reply)
        val received = alice.decrypt(replyMsg)

        assertArrayEquals(reply, received)
        alice.destroy()
        bob.destroy()
    }

    @Test
    @DisplayName("Multiple sequential messages in one direction")
    fun `multiple messages alice to bob`() {
        val (alice, bob) = createSession()

        val messages = listOf("msg1", "msg2", "msg3", "msg4", "msg5")
        for (text in messages) {
            val plain   = text.toByteArray()
            val msg     = alice.encrypt(plain)
            val decrypt = bob.decrypt(msg)
            assertArrayEquals(plain, decrypt, "Mismatch for: $text")
        }

        alice.destroy()
        bob.destroy()
    }

    @Test
    @DisplayName("Alternating messages in both directions (DH ratchet each step)")
    fun `alternating messages trigger DH ratchet`() {
        val (alice, bob) = createSession()

        val pairs = listOf("A1" to "B1", "A2" to "B2", "A3" to "B3")
        for ((aTxt, bTxt) in pairs) {
            val aMsg = alice.encrypt(aTxt.toByteArray())
            assertArrayEquals(aTxt.toByteArray(), bob.decrypt(aMsg))

            val bMsg = bob.encrypt(bTxt.toByteArray())
            assertArrayEquals(bTxt.toByteArray(), alice.decrypt(bMsg))
        }

        alice.destroy()
        bob.destroy()
    }

    @Test
    @DisplayName("AAD mismatch fails authentication")
    fun `wrong aad fails decryption`() {
        val (alice, bob) = createSession()

        val msg = alice.encrypt("secret".toByteArray(), aad = "correct-context".toByteArray())

        assertThrows(SecurityException::class.java) {
            bob.decrypt(msg, aad = "wrong-context".toByteArray())
        }

        alice.destroy()
        bob.destroy()
    }

    @Test
    @DisplayName("AAD round-trip: correct AAD decrypts successfully")
    fun `correct aad decrypts`() {
        val (alice, bob) = createSession()
        val aad       = "com.example.app".toByteArray()
        val plaintext = "aad-bound message".toByteArray()

        val msg       = alice.encrypt(plaintext, aad)
        val decrypted = bob.decrypt(msg, aad)

        assertArrayEquals(plaintext, decrypted)
        alice.destroy()
        bob.destroy()
    }

    @Test
    @DisplayName("Each message uses a unique message key (forward secrecy)")
    fun `unique message keys per message`() {
        val (alice, bob) = createSession()

        val msg1 = alice.encrypt("first".toByteArray())
        val msg2 = alice.encrypt("second".toByteArray())

        // Different nonces → different ciphertexts (different message keys)
        assertFalse(
            msg1.payload.ciphertext.contentEquals(msg2.payload.ciphertext),
            "Ciphertexts must differ — different message keys"
        )
        assertFalse(
            msg1.payload.nonce.contentEquals(msg2.payload.nonce),
            "Nonces must be unique"
        )

        // Still decryptable
        assertArrayEquals("first".toByteArray(),  bob.decrypt(msg1))
        assertArrayEquals("second".toByteArray(), bob.decrypt(msg2))

        alice.destroy()
        bob.destroy()
    }

    @Test
    @DisplayName("destroy() — session can be torn down without crash")
    fun `destroy does not throw`() {
        val (alice, bob) = createSession()
        alice.encrypt("last message".toByteArray())
        assertDoesNotThrow {
            alice.destroy()
            bob.destroy()
        }
    }

    @Test
    @DisplayName("Large payload roundtrip (64 KB)")
    fun `large payload roundtrip`() {
        val (alice, bob) = createSession()
        val large = ByteArray(65536) { it.toByte() }

        val msg       = alice.encrypt(large)
        val decrypted = bob.decrypt(msg)

        assertArrayEquals(large, decrypted)
        alice.destroy()
        bob.destroy()
    }
}
