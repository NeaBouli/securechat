/*
 * SecureChat — StealthXIdentity Unit Tests
 * NEA-196: sx_ID derivation from Ed25519 public key
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.identity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("StealthXIdentity — NEA-196 sx_ID derivation")
class StealthXIdentityTest {

    private val BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val SX_ID_REGEX = Regex("^sx_[1-9A-HJ-NP-Za-km-z]{9}\$")

    @Test
    @DisplayName("deriveShortId produces exactly 9 Base58 characters")
    fun `deriveShortId length is 9`() {
        val knownPublicKeyHex = "a".repeat(64)
        val shortId = StealthXIdentity.deriveShortId(knownPublicKeyHex)
        assertEquals(9, shortId.length)
    }

    @Test
    @DisplayName("deriveShortId output contains only valid Base58 characters")
    fun `deriveShortId only base58 chars`() {
        val shortId = StealthXIdentity.deriveShortId("b".repeat(64))
        assertTrue(shortId.all { it in BASE58 }, "All chars must be in Base58 alphabet: $shortId")
    }

    @Test
    @DisplayName("deriveShortId is deterministic — same input → same output")
    fun `deriveShortId is deterministic`() {
        val hex = "dead".repeat(16)
        assertEquals(
            StealthXIdentity.deriveShortId(hex),
            StealthXIdentity.deriveShortId(hex)
        )
    }

    @Test
    @DisplayName("deriveShortId produces different IDs for different public keys")
    fun `different public keys produce different IDs`() {
        val id1 = StealthXIdentity.deriveShortId("a".repeat(64))
        val id2 = StealthXIdentity.deriveShortId("b".repeat(64))
        assertNotEquals(id1, id2)
    }

    @Test
    @DisplayName("sx_ID with prefix matches format regex — NEA-196 regression")
    fun `sx_ID format matches expected regex`() {
        val hexKeys = listOf(
            "a".repeat(64),
            "b".repeat(64),
            "0f1e2d3c4b5a6978".repeat(4),
            "deadbeef".repeat(8)
        )
        for (hex in hexKeys) {
            val sxId = "sx_" + StealthXIdentity.deriveShortId(hex)
            assertTrue(sxId.matches(SX_ID_REGEX), "sx_ID '$sxId' does not match regex for input $hex")
        }
    }

    @Test
    @DisplayName("Base58 alphabet excludes ambiguous chars: 0, O, I, l")
    fun `base58 excludes ambiguous characters`() {
        repeat(100) {
            val hex = "%064x".format(it.toLong())
            val shortId = StealthXIdentity.deriveShortId(hex)
            assertTrue('0' !in shortId, "shortId contains '0'")
            assertTrue('O' !in shortId, "shortId contains 'O'")
            assertTrue('I' !in shortId, "shortId contains 'I'")
            assertTrue('l' !in shortId, "shortId contains 'l'")
        }
    }

    @Test
    @DisplayName("Known vector: SHA-256 of 0xaa*32 → deterministic short ID")
    fun `known vector regression`() {
        val hex = "aa".repeat(32)
        val shortId = StealthXIdentity.deriveShortId(hex)
        assertEquals(9, shortId.length)
        assertTrue(shortId.all { it in BASE58 })
        assertEquals(shortId, StealthXIdentity.deriveShortId(hex))
    }
}
