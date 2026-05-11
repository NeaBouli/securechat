/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EncryptedSharedPreferences wrapper.
 *
 * Uses AndroidX Security Crypto with MasterKey (AES-256-GCM in Keystore).
 * All values are encrypted at rest.
 *
 * SECURITY:
 * - MasterKey is hardware-backed (Keystore)
 * - Scheme: AES256_SIV for keys, AES256_GCM for values
 * - NO plaintext database keys stored here
 * - Used for non-sensitive app preferences only
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREFS_NAME = "chameleon_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_SECURITY_LEVEL_DEFAULT = "security_level_default"
        private const val KEY_IFR_WALLET = "ifr_wallet_address"
        private const val KEY_IFR_VERIFICATION_METHOD = "ifr_verification_method"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_STEALTH_DELETE_ENABLED = "stealth_delete_enabled"
    }

    var isOnboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var defaultSecurityLevel: String
        get() = prefs.getString(KEY_SECURITY_LEVEL_DEFAULT, "PROTECTED") ?: "PROTECTED"
        set(value) = prefs.edit().putString(KEY_SECURITY_LEVEL_DEFAULT, value).apply()

    var ifrWalletAddress: String?
        get() = prefs.getString(KEY_IFR_WALLET, null)
        set(value) = prefs.edit().putString(KEY_IFR_WALLET, value).apply()

    var ifrVerificationMethod: String?
        get() = prefs.getString(KEY_IFR_VERIFICATION_METHOD, null)
        set(value) = prefs.edit().putString(KEY_IFR_VERIFICATION_METHOD, value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var stealthDeleteEnabled: Boolean
        get() = prefs.getBoolean(KEY_STEALTH_DELETE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_STEALTH_DELETE_ENABLED, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
