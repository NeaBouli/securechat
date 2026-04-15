/*
 * Chameleon — IFR Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr

import com.stealthx.shared.model.IfrTier
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigInteger

@DisplayName("IFRConstants — Tier Calculation")
class IFRConstantsTest {

    @Test
    @DisplayName("0 IFR → FREE")
    fun `zero amount is free`() {
        assertEquals(IfrTier.FREE, IFRConstants.tierFromAmount(BigInteger.ZERO))
    }

    @Test
    @DisplayName("1999 IFR → FREE (below PRO threshold)")
    fun `below pro threshold is free`() {
        assertEquals(IfrTier.FREE, IFRConstants.tierFromAmount(BigInteger("1999999999999")))
    }

    @Test
    @DisplayName("2000 IFR → PRO (exact threshold)")
    fun `exact pro threshold is pro`() {
        assertEquals(IfrTier.PRO, IFRConstants.tierFromAmount(BigInteger("2000000000000")))
    }

    @Test
    @DisplayName("2001 IFR → PRO (above threshold)")
    fun `above pro threshold is pro`() {
        assertEquals(IfrTier.PRO, IFRConstants.tierFromAmount(BigInteger("2001000000000")))
    }

    @Test
    @DisplayName("5999 IFR → PRO (below ELITE)")
    fun `below elite threshold is pro`() {
        assertEquals(IfrTier.PRO, IFRConstants.tierFromAmount(BigInteger("5999999999999")))
    }

    @Test
    @DisplayName("6000 IFR → ELITE (exact threshold)")
    fun `exact elite threshold is elite`() {
        assertEquals(IfrTier.ELITE, IFRConstants.tierFromAmount(BigInteger("6000000000000")))
    }

    @Test
    @DisplayName("10000 IFR → ELITE (above threshold)")
    fun `above elite threshold is elite`() {
        assertEquals(IfrTier.ELITE, IFRConstants.tierFromAmount(BigInteger("10000000000000")))
    }

    @Test
    @DisplayName("PRO threshold constant is 2000 * 10^9")
    fun `pro threshold value`() {
        assertEquals(BigInteger("2000000000000"), IFRConstants.PRO_THRESHOLD)
    }

    @Test
    @DisplayName("ELITE threshold constant is 6000 * 10^9")
    fun `elite threshold value`() {
        assertEquals(BigInteger("6000000000000"), IFRConstants.ELITE_THRESHOLD)
    }

    @Test
    @DisplayName("Contract addresses are valid hex")
    fun `contract addresses format`() {
        assertTrue(IFRConstants.IFR_LOCK_ADDRESS.startsWith("0x"))
        assertEquals(42, IFRConstants.IFR_LOCK_ADDRESS.length)
        assertTrue(IFRConstants.IFR_TOKEN_ADDRESS.startsWith("0x"))
        assertTrue(IFRConstants.BUILDER_REGISTRY_ADDRESS.startsWith("0x"))
    }

    @Test
    @DisplayName("Chain ID is Ethereum Mainnet (1)")
    fun `chain id is mainnet`() {
        assertEquals(1L, IFRConstants.CHAIN_ID)
    }

    @Test
    @DisplayName("Token decimals is 9")
    fun `token decimals`() {
        assertEquals(9, IFRConstants.TOKEN_DECIMALS)
    }

    @Test
    @DisplayName("RPC endpoints are non-empty")
    fun `rpc endpoints exist`() {
        assertTrue(IFRConstants.RPC_ENDPOINTS.isNotEmpty())
        IFRConstants.RPC_ENDPOINTS.forEach { assertTrue(it.startsWith("https://")) }
    }

    @Test
    @DisplayName("Cache validity is 30 days")
    fun `cache validity period`() {
        assertEquals(30L, IFRConstants.CACHE_VALIDITY_DAYS)
    }
}
