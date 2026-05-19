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

        try {
            SodiumInitializer.ensureInit()
        } catch (e: Exception) {
            Timber.e(e, "SodiumInitializer failed — crypto unavailable")
        }

        try {
            StealthXIdentity.getOrCreateWithSeed(this)
        } catch (e: Exception) {
            Timber.e(e, "Identity init failed — will retry on next launch")
        }

        if (BuildConfig.FORCE_ELITE) {
            com.stealthx.shared.DevTierOverride.forceElite = true
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
