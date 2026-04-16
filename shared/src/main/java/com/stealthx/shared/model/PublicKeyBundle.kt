/*
 * SecureChat — Public Key Bundle
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Signed public key bundle — shared with contacts to establish
 * end-to-end encrypted messaging channel via QR / NFC.
 *
 * Pure Kotlin / JVM — no Android imports. Base64/URI helpers
 * live in :data (PublicKeyBundleQr).
 */
package com.stealthx.shared.model

data class PublicKeyBundle(
    val sxId: String,                  // sx_a7Kx9mPq2
    val customHandle: String?,         // @username (optional)
    val x25519PublicKey: ByteArray,    // 32 bytes — session key exchange
    val ed25519PublicKey: ByteArray,   // 32 bytes — identity / verification
    val signature: ByteArray,          // Ed25519 signature over the above fields
    val version: Int = 1,
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PublicKeyBundle) return false
        return sxId == other.sxId &&
               x25519PublicKey.contentEquals(other.x25519PublicKey) &&
               ed25519PublicKey.contentEquals(other.ed25519PublicKey) &&
               signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var result = sxId.hashCode()
        result = 31 * result + x25519PublicKey.contentHashCode()
        result = 31 * result + ed25519PublicKey.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}
