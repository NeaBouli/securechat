package com.stealthx.securechat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stealthx.domain.repository.AccessTierRepository
import com.stealthx.shared.model.AccessTier
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * DEBUG ONLY — never shipped in release builds (debug source set).
 *
 * ADB usage:
 *   adb shell am broadcast -a com.stealthx.securechat.SET_TIER \
 *     --es tier ELITE -n com.stealthx.securechat/.SetTierReceiver
 *
 * Valid tier values: FREE, PRO, ELITE
 */
@AndroidEntryPoint
class SetTierReceiver : BroadcastReceiver() {

    @EntryPoint
    @dagger.hilt.InstallIn(SingletonComponent::class)
    interface TierRepositoryEntryPoint {
        fun accessTierRepository(): AccessTierRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.stealthx.securechat.SET_TIER") return
        val tierName = intent.getStringExtra("tier") ?: return
        val tier = runCatching { AccessTier.valueOf(tierName) }.getOrNull() ?: return

        val repo = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TierRepositoryEntryPoint::class.java
        ).accessTierRepository()

        CoroutineScope(Dispatchers.IO).launch {
            val accessWeight = tier.rank
            if (tier == AccessTier.FREE) {
                repo.invalidateCache()
            } else {
                repo.saveTierResult("debug_override", accessWeight, tier)
            }
        }
    }
}
