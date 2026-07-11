package com.stealthx.securechat

import android.app.Application
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.stealthx.crypto.SodiumInitializer
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.securechat.service.MessageListenerService
import com.stealthx.shared.model.AccessTier
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject
import java.util.concurrent.TimeUnit

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
        scheduleEntitlementRefresh()
    }

    private fun scheduleEntitlementRefresh() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniqueWork(
            "securechat-entitlement-refresh-now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<EntitlementRefreshWorker>().setConstraints(constraints).build()
        )
        workManager.enqueueUniquePeriodicWork(
            "securechat-entitlement-refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<EntitlementRefreshWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
        )
    }
}
