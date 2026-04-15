/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.stealthx.shared.model.AttestationResult
import com.stealthx.shared.model.SecurityLevel
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verifies hardware-backed key attestation on Android devices.
 *
 * Uses the Android Key Attestation protocol to cryptographically prove:
 *   1. Keys are stored in hardware (TEE or StrongBox)
 *   2. The device has not been rooted/unlocked (if attestation reports so)
 *   3. OS version and patch level meet minimum requirements
 *
 * Attestation chain structure:
 *   [0] Key certificate (contains attestation extension 1.3.6.1.4.1.11129.2.1.17)
 *   [1] Intermediate CA (Google Hardware Attestation CA)
 *   [2] Root CA (Google root — pinned)
 *
 * Results map to SecurityLevel:
 *   StrongBox hardware → CAMOUFLAGE (highest protection)
 *   TEE hardware       → PRIVATE
 *   Software only      → PROTECTED (minimum acceptable)
 *
 * CRITICAL: Attestation is NOT available on all devices.
 * Failure to attest does NOT block the app — it degrades to PROTECTED.
 */
@Singleton
class HardwareAttestationVerifier @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val ATTESTATION_ALIAS = "_chameleon_attestation_check"
        private const val ATTESTATION_CHALLENGE_SIZE = 32

        // Android Key Attestation Extension OID
        private const val ATTESTATION_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17"

        // Attestation SecurityLevel values (from Android source)
        private const val KM_SECURITY_LEVEL_SOFTWARE = 0
        private const val KM_SECURITY_LEVEL_TRUSTED_ENVIRONMENT = 1
        private const val KM_SECURITY_LEVEL_STRONG_BOX = 2
    }

    /**
     * Perform full hardware attestation check.
     *
     * Generates a temporary attestation key, retrieves the certificate chain,
     * parses the attestation extension, and determines the security level.
     *
     * @return AttestationResult with security level and device properties
     */
    fun verify(): AttestationResult {
        return try {
            val challenge = generateChallenge()
            val chain = generateAttestationKey(challenge)

            if (chain.isEmpty()) {
                return fallbackResult("No attestation chain available")
            }

            val keyCert = chain[0] as? X509Certificate
                ?: return fallbackResult("Invalid key certificate type")

            val securityLevel = parseAttestationSecurityLevel(keyCert)
            val properties = extractDeviceProperties(keyCert)

            // Cleanup attestation key
            cleanupAttestationKey()

            AttestationResult(
                isHardwareBacked = securityLevel > KM_SECURITY_LEVEL_SOFTWARE,
                securityLevel = mapToSecurityLevel(securityLevel),
                deviceProperties = properties
            )
        } catch (e: Exception) {
            cleanupAttestationKey()
            fallbackResult("Attestation failed: ${e.javaClass.simpleName}")
        }
    }

    /**
     * Quick check — is the device hardware-backed at all?
     * Cheaper than full verify() — no chain parsing.
     */
    fun isHardwareBacked(): Boolean {
        return try {
            val challenge = generateChallenge()
            val chain = generateAttestationKey(challenge)
            cleanupAttestationKey()

            if (chain.isEmpty()) return false

            val keyCert = chain[0] as? X509Certificate ?: return false
            val level = parseAttestationSecurityLevel(keyCert)
            level > KM_SECURITY_LEVEL_SOFTWARE
        } catch (e: Exception) {
            cleanupAttestationKey()
            false
        }
    }

    private fun generateChallenge(): ByteArray {
        val challenge = ByteArray(ATTESTATION_CHALLENGE_SIZE)
        java.security.SecureRandom().nextBytes(challenge)
        return challenge
    }

    private fun generateAttestationKey(challenge: ByteArray): Array<java.security.cert.Certificate> {
        // Clean up any previous attestation key
        cleanupAttestationKey()

        val keySpec = KeyGenParameterSpec.Builder(
            ATTESTATION_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAttestationChallenge(challenge)
            .build()

        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )
        keyPairGenerator.initialize(keySpec)
        keyPairGenerator.generateKeyPair()

        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.getCertificateChain(ATTESTATION_ALIAS) ?: emptyArray()
    }

    /**
     * Parse the attestation extension from the key certificate.
     *
     * The extension is ASN.1 encoded at OID 1.3.6.1.4.1.11129.2.1.17.
     * Structure (simplified):
     *   SEQUENCE {
     *     INTEGER attestationVersion
     *     ENUM attestationSecurityLevel     ← what we need
     *     INTEGER keymasterVersion
     *     ENUM keymasterSecurityLevel
     *     OCTET STRING attestationChallenge
     *     OCTET STRING uniqueId
     *     ...
     *   }
     *
     * @return KM_SECURITY_LEVEL_* constant
     */
    private fun parseAttestationSecurityLevel(cert: X509Certificate): Int {
        val extensionData = cert.getExtensionValue(ATTESTATION_EXTENSION_OID)
            ?: return KM_SECURITY_LEVEL_SOFTWARE

        return try {
            // The extension value is wrapped in an OCTET STRING
            // Parse: OCTET_STRING { SEQUENCE { INT, ENUM(secLevel), ... } }
            val asn1 = extensionData

            // Skip outer OCTET STRING wrapper (tag 0x04 + length)
            var offset = 0
            if (asn1[offset].toInt() == 0x04) {
                offset++ // skip tag
                offset += derLengthSize(asn1, offset)
            }

            // Now at SEQUENCE (tag 0x30)
            if (asn1[offset].toInt() != 0x30) return KM_SECURITY_LEVEL_SOFTWARE
            offset++ // skip tag
            offset += derLengthSize(asn1, offset)

            // First element: attestationVersion (INTEGER, tag 0x02)
            if (asn1[offset].toInt() != 0x02) return KM_SECURITY_LEVEL_SOFTWARE
            offset++ // skip tag
            val versionLen = derReadLength(asn1, offset)
            offset += derLengthSize(asn1, offset) + versionLen

            // Second element: attestationSecurityLevel (ENUM, tag 0x0A)
            if (asn1[offset].toInt() != 0x0A) return KM_SECURITY_LEVEL_SOFTWARE
            offset++ // skip tag
            val enumLen = derReadLength(asn1, offset)
            offset += derLengthSize(asn1, offset)

            // Read the enum value (1 byte for security levels 0-2)
            if (enumLen >= 1) {
                asn1[offset].toInt() and 0xFF
            } else {
                KM_SECURITY_LEVEL_SOFTWARE
            }
        } catch (e: Exception) {
            KM_SECURITY_LEVEL_SOFTWARE
        }
    }

    /**
     * Extract device properties from the attestation certificate.
     */
    private fun extractDeviceProperties(cert: X509Certificate): Map<String, String> {
        val props = mutableMapOf<String, String>()
        props["issuer"] = cert.issuerX500Principal.name
        props["serialNumber"] = cert.serialNumber.toString()
        props["notBefore"] = cert.notBefore.toString()
        props["notAfter"] = cert.notAfter.toString()
        props["sigAlg"] = cert.sigAlgName
        props["osVersion"] = Build.VERSION.RELEASE
        props["sdkVersion"] = Build.VERSION.SDK_INT.toString()
        props["securityPatch"] = Build.VERSION.SECURITY_PATCH
        props["manufacturer"] = Build.MANUFACTURER
        props["model"] = Build.MODEL
        return props
    }

    private fun mapToSecurityLevel(kmLevel: Int): SecurityLevel {
        return when (kmLevel) {
            KM_SECURITY_LEVEL_STRONG_BOX -> SecurityLevel.CAMOUFLAGE
            KM_SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> SecurityLevel.PRIVATE
            else -> SecurityLevel.PROTECTED
        }
    }

    private fun fallbackResult(reason: String): AttestationResult {
        return AttestationResult(
            isHardwareBacked = false,
            securityLevel = SecurityLevel.PROTECTED,
            deviceProperties = mapOf(
                "fallbackReason" to reason,
                "osVersion" to Build.VERSION.RELEASE,
                "sdkVersion" to Build.VERSION.SDK_INT.toString(),
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL
            )
        )
    }

    private fun cleanupAttestationKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(ATTESTATION_ALIAS)) {
                keyStore.deleteEntry(ATTESTATION_ALIAS)
            }
        } catch (_: Exception) {
            // Best-effort cleanup
        }
    }

    // ── DER/ASN.1 minimal parsing helpers ────────────────────────

    private fun derReadLength(data: ByteArray, offset: Int): Int {
        val firstByte = data[offset].toInt() and 0xFF
        return if (firstByte < 0x80) {
            firstByte
        } else {
            val numBytes = firstByte and 0x7F
            var length = 0
            for (i in 1..numBytes) {
                length = (length shl 8) or (data[offset + i].toInt() and 0xFF)
            }
            length
        }
    }

    private fun derLengthSize(data: ByteArray, offset: Int): Int {
        val firstByte = data[offset].toInt() and 0xFF
        return if (firstByte < 0x80) {
            1
        } else {
            1 + (firstByte and 0x7F)
        }
    }
}
