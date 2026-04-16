/*
 * Chameleon — Shared Data Models
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.shared.model

import java.time.Instant
import java.util.UUID

// ── Crypto Models ─────────────────────────────────────────────

/**
 * Encrypted payload produced by ChameleonCrypto.encrypt().
 * Carries everything needed to decrypt independently.
 */
data class EncryptedPayload(
    val ciphertext:   ByteArray,
    val nonce:        ByteArray,       // 24 bytes for XChaCha20
    val paddedLength: Int,             // original padded length before encryption
    val aad:          ByteArray,       // additional authenticated data
    val algorithm:    String,          // "XChaCha20-Poly1305"
    val version:      Int              // payload format version for migration
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedPayload) return false
        return ciphertext.contentEquals(other.ciphertext) &&
               nonce.contentEquals(other.nonce) &&
               algorithm == other.algorithm &&
               version == other.version
    }
    override fun hashCode(): Int = ciphertext.contentHashCode()
}

/**
 * A Double Ratchet message — encrypted payload + ratchet headers.
 */
data class RatchetMessage(
    val dhPublicKey:  ByteArray,       // sender's current DH public key
    val counter:      Int,             // message counter in current sending chain
    val prevCounter:  Int,             // messages in previous sending chain
    val payload:      EncryptedPayload
)

// ── Security Models ───────────────────────────────────────────

/**
 * Security levels — from least to most protected.
 * RuleEngine always resolves to the HIGHEST applicable level (Fail Secure).
 */
enum class SecurityLevel(val displayName: String, val colorHex: String) {
    PUBLIC    ("Public",     "#4CAF50"),  // Green
    PROTECTED ("Protected",  "#FFC107"),  // Yellow
    PRIVATE   ("Private",    "#FF5722"),  // Orange
    CAMOUFLAGE("Camouflage", "#F44336")  // Red
}

/**
 * IFR Token access tiers.
 * Gated via TierGate.kt in :domain — nowhere else.
 */
enum class IfrTier(val minLockAmount: Long) {
    FREE  (0L),
    PRO   (2_000_000_000_000L),    // 2,000 IFR × 10^9
    ELITE (6_000_000_000_000L)     // 6,000 IFR × 10^9
}

/**
 * Result of hardware attestation check.
 */
data class AttestationResult(
    val isHardwareBacked: Boolean,
    val securityLevel:    SecurityLevel,
    val deviceProperties: Map<String, String>
)

// ── Rule Engine Models ────────────────────────────────────────

enum class TriggerType { APP, LOCATION, TIME, WIFI, BLUETOOTH }

enum class ActionType { SET_LEVEL, AUTO_ENCRYPT, SHOW_DECOY }

/**
 * Trigger context — input to the Rule Engine.
 */
data class TriggerContext(
    val packageName:  String?       = null,
    val wifiSsid:     String?       = null,
    val latitude:     Double?       = null,
    val longitude:    Double?       = null,
    val bluetoothId:  String?       = null,
    val hourOfDay:    Int           = 0,
    val dayOfWeek:    Int           = 0
)

// ── IFR Cache Model ───────────────────────────────────────────

/**
 * Cached IFR verification result.
 * Stored in Room DB with HMAC protection against tampering.
 * Expires after 30 days — triggers re-verification.
 */
data class CachedTierResult(
    val walletAddress: String,        // EIP-55 checksum address
    val lockedAmount:  Long,          // raw amount × 10^9
    val tier:          IfrTier,
    val verifiedAt:    Instant,
    val expiresAt:     Instant,       // verifiedAt + 30 days
    val hmac:          ByteArray      // HMAC-SHA256 over all above fields
)

// ── Unified StealthX Identity ─────────────────────────────────

/**
 * Cross-app contact identity.
 * One sx_ID works in SecureCall AND SecureChat.
 */
data class StealthXContactId(
    val rawId: String,          // sx_a7Kx9mPq2nRt
    val customHandle: String?,  // @username
    val publicKeyHex: String,
    val supportsCall: Boolean = false,
    val supportsChat: Boolean = false
) {
    val displayId: String get() = customHandle ?: rawId
    val hasSecureCall: Boolean get() = supportsCall
    val hasSecureChat: Boolean get() = supportsChat
}

// ── Key Type Enums ────────────────────────────────────────────

enum class KeyType    { SYMMETRIC, ASYMMETRIC_PUBLIC, SHARED_SECRET }
enum class Algorithm  { XCHACHA20_POLY1305, X25519, ED25519, ARGON2ID }
