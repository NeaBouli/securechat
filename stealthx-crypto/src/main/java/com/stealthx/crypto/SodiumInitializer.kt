/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.crypto

import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.LazySodiumAndroid

/**
 * Singleton initializer for libsodium via lazysodium.
 *
 * MUST be called in Application.onCreate() before any crypto operation.
 * Thread-safe via @Synchronized. Idempotent — safe to call multiple times.
 *
 * CRITICAL: The :crypto process (CryptoService) must also call ensureInit()
 * in its own Service.onCreate() — each Android process needs its own JNI init.
 */
object SodiumInitializer {

    @Volatile
    private var initialized = false

    private lateinit var _sodium: LazySodiumAndroid

    val sodium: LazySodiumAndroid
        get() {
            check(initialized) {
                "SodiumInitializer.ensureInit() must be called before accessing sodium. " +
                "Call it in Application.onCreate()."
            }
            return _sodium
        }

    @Synchronized
    fun ensureInit() {
        if (initialized) return
        _sodium = LazySodiumAndroid(SodiumAndroid())
        initialized = true
    }

    fun isInitialized(): Boolean = initialized
}
