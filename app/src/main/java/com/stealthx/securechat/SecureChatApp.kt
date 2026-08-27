package com.stealthx.securechat

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.stealthx.crypto.SodiumInitializer
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.shared.model.AccessTier
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit

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

        val allowDevTierOverride = BuildConfig.ALLOW_TIER_OVERRIDE
        com.stealthx.shared.DevTierOverride.forcedTier =
            BuildConfig.FORCED_TIER
                .takeIf { allowDevTierOverride && it.isNotBlank() }
                ?.let { AccessTier.valueOf(it) }
        com.stealthx.shared.DevTierOverride.forceElite = allowDevTierOverride && BuildConfig.FORCE_ELITE

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // The message listener FGS is intentionally NOT started here: Application.onCreate
        // also runs when the process is created in the background (e.g. broadcast receivers,
        // WorkManager), where startForegroundService throws
        // ForegroundServiceStartNotAllowedException on targetSdk 31+. MainActivity starts the
        // listener from the foreground instead when backgroundListenerEnabled is set.
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
