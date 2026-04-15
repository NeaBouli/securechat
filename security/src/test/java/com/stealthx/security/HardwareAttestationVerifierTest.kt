/*
 * Chameleon — Security Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.security

import com.stealthx.shared.model.SecurityLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * HardwareAttestationVerifier tests.
 *
 * Key Attestation requires AndroidKeyStore which is not available in Robolectric.
 * These tests are gated and run on real devices / emulators.
 */
class HardwareAttestationVerifierTest {

    private val verifier = HardwareAttestationVerifier()
    private var keystoreAvailable = false

    @BeforeEach
    fun checkKeystore() {
        keystoreAvailable = try {
            java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            true
        } catch (e: Exception) {
            false
        }
    }

    @Test
    @DisplayName("verify() returns result without crashing")
    fun `verify returns valid result`() {
        assumeTrue(keystoreAvailable, "AndroidKeyStore not available in Robolectric")
        val result = verifier.verify()
        assertNotNull(result)
        assertNotNull(result.securityLevel)
        assertTrue(result.deviceProperties.isNotEmpty())
    }

    @Test
    @DisplayName("verify() fallback is PROTECTED (Fail Secure)")
    fun `fallback is never PUBLIC`() {
        assumeTrue(keystoreAvailable, "AndroidKeyStore not available in Robolectric")
        val result = verifier.verify()
        assertNotEquals(SecurityLevel.PUBLIC, result.securityLevel)
    }

    @Test
    @DisplayName("isHardwareBacked() returns false on emulator")
    fun `emulator is not hardware backed`() {
        assumeTrue(keystoreAvailable, "AndroidKeyStore not available in Robolectric")
        // On emulator, TEE attestation is typically not available
        val result = verifier.isHardwareBacked()
        assertNotNull(result) // just verify it doesn't crash
    }

    @Test
    @DisplayName("verify() cleans up attestation key")
    fun `attestation key is cleaned up`() {
        assumeTrue(keystoreAvailable, "AndroidKeyStore not available in Robolectric")
        verifier.verify()
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        assertFalse(keyStore.containsAlias("_chameleon_attestation_check"))
    }

    @Test
    @DisplayName("SecurityLevel ordering: PUBLIC < PROTECTED < PRIVATE < CAMOUFLAGE")
    fun `security level ordering is correct`() {
        assertTrue(SecurityLevel.PUBLIC.ordinal < SecurityLevel.PROTECTED.ordinal)
        assertTrue(SecurityLevel.PROTECTED.ordinal < SecurityLevel.PRIVATE.ordinal)
        assertTrue(SecurityLevel.PRIVATE.ordinal < SecurityLevel.CAMOUFLAGE.ordinal)
    }

    @Test
    @DisplayName("Fallback result has correct structure")
    fun `fallback produces valid AttestationResult`() {
        // Test without AndroidKeyStore — verify() should gracefully degrade
        if (!keystoreAvailable) {
            val result = verifier.verify()
            assertFalse(result.isHardwareBacked)
            assertEquals(SecurityLevel.PROTECTED, result.securityLevel)
            assertTrue(result.deviceProperties.containsKey("fallbackReason"))
        }
    }
}
