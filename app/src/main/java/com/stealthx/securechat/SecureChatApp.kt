package com.stealthx.securechat

import android.app.Application
import android.content.Intent
import com.stealthx.crypto.SodiumInitializer
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.securechat.service.MessageListenerService
import com.stealthx.shared.model.AccessTier
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SecureChatApp : Application() {
    @Inject lateinit var appPreferences: AppPreferences

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

        val allowDevTierOverride = BuildConfig.DEBUG
        com.stealthx.shared.DevTierOverride.forcedTier =
            BuildConfig.FORCED_TIER
                .takeIf { allowDevTierOverride && it.isNotBlank() }
                ?.let { AccessTier.valueOf(it) }
        com.stealthx.shared.DevTierOverride.forceElite = allowDevTierOverride && BuildConfig.FORCE_ELITE

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        if (appPreferences.backgroundListenerEnabled) {
            startForegroundService(Intent(this, MessageListenerService::class.java))
        }
    }
}
