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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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
    private const val ID_PREFIX = "sx_"

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

    private fun deriveShortId(publicKeyHex: String): String {
        val base58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val bytes = publicKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        return hash.take(9).map { b ->
            base58[((b.toInt() and 0xFF) % 58)]
        }.joinToString("")
    }

    private fun getEncryptedPrefs(context: Context): SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}
