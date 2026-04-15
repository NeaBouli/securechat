/*
 * Chameleon — IFR Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr

import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.ifr.activator.IFRTierActivator
import com.stealthx.ifr.verifier.IFRLockVerifier
import com.stealthx.shared.model.CachedTierResult
import com.stealthx.shared.model.IfrTier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.time.Instant

@DisplayName("IFRTierActivator")
class IFRTierActivatorTest {

    private val verifier = mockk<IFRLockVerifier>()
    private val repo = mockk<IfrTierRepository>()
    private val activator = IFRTierActivator(verifier, repo)

    private val testWallet = "0xAb5801a7D398351b8bE11C439e05C5B3259aeC9B"

    @Test
    @DisplayName("2000 IFR locked → PRO tier activated")
    fun `pro activation`() = runTest {
        coEvery { verifier.getLockedAmount(testWallet) } returns BigInteger("2000000000000")
        coEvery { repo.saveTierResult(any(), any(), any()) } returns Unit

        val result = activator.activate(testWallet)

        assertEquals(IfrTier.PRO, result.tier)
        assertEquals(2_000_000_000_000L, result.lockedAmount)
        assertFalse(result.fromCache)
        assertNull(result.error)
        coVerify { repo.saveTierResult(testWallet, 2_000_000_000_000L, IfrTier.PRO) }
    }

    @Test
    @DisplayName("6000 IFR locked → ELITE tier activated")
    fun `elite activation`() = runTest {
        coEvery { verifier.getLockedAmount(testWallet) } returns BigInteger("6000000000000")
        coEvery { repo.saveTierResult(any(), any(), any()) } returns Unit

        val result = activator.activate(testWallet)

        assertEquals(IfrTier.ELITE, result.tier)
        assertFalse(result.fromCache)
    }

    @Test
    @DisplayName("0 IFR locked → FREE tier")
    fun `zero balance is free`() = runTest {
        coEvery { verifier.getLockedAmount(testWallet) } returns BigInteger.ZERO
        coEvery { repo.saveTierResult(any(), any(), any()) } returns Unit

        val result = activator.activate(testWallet)

        assertEquals(IfrTier.FREE, result.tier)
        assertEquals(0L, result.lockedAmount)
    }

    @Test
    @DisplayName("Network error + valid cache → keep cache tier")
    fun `network error with cache keeps tier`() = runTest {
        coEvery { verifier.getLockedAmount(testWallet) } throws Exception("Network timeout")
        coEvery { repo.isCacheValid() } returns true
        coEvery { repo.getCachedResult() } returns CachedTierResult(
            walletAddress = testWallet,
            lockedAmount = 2_000_000_000_000L,
            tier = IfrTier.PRO,
            verifiedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(30 * 86400L),
            hmac = ByteArray(32)
        )

        val result = activator.activate(testWallet)

        assertEquals(IfrTier.PRO, result.tier)
        assertTrue(result.fromCache)
        assertNull(result.error)
    }

    @Test
    @DisplayName("Network error + no cache → FREE")
    fun `network error without cache returns free`() = runTest {
        coEvery { verifier.getLockedAmount(testWallet) } throws Exception("All RPC failed")
        coEvery { repo.isCacheValid() } returns false

        val result = activator.activate(testWallet)

        assertEquals(IfrTier.FREE, result.tier)
        assertFalse(result.fromCache)
        assertNotNull(result.error)
    }

    @Test
    @DisplayName("HMAC is saved via repository (not computed here)")
    fun `hmac delegated to repository`() = runTest {
        coEvery { verifier.getLockedAmount(testWallet) } returns BigInteger("3000000000000")
        coEvery { repo.saveTierResult(any(), any(), any()) } returns Unit

        activator.activate(testWallet)

        // Verify saveTierResult is called (HMAC computed internally by repo)
        coVerify(exactly = 1) { repo.saveTierResult(testWallet, 3_000_000_000_000L, IfrTier.PRO) }
    }
}
