package com.stealthx.securechat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.shared.model.IfrTier
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
        fun ifrTierRepository(): IfrTierRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.stealthx.securechat.SET_TIER") return
        val tierName = intent.getStringExtra("tier") ?: return
        val tier = runCatching { IfrTier.valueOf(tierName) }.getOrNull() ?: return

        val repo = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TierRepositoryEntryPoint::class.java
        ).ifrTierRepository()

        CoroutineScope(Dispatchers.IO).launch {
            val lockedAmount = when (tier) {
                IfrTier.ELITE -> 8000L
                IfrTier.PRO -> 2000L
                IfrTier.FREE -> 0L
            }
            if (tier == IfrTier.FREE) {
                repo.invalidateCache()
            } else {
                repo.saveTierResult("0xDebugWallet", lockedAmount, tier)
            }
        }
    }
}
