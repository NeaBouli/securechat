package com.stealthx.securechat.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Fail-closed recovery decision for the background message listener.
 *
 * The listener is restarted only after device boot or after the app itself was
 * updated, and only when the user explicitly enabled it. Unknown or missing
 * actions never start it.
 */
object ListenerRecoveryDecision {

    private val RECOVERY_ACTIONS = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED
    )

    fun isRecoveryAction(action: String?): Boolean = action in RECOVERY_ACTIONS

    fun shouldStart(action: String?, enabled: Boolean): Boolean =
        enabled && isRecoveryAction(action)
}

/**
 * Centralized, crash-safe foreground-service start for [MessageListenerService].
 * Never throws — a failed start is logged and swallowed so callers (boot
 * receiver, cold-start activity) fail closed instead of crashing.
 */
object ListenerStartup {

    fun startSafely(context: Context) {
        runCatching {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, MessageListenerService::class.java)
            )
        }.onFailure {
            Timber.e(it, "MessageListenerService start failed — listener remains stopped")
        }
    }
}
