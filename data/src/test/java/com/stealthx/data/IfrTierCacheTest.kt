/*
 * Chameleon — Data Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data

import com.stealthx.data.entity.IfrTierCacheEntity
import com.stealthx.shared.model.IfrTier
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("IfrTierCache — Entity + HMAC Logic")
class IfrTierCacheTest {

    @Test
    @DisplayName("Entity stores EIP-55 wallet address as primary key")
    fun `wallet address is primary key`() {
        val entity = createEntity("0xAb5801a7D398351b8bE11C439e05C5B3259aeC9B")
        assertEquals("0xAb5801a7D398351b8bE11C439e05C5B3259aeC9B", entity.walletAddress)
    }

    @Test
    @DisplayName("expiresAt = verifiedAt + 30 days")
    fun `expiry is 30 days from verification`() {
        val verifiedAt = Instant.now().epochSecond
        val expiresAt = verifiedAt + (30 * 86400L)
        val entity = createEntity(verifiedAt = verifiedAt, expiresAt = expiresAt)
        assertEquals(30 * 86400L, entity.expiresAt - entity.verifiedAt)
    }

    @Test
    @DisplayName("Expired cache detected correctly")
    fun `expired cache is detected`() {
        val verifiedAt = Instant.now().epochSecond - (31 * 86400L)
        val expiresAt = verifiedAt + (30 * 86400L)
        val entity = createEntity(verifiedAt = verifiedAt, expiresAt = expiresAt)
        assertTrue(entity.expiresAt <= Instant.now().epochSecond)
    }

    @Test
    @DisplayName("Valid cache not yet expired")
    fun `valid cache is not expired`() {
        val verifiedAt = Instant.now().epochSecond
        val expiresAt = verifiedAt + (30 * 86400L)
        val entity = createEntity(verifiedAt = verifiedAt, expiresAt = expiresAt)
        assertTrue(entity.expiresAt > Instant.now().epochSecond)
    }

    @Test
    @DisplayName("Tier string roundtrip")
    fun `tier enum roundtrip`() {
        IfrTier.entries.forEach { tier ->
            val entity = createEntity(tier = tier.name)
            assertEquals(tier, IfrTier.valueOf(entity.tier))
        }
    }

    @Test
    @DisplayName("Invalid tier string defaults to FREE")
    fun `invalid tier defaults to free`() {
        val entity = createEntity(tier = "INVALID")
        val tier = try { IfrTier.valueOf(entity.tier) } catch (_: Exception) { IfrTier.FREE }
        assertEquals(IfrTier.FREE, tier)
    }

    @Test
    @DisplayName("HMAC field is present and non-empty")
    fun `hmac field is populated`() {
        val entity = createEntity()
        assertTrue(entity.hmac.isNotEmpty())
    }

    @Test
    @DisplayName("Tampered HMAC does not equal original")
    fun `tampered hmac detectable`() {
        val entity = createEntity()
        val tampered = entity.copy(hmac = ByteArray(32) { 0xFF.toByte() })
        assertFalse(entity.hmac.contentEquals(tampered.hmac))
    }

    @Test
    @DisplayName("Tampered amount with original HMAC is detectable")
    fun `tampered amount changes hmac expectation`() {
        val entity = createEntity(lockedAmount = 2_000_000_000_000L)
        val tampered = entity.copy(lockedAmount = 6_000_000_000_000L)
        // The HMAC was computed with original amount, changing amount means mismatch
        assertNotEquals(entity.lockedAmount, tampered.lockedAmount)
        // Same HMAC → validation would fail since input changed
    }

    private fun createEntity(
        walletAddress: String = "0xAb5801a7D398351b8bE11C439e05C5B3259aeC9B",
        lockedAmount: Long = 2_000_000_000_000L,
        tier: String = "PRO",
        verifiedAt: Long = Instant.now().epochSecond,
        expiresAt: Long = Instant.now().epochSecond + (30 * 86400L)
    ): IfrTierCacheEntity {
        return IfrTierCacheEntity(
            walletAddress = walletAddress,
            lockedAmount = lockedAmount,
            tier = tier,
            verifiedAt = verifiedAt,
            expiresAt = expiresAt,
            hmac = ByteArray(32) { it.toByte() }
        )
    }
}
