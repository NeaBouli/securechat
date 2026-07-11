package com.stealthx.crypto

import com.stealthx.shared.model.AccessTier
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EntitlementTokenVerifierTest {
    private val now = 1_800_000_000L
    private val keyPair = ChameleonCrypto.generateSigningKeyPair()

    @Test
    fun `valid token is bound to SecureChat device and product`() {
        val token = token(subject = "sx_device_1", tier = "PRO")
        val result = EntitlementTokenVerifier.verify(token, b64(keyPair.first), "securechat", "sx_device_1", now)
        assertEquals(AccessTier.PRO, result.tier)
        assertEquals("securechat_pro_lifetime", result.productId)
    }

    @Test
    fun `tampered expired and copied tokens fail closed`() {
        val valid = token(subject = "sx_device_1", tier = "ELITE")
        assertThrows(SecurityException::class.java) {
            EntitlementTokenVerifier.verify("${valid.substringBefore('.')}x.${valid.substringAfter('.')}", b64(keyPair.first), "securechat", "sx_device_1", now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EntitlementTokenVerifier.verify(valid, b64(keyPair.first), "securechat", "sx_other_device", now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EntitlementTokenVerifier.verify(token(expiresAt = now - 1), b64(keyPair.first), "securechat", "sx_device_1", now)
        }
    }

    private fun token(subject: String = "sx_device_1", tier: String = "PRO", expiresAt: Long = now + 2_592_000): String {
        val payload = listOf(
            "v=1", "iss=stealthx", "aud=securechat", "sub=$subject", "tier=$tier",
            "product=securechat_pro_lifetime", "iat=${now - 10}", "exp=$expiresAt",
            "order=${MessageDigest.getInstance("SHA-256").digest("order".toByteArray()).joinToString("") { "%02x".format(it) }.take(32)}"
        ).joinToString("\n")
        val encoded = b64(payload.toByteArray(StandardCharsets.UTF_8))
        return "$encoded.${b64(ChameleonCrypto.sign(encoded.toByteArray(StandardCharsets.UTF_8), keyPair.second))}"
    }

    private fun b64(value: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value)
}
