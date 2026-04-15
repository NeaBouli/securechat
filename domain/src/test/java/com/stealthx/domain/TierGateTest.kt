/*
 * Chameleon — Domain Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain

import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.domain.tier.TierGateImpl
import com.stealthx.shared.model.IfrTier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TierGate")
class TierGateTest {

    @Test
    @DisplayName("Returns FREE when no cache exists")
    fun `no cache returns free`() = runTest {
        val repo = mockk<IfrTierRepository>()
        coEvery { repo.getCachedTier() } returns IfrTier.FREE
        val gate = TierGateImpl(repo)
        assertEquals(IfrTier.FREE, gate.getTier())
    }

    @Test
    @DisplayName("Returns PRO when valid PRO cache exists")
    fun `valid pro cache returns pro`() = runTest {
        val repo = mockk<IfrTierRepository>()
        coEvery { repo.getCachedTier() } returns IfrTier.PRO
        val gate = TierGateImpl(repo)
        assertEquals(IfrTier.PRO, gate.getTier())
    }

    @Test
    @DisplayName("Returns ELITE when valid ELITE cache exists")
    fun `valid elite cache returns elite`() = runTest {
        val repo = mockk<IfrTierRepository>()
        coEvery { repo.getCachedTier() } returns IfrTier.ELITE
        val gate = TierGateImpl(repo)
        assertEquals(IfrTier.ELITE, gate.getTier())
    }

    @Test
    @DisplayName("HMAC mismatch returns FREE (via repo)")
    fun `hmac mismatch returns free`() = runTest {
        val repo = mockk<IfrTierRepository>()
        // Repo returns FREE when HMAC fails
        coEvery { repo.getCachedTier() } returns IfrTier.FREE
        val gate = TierGateImpl(repo)
        assertEquals(IfrTier.FREE, gate.getTier())
    }

    @Test
    @DisplayName("Expired cache returns FREE (via repo)")
    fun `expired cache returns free`() = runTest {
        val repo = mockk<IfrTierRepository>()
        coEvery { repo.getCachedTier() } returns IfrTier.FREE
        val gate = TierGateImpl(repo)
        assertEquals(IfrTier.FREE, gate.getTier())
    }

    @Test
    @DisplayName("getTierSync returns last known value")
    fun `sync returns last known`() = runTest {
        val repo = mockk<IfrTierRepository>()
        coEvery { repo.getCachedTier() } returns IfrTier.PRO
        val gate = TierGateImpl(repo)
        assertEquals(IfrTier.FREE, gate.getTierSync()) // before first load
        gate.getTier() // load
        assertEquals(IfrTier.PRO, gate.getTierSync()) // after load
    }

    @Test
    @DisplayName("invalidateCache resets to FREE")
    fun `invalidate resets to free`() = runTest {
        val repo = mockk<IfrTierRepository>()
        coEvery { repo.getCachedTier() } returns IfrTier.ELITE
        coEvery { repo.invalidateCache() } returns Unit
        val gate = TierGateImpl(repo)
        gate.getTier()
        assertEquals(IfrTier.ELITE, gate.getTierSync())
        gate.invalidateCache()
        assertEquals(IfrTier.FREE, gate.getTierSync())
        coVerify { repo.invalidateCache() }
    }

    @Test
    @DisplayName("requiresPro is true for PRO and ELITE")
    fun `requiresPro checks`() = runTest {
        val repo = mockk<IfrTierRepository>()
        coEvery { repo.getCachedTier() } returns IfrTier.PRO
        val gate = TierGateImpl(repo)
        assertTrue(gate.requiresPro())
    }

    @Test
    @DisplayName("requiresElite is false for PRO")
    fun `requiresElite false for pro`() = runTest {
        val repo = mockk<IfrTierRepository>()
        coEvery { repo.getCachedTier() } returns IfrTier.PRO
        val gate = TierGateImpl(repo)
        assertFalse(gate.requiresElite())
    }

    @Test
    @DisplayName("isCacheValid delegates to repo")
    fun `cache validity check`() = runTest {
        val repo = mockk<IfrTierRepository>()
        coEvery { repo.isCacheValid() } returns true
        val gate = TierGateImpl(repo)
        assertTrue(gate.isCacheValid())
    }
}
