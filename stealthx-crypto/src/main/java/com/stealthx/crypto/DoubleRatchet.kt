/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.crypto

import com.stealthx.shared.model.EncryptedPayload
import com.stealthx.shared.model.RatchetMessage

/**
 * Double Ratchet protocol implementation.
 *
 * Provides:
 *   - Perfect Forward Secrecy (PFS): compromising one key doesn't expose past messages
 *   - Post-Compromise Security: break-in recovery via DH ratchet steps
 *   - Out-of-order message handling via message key skipping
 *
 * Based on Signal's Double Ratchet spec:
 * https://signal.org/docs/specifications/doubleratchet/
 *
 * Crypto primitives — ALL via ChameleonCrypto (lazysodium):
 *   DH ratchet: X25519
 *   Symmetric ratchet: XChaCha20-Poly1305
 *   KDF: HKDF-SHA256 (via sodium.cryptoKdf*)
 *
 * CRITICAL: Old chain keys MUST be wiped immediately after use.
 * Failure to wipe defeats the purpose of forward secrecy.
 */
class DoubleRatchet private constructor(
    private var rootKey:          ByteArray,
    private var sendChainKey:     ByteArray?,
    private var recvChainKey:     ByteArray?,
    private var sendDhKeyPair:    Pair<ByteArray, ByteArray>, // (pub, priv)
    private var recvDhPublicKey:  ByteArray?,
    private var sendCounter:      Int = 0,
    private var recvCounter:      Int = 0,
    private var prevSendCounter:  Int = 0,
) {

    // Skipped message keys — for out-of-order delivery
    private val skippedKeys = mutableMapOf<Pair<ByteArray, Int>, ByteArray>()

    companion object {

        private const val MAX_SKIP = 100   // max skipped messages before error
        private const val INFO_ROOT     = "ChameleonRatchetRoot_v1"
        private const val INFO_CHAIN    = "ChameleonRatchetChain_v1"
        private const val INFO_MSG      = "ChameleonRatchetMessage_v1"

        /**
         * Initialize ratchet as sender (Alice — initiates key exchange).
         * Call after X25519 shared secret has been established via key exchange.
         *
         * @param sharedSecret  32-byte shared secret from X25519 key exchange
         * @param theirDhPublic Their initial DH public key
         */
        fun initSender(
            sharedSecret: ByteArray,
            theirDhPublic: ByteArray
        ): DoubleRatchet {
            val dhKeyPair = ChameleonCrypto.generateX25519KeyPair()
            val (rootKey, sendChainKey) = kdfRootKey(sharedSecret, dhKeyPair, theirDhPublic)
            return DoubleRatchet(
                rootKey         = rootKey,
                sendChainKey    = sendChainKey,
                recvChainKey    = null,
                sendDhKeyPair   = dhKeyPair,
                recvDhPublicKey = theirDhPublic
            )
        }

        /**
         * Initialize ratchet as receiver (Bob — accepts key exchange).
         *
         * @param sharedSecret  32-byte shared secret from X25519 key exchange
         * @param myDhKeyPair   Our DH key pair (pub, priv)
         */
        fun initReceiver(
            sharedSecret: ByteArray,
            myDhKeyPair: Pair<ByteArray, ByteArray>
        ): DoubleRatchet {
            return DoubleRatchet(
                rootKey         = sharedSecret.copyOf(),
                sendChainKey    = null,
                recvChainKey    = null,
                sendDhKeyPair   = myDhKeyPair,
                recvDhPublicKey = null
            )
        }

        // KDF for root key + chain key derivation via HKDF-SHA256
        private fun kdfRootKey(
            rootKey: ByteArray,
            dhKeyPair: Pair<ByteArray, ByteArray>,
            theirPublicKey: ByteArray
        ): Pair<ByteArray, ByteArray> {
            val dhOutput = ChameleonCrypto.computeSharedSecret(dhKeyPair.second, theirPublicKey)
            // HKDF-SHA256: salt=rootKey, ikm=dhOutput, info=INFO_ROOT, len=64
            val derived = ChameleonCrypto.hkdf(
                ikm = dhOutput,
                salt = rootKey,
                info = INFO_ROOT.toByteArray(),
                length = 64
            )
            val newRootKey = derived.copyOfRange(0, 32)
            val chainKey   = derived.copyOfRange(32, 64)
            ChameleonCrypto.wipeBytes(dhOutput)
            ChameleonCrypto.wipeBytes(derived)
            return Pair(newRootKey, chainKey)
        }
    }

    // ── Send ─────────────────────────────────────────────────────

    /**
     * Encrypt a message for sending.
     * Advances the sending chain key after use (forward secrecy).
     *
     * @param plaintext  Raw bytes to send
     * @param aad        Additional authenticated data
     * @return           RatchetMessage with encrypted payload + headers
     */
    fun encrypt(plaintext: ByteArray, aad: ByteArray = ByteArray(0)): RatchetMessage {
        val (msgKey, newChainKey) = kdfChainKey(
            checkNotNull(sendChainKey) { "Send chain not initialized — perform DH ratchet first" }
        )

        // Wipe old chain key — forward secrecy
        ChameleonCrypto.wipeBytes(sendChainKey!!)
        sendChainKey = newChainKey

        val payload = ChameleonCrypto.encrypt(plaintext, msgKey, aad)
        ChameleonCrypto.wipeBytes(msgKey)

        val msg = RatchetMessage(
            dhPublicKey  = sendDhKeyPair.first.copyOf(),
            counter      = sendCounter,
            prevCounter  = prevSendCounter,
            payload      = payload
        )
        sendCounter++
        return msg
    }

    // ── Receive ───────────────────────────────────────────────────

    /**
     * Decrypt a received RatchetMessage.
     * Performs DH ratchet step if sender has a new DH key.
     *
     * @param message  RatchetMessage from sender
     * @param aad      Additional authenticated data (must match sender's aad)
     * @return         Decrypted plaintext
     * @throws SecurityException if decryption/auth fails
     */
    fun decrypt(message: RatchetMessage, aad: ByteArray = ByteArray(0)): ByteArray {
        // Check skipped message keys first
        val skippedKey = skippedKeys[Pair(message.dhPublicKey, message.counter)]
        if (skippedKey != null) {
            skippedKeys.remove(Pair(message.dhPublicKey, message.counter))
            val plain = ChameleonCrypto.decrypt(message.payload, skippedKey)
            ChameleonCrypto.wipeBytes(skippedKey)
            return plain
        }

        // DH ratchet step if new DH key from sender
        val needsDhRatchet = recvDhPublicKey == null ||
            !message.dhPublicKey.contentEquals(recvDhPublicKey!!)

        if (needsDhRatchet) {
            skipMessageKeys(message.prevCounter)
            dhRatchetStep(message.dhPublicKey)
        }

        skipMessageKeys(message.counter)

        val (msgKey, newChainKey) = kdfChainKey(
            checkNotNull(recvChainKey) { "Receive chain not initialized" }
        )
        ChameleonCrypto.wipeBytes(recvChainKey!!)
        recvChainKey = newChainKey
        recvCounter++

        val plain = ChameleonCrypto.decrypt(message.payload, msgKey)
        ChameleonCrypto.wipeBytes(msgKey)
        return plain
    }

    // ── DH Ratchet Step ───────────────────────────────────────────

    private fun dhRatchetStep(theirNewDhPublic: ByteArray) {
        prevSendCounter = sendCounter
        sendCounter = 0
        recvCounter = 0
        recvDhPublicKey = theirNewDhPublic.copyOf()

        // Receiving chain: root key + DH(our_send, their_new)
        val (newRootKey1, newRecvChain) = kdfRootKey(rootKey, sendDhKeyPair, theirNewDhPublic)
        ChameleonCrypto.wipeBytes(rootKey)
        rootKey = newRootKey1
        recvChainKey = newRecvChain

        // Generate new DH key pair for sending
        val newDhKeyPair = ChameleonCrypto.generateX25519KeyPair()
        ChameleonCrypto.wipeBytes(sendDhKeyPair.second) // wipe old private key
        sendDhKeyPair = newDhKeyPair

        // Sending chain: root key + DH(our_new, their_new)
        val (newRootKey2, newSendChain) = kdfRootKey(rootKey, sendDhKeyPair, theirNewDhPublic)
        ChameleonCrypto.wipeBytes(rootKey)
        rootKey = newRootKey2
        sendChainKey = newSendChain
    }

    // ── Chain Key KDF ─────────────────────────────────────────────

    private fun kdfChainKey(chainKey: ByteArray): Pair<ByteArray, ByteArray> {
        // HKDF-SHA256 chain ratchet: derive message key + next chain key
        val msgKey = ChameleonCrypto.hkdf(
            ikm = chainKey,
            salt = null,
            info = INFO_MSG.toByteArray(),
            length = 32
        )
        val newChain = ChameleonCrypto.hkdf(
            ikm = chainKey,
            salt = null,
            info = INFO_CHAIN.toByteArray(),
            length = 32
        )
        return Pair(msgKey, newChain)
    }

    private fun skipMessageKeys(until: Int) {
        if (recvCounter > until) return
        check(until - recvCounter <= MAX_SKIP) {
            "Too many skipped messages: ${until - recvCounter} > $MAX_SKIP"
        }
        while (recvCounter < until) {
            val (msgKey, newChain) = kdfChainKey(recvChainKey!!)
            ChameleonCrypto.wipeBytes(recvChainKey!!)
            recvChainKey = newChain
            skippedKeys[Pair(recvDhPublicKey!!.copyOf(), recvCounter)] = msgKey
            recvCounter++
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────

    /**
     * Wipe all key material from memory.
     * Call when the session is terminated.
     */
    fun destroy() {
        ChameleonCrypto.wipeBytes(rootKey)
        sendChainKey?.let { ChameleonCrypto.wipeBytes(it) }
        recvChainKey?.let { ChameleonCrypto.wipeBytes(it) }
        ChameleonCrypto.wipeBytes(sendDhKeyPair.second) // private key
        skippedKeys.values.forEach { ChameleonCrypto.wipeBytes(it) }
        skippedKeys.clear()
    }
}
