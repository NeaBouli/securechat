/*
 * StealthX Unified Identity System
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * One ID for all StealthX products.
 * Based on Ed25519 Public Key — deterministic,
 * cryptographically secure, no central authority.
 */
package com.stealthx.data.identity

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.shared.model.PublicKeyBundle

data class StealthXId(
    val raw: String,           // sx_a7Kx9mPq2nRt
    val customHandle: String?, // @username (Pro/Elite only)
    val publicKeyHex: String,  // Full Ed25519 Public Key
    val createdAt: Long
) {
    val displayId: String
        get() = customHandle ?: raw

    val deepLink: String
        get() = "stealthx://add/$raw"

    val qrContent: String
        get() = "stealthx://add/$raw"
}

object StealthXIdentity {

    private const val PREFS_NAME = "stealthx_identity"
    private const val KEY_RAW_ID = "raw_id"
    private const val KEY_PUBLIC_KEY = "public_key"
    private const val KEY_CUSTOM_HANDLE = "custom_handle"
    private const val KEY_CREATED_AT = "created_at"
    private const val KEY_X25519_PUBLIC = "x25519_public"
    private const val KEY_X25519_PRIVATE = "x25519_private"
    private const val KEY_ED25519_PUBLIC = "ed25519_public"
    private const val KEY_ED25519_PRIVATE = "ed25519_private"
    private const val ID_PREFIX = "sx_"
    private val B64 = Base64.NO_WRAP

    /**
     * Returns the Unified ID using a random device seed — creates it on first call.
     * No biometric required — seed is stored in EncryptedSharedPreferences.
     * Call from Application.onCreate() before any UI reads the identity.
     */
    fun getOrCreateWithSeed(context: Context): StealthXId {
        val prefs = getEncryptedPrefs(context)
        val existingSeed = prefs.getString("identity_seed", null)
        val seedHex = if (existingSeed != null) {
            existingSeed
        } else {
            val seed = java.security.SecureRandom().generateSeed(32)
            val hex = seed.joinToString("") { "%02x".format(it) }
            prefs.edit().putString("identity_seed", hex).apply()
            hex
        }
        return getOrCreate(context, seedHex)
    }

    /**
     * Returns the Unified ID — creates it on first call.
     * ONE-TIME per device — valid for SecureCall AND SecureChat.
     */
    fun getOrCreate(context: Context, publicKeyHex: String): StealthXId {
        val prefs = getEncryptedPrefs(context)

        val existingId = prefs.getString(KEY_RAW_ID, null)
        if (existingId != null) {
            return StealthXId(
                raw = existingId,
                customHandle = prefs.getString(KEY_CUSTOM_HANDLE, null),
                publicKeyHex = prefs.getString(KEY_PUBLIC_KEY, publicKeyHex)!!,
                createdAt = prefs.getLong(KEY_CREATED_AT, 0L)
            )
        }

        val newId = ID_PREFIX + deriveShortId(publicKeyHex)
        val now = System.currentTimeMillis()

        prefs.edit()
            .putString(KEY_RAW_ID, newId)
            .putString(KEY_PUBLIC_KEY, publicKeyHex)
            .putLong(KEY_CREATED_AT, now)
            .apply()

        return StealthXId(
            raw = newId,
            customHandle = null,
            publicKeyHex = publicKeyHex,
            createdAt = now
        )
    }

    fun setCustomHandle(context: Context, handle: String): Result<Unit> {
        if (!handle.matches(Regex("@[a-zA-Z0-9_]{3,20}"))) {
            return Result.failure(
                IllegalArgumentException("Handle must be @username (3-20 chars)")
            )
        }
        getEncryptedPrefs(context).edit()
            .putString(KEY_CUSTOM_HANDLE, handle)
            .apply()
        return Result.success(Unit)
    }

    fun get(context: Context): StealthXId? {
        val prefs = getEncryptedPrefs(context)
        val rawId = prefs.getString(KEY_RAW_ID, null) ?: return null
        return StealthXId(
            raw = rawId,
            customHandle = prefs.getString(KEY_CUSTOM_HANDLE, null),
            publicKeyHex = prefs.getString(KEY_PUBLIC_KEY, "")!!,
            createdAt = prefs.getLong(KEY_CREATED_AT, 0L)
        )
    }

    fun createPublicKeyBundle(context: Context): PublicKeyBundle {
        val identity = getOrCreateWithSeed(context)
        val prefs = getEncryptedPrefs(context)
        ensureKeyPairs(prefs)
        val x25519 = prefs.getString(KEY_X25519_PUBLIC, null)!!.fromBase64()
        val ed25519 = prefs.getString(KEY_ED25519_PUBLIC, null)!!.fromBase64()
        val edPrivate = prefs.getString(KEY_ED25519_PRIVATE, null)!!.fromBase64()
        val createdAt = identity.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        val payload = buildSignPayload(identity.raw, identity.customHandle, x25519, ed25519, createdAt)
        val signature = ChameleonCrypto.sign(payload, edPrivate)
        ChameleonCrypto.wipeBytes(edPrivate)
        return PublicKeyBundle(
            sxId = identity.raw,
            customHandle = identity.customHandle,
            x25519PublicKey = x25519,
            ed25519PublicKey = ed25519,
            signature = signature,
            version = 1,
            createdAt = createdAt
        )
    }

    private fun deriveShortId(publicKeyHex: String): String {
        val base58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val bytes = publicKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        return hash.take(9).map { b ->
            base58[((b.toInt() and 0xFF) % 58)]
        }.joinToString("")
    }

    private fun ensureKeyPairs(prefs: SharedPreferences) {
        if (prefs.getString(KEY_X25519_PUBLIC, null) != null &&
            prefs.getString(KEY_ED25519_PUBLIC, null) != null
        ) return

        val (xPublic, xPrivate) = ChameleonCrypto.generateX25519KeyPair()
        val (edPublic, edPrivate) = ChameleonCrypto.generateSigningKeyPair()
        prefs.edit()
            .putString(KEY_X25519_PUBLIC, xPublic.toBase64())
            .putString(KEY_X25519_PRIVATE, xPrivate.toBase64())
            .putString(KEY_ED25519_PUBLIC, edPublic.toBase64())
            .putString(KEY_ED25519_PRIVATE, edPrivate.toBase64())
            .apply()
    }

    private fun buildSignPayload(
        sxId: String,
        handle: String?,
        x25519: ByteArray,
        ed25519: ByteArray,
        createdAt: Long
    ): ByteArray {
        return buildString {
            append(sxId)
            append("|")
            append(handle ?: "")
            append("|")
            append(x25519.joinToString("") { "%02x".format(it) })
            append("|")
            append(ed25519.joinToString("") { "%02x".format(it) })
            append("|")
            append(createdAt.toString())
        }.toByteArray(Charsets.UTF_8)
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, B64)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, B64)

    private fun getEncryptedPrefs(context: Context): SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}
