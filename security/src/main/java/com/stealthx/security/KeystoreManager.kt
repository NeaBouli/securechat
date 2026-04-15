/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.annotation.SuppressLint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val PURPOSE_ENCRYPT_DECRYPT =
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        const val PURPOSE_SIGN_VERIFY =
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
    }

    fun getOrCreateAesKey(alias: String, requireAuth: Boolean = true): SecretKey {
        if (keyStore.containsAlias(alias)) {
            return (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        }

        val builder = KeyGenParameterSpec.Builder(alias, PURPOSE_ENCRYPT_DECRYPT)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(requireAuth)
            .setUserAuthenticationValidityDurationSeconds(-1)
        applyStrongBox(builder, isStrongBoxAvailable())

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            .apply { init(builder.build()) }
            .generateKey()
    }

    fun getOrCreateSigningKeyPair(alias: String): KeyPair {
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
            return KeyPair(entry.certificate.publicKey, entry.privateKey)
        }

        val builder = KeyGenParameterSpec.Builder(alias, PURPOSE_SIGN_VERIFY)
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("ED25519"))
            .setDigests(KeyProperties.DIGEST_NONE)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(-1)
        applyStrongBox(builder, isStrongBoxAvailable())

        return KeyPairGenerator.getInstance("EC", KEYSTORE_PROVIDER)
            .apply { initialize(builder.build()) }
            .generateKeyPair()
    }

    fun getOrCreateHmacKey(alias: String): SecretKey {
        if (keyStore.containsAlias(alias)) {
            return (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        }

        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(256)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
        applyStrongBox(builder, false)

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEYSTORE_PROVIDER)
            .apply { init(builder.build()) }
            .generateKey()
    }

    fun deleteKey(alias: String) {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    fun listAllAliases(): List<String> = keyStore.aliases().toList()

    fun containsKey(alias: String): Boolean = keyStore.containsAlias(alias)

    fun isStrongBoxAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            checkStrongBoxSupport()
        } catch (e: StrongBoxUnavailableException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun checkStrongBoxSupport(): Boolean {
        val testAlias = "_sb_test_${System.currentTimeMillis()}"
        val builderObj = KeyGenParameterSpec.Builder(testAlias, PURPOSE_ENCRYPT_DECRYPT)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        // setStrongBoxBacked via reflection to avoid compile-time API level issues
        val method = builderObj.javaClass.getMethod("setStrongBoxBacked", Boolean::class.javaPrimitiveType)
        method.invoke(builderObj, true)
        val spec = builderObj.build()
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            .apply { init(spec) }
            .generateKey()
        keyStore.deleteEntry(testAlias)
        return true
    }

    private fun applyStrongBox(builder: KeyGenParameterSpec.Builder, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val method = builder.javaClass.getMethod("setStrongBoxBacked", Boolean::class.javaPrimitiveType)
                method.invoke(builder, enabled)
            } catch (_: Exception) {
                // StrongBox not available on this API level
            }
        }
    }
}
