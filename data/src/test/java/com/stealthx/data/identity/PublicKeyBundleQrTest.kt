/*
 * SecureChat — Public Key Bundle QR Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.identity

import com.stealthx.shared.model.PublicKeyBundle
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PublicKeyBundleQr")
class PublicKeyBundleQrTest {

    @Test
    @DisplayName("roundtrip preserves createdAt for signature validation")
    fun `roundtrip preserves createdAt`() {
        val bundle = PublicKeyBundle(
            sxId = "sx_1234567890",
            customHandle = "@alice",
            x25519PublicKey = ByteArray(32) { it.toByte() },
            ed25519PublicKey = ByteArray(32) { (it + 32).toByte() },
            signature = ByteArray(64) { (it + 64).toByte() },
            createdAt = 1_779_000_000_000L
        )

        val content = PublicKeyBundleQr.toQrContent(bundle)
        val parsed = PublicKeyBundleQr.fromQrContent(content).getOrThrow()

        assertEquals(bundle.sxId, parsed.sxId)
        assertEquals(bundle.customHandle, parsed.customHandle)
        assertEquals(bundle.createdAt, parsed.createdAt)
        assertArrayEquals(bundle.x25519PublicKey, parsed.x25519PublicKey)
        assertArrayEquals(bundle.ed25519PublicKey, parsed.ed25519PublicKey)
        assertArrayEquals(bundle.signature, parsed.signature)
    }

    @Test
    @DisplayName("missing createdAt is rejected")
    fun `missing createdAt is rejected`() {
        val result = PublicKeyBundleQr.fromQrContent(
            "stealthx://add/sx_1234567890?x=AA&e=AA&s=AA"
        )

        assertTrue(result.isFailure)
        assertEquals("Missing createdAt", result.exceptionOrNull()?.message)
    }
}
