/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.keys

import com.stealthx.crypto.ChameleonCrypto

/**
 * X25519 Key Manager — generates key pairs for session key exchange.
 *
 * All crypto delegated to ChameleonCrypto in :stealthx-crypto.
 * Public bundle creation and verification lives in
 * [com.stealthx.domain.keyexchange.KeyExchangeManager] which binds
 * the session keys to the unified StealthX identity (sx_ID).
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
