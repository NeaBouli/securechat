/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.keys

import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.shared.model.PublicKeyBundle
import java.time.Instant

/**
 * X25519 Key Manager — generates key pairs and public bundles for key exchange.
 *
 * All crypto delegated to ChameleonCrypto in :stealthx-crypto.
 * Public bundles are Ed25519-signed for authentication.
 */
class X25519KeyManager {

    /**
     * Generate a fresh X25519 key pair.
     * @return Pair(publicKey 32B, privateKey 32B)
     */
    fun generateKeyPair(): Pair<ByteArray, ByteArray> {
        return ChameleonCrypto.generateX25519KeyPair()
    }

    /**
     * Create a signed public key bundle for QR/NFC exchange.
     *
     * @param identityKeyPair  Ed25519 identity key pair (pub, priv)
     * @param dhKeyPair        X25519 session key pair (pub, priv)
     * @param displayName      Human-readable contact name
     * @return                 PublicKeyBundle with Ed25519 signature
     */
    fun createPublicBundle(
        identityKeyPair: Pair<ByteArray, ByteArray>,
        dhKeyPair: Pair<ByteArray, ByteArray>,
        displayName: String
    ): PublicKeyBundle {
        val signature = ChameleonCrypto.sign(dhKeyPair.first, identityKeyPair.second)

        return PublicKeyBundle(
            identityKey = identityKeyPair.first.copyOf(),
            dhPublicKey = dhKeyPair.first.copyOf(),
            signature = signature,
            displayName = displayName,
            createdAt = Instant.now().epochSecond
        )
    }

    /**
     * Verify a received public key bundle.
     *
     * @param bundle  PublicKeyBundle from contact
     * @return        true if Ed25519 signature is valid
     */
    fun verifyBundle(bundle: PublicKeyBundle): Boolean {
        return ChameleonCrypto.verify(
            message = bundle.dhPublicKey,
            signature = bundle.signature,
            publicKey = bundle.identityKey
        )
    }

    /**
     * Compute session key from our private key + their public key.
     *
     * @param myPrivateKey     Our X25519 private key
     * @param theirPublicKey   Their X25519 public key
     * @return                 32-byte shared secret (WIPE AFTER USE)
     */
    fun computeSessionKey(myPrivateKey: ByteArray, theirPublicKey: ByteArray): ByteArray {
        return ChameleonCrypto.computeSharedSecret(myPrivateKey, theirPublicKey)
    }
}
