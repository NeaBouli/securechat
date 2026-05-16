package com.stealthx.securechat

import android.app.Application
import com.stealthx.crypto.SodiumInitializer
import com.stealthx.data.identity.StealthXIdentity
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SecureChatApp : Application() {
    override fun onCreate() {
        super.onCreate()

        SodiumInitializer.ensureInit()

        // Create per-device identity on first launch — idempotent on subsequent launches
        StealthXIdentity.getOrCreateWithSeed(this)

        if (BuildConfig.DEBUG && BuildConfig.FORCE_ELITE) {
            com.stealthx.shared.DevTierOverride.forceElite = true
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
