package com.stealthx.crypto

import com.stealthx.shared.model.AccessTier
import java.nio.charset.StandardCharsets
import java.util.Base64

data class VerifiedEntitlement(
    val tier: AccessTier,
    val productId: String,
    val expiresAtEpochSeconds: Long
)

object EntitlementTokenVerifier {
    private const val MAX_TOKEN_LENGTH = 4096
    private const val MAX_LIFETIME_SECONDS = 31L * 24 * 60 * 60

    fun verify(
        token: String,
        publicKeyBase64: String,
        expectedAudience: String,
        expectedSubject: String,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000
    ): VerifiedEntitlement {
        require(token.length in 1..MAX_TOKEN_LENGTH) { "Invalid entitlement token length" }
        val parts = token.split('.')
        require(parts.size == 2 && parts.all { it.isNotBlank() }) { "Invalid entitlement token format" }

        val publicKey = decodeUrl(publicKeyBase64)
        val signature = decodeUrl(parts[1])
        require(publicKey.size == 32) { "Invalid entitlement public key" }
        require(signature.size == 64) { "Invalid entitlement signature" }
        val signingInput = parts[0].toByteArray(StandardCharsets.UTF_8)
        if (!ChameleonCrypto.verify(signingInput, signature, publicKey)) {
            throw SecurityException("Entitlement signature verification failed")
        }

        val payload = decodeUrl(parts[0]).toString(StandardCharsets.UTF_8)
        val claims = parseClaims(payload)
        require(claims["v"] == "1" && claims["iss"] == "stealthx") { "Unsupported entitlement issuer" }
        require(claims["aud"] == expectedAudience) { "Entitlement audience mismatch" }
        require(claims["sub"] == expectedSubject) { "Entitlement subject mismatch" }
        val product = requireNotNull(claims["product"]).also {
            require(it.matches(Regex("^securechat_[a-z0-9_]{1,100}$"))) { "Invalid SecureChat entitlement product" }
        }
        val tier = when (claims["tier"]) {
            "PRO" -> AccessTier.PRO
            "ELITE" -> AccessTier.ELITE
            else -> throw IllegalArgumentException("Invalid SecureChat entitlement tier")
        }
        val issuedAt = claims["iat"]?.toLongOrNull() ?: throw IllegalArgumentException("Invalid entitlement issue time")
        val expiresAt = claims["exp"]?.toLongOrNull() ?: throw IllegalArgumentException("Invalid entitlement expiry")
        require(issuedAt <= nowEpochSeconds + 60) { "Entitlement issued in the future" }
        require(expiresAt > nowEpochSeconds) { "Entitlement expired" }
        require(expiresAt > issuedAt && expiresAt - issuedAt <= MAX_LIFETIME_SECONDS) { "Invalid entitlement lifetime" }
        require(claims["order"]?.matches(Regex("^[a-f0-9]{32}$")) == true) { "Invalid entitlement order reference" }

        return VerifiedEntitlement(tier, product, expiresAt)
    }

    private fun decodeUrl(value: String): ByteArray = try {
        Base64.getUrlDecoder().decode(value)
    } catch (error: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid base64url entitlement value", error)
    }

    private fun parseClaims(payload: String): Map<String, String> {
        val lines = payload.split('\n')
        require(lines.size == 9) { "Invalid entitlement claim count" }
        val claims = linkedMapOf<String, String>()
        for (line in lines) {
            val separator = line.indexOf('=')
            require(separator in 1 until line.lastIndex) { "Invalid entitlement claim" }
            val key = line.substring(0, separator)
            val value = line.substring(separator + 1)
            require(key.matches(Regex("^[a-z]+$")) && value.length <= 180 && claims.put(key, value) == null) {
                "Invalid or duplicate entitlement claim"
            }
        }
        require(claims.keys == setOf("v", "iss", "aud", "sub", "tier", "product", "iat", "exp", "order")) {
            "Unexpected entitlement claims"
        }
        return claims
    }
}
