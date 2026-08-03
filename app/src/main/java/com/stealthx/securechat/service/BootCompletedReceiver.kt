package com.stealthx.securechat.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stealthx.data.prefs.AppPreferences
import timber.log.Timber

/**
 * Restarts [MessageListenerService] after device boot or after the app is
 * updated, but only when the user enabled the Background Message Listener.
 *
 * Every other action is ignored. The encrypted [AppPreferences] are read
 * defensively: any failure (keystore, decryption) fails closed — the listener
 * is not started and the process does not crash.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (!ListenerRecoveryDecision.isRecoveryAction(action)) return

        val enabled = runCatching {
            AppPreferences(context.applicationContext).backgroundListenerEnabled
        }.onFailure {
            Timber.e(it, "BootCompletedReceiver: preferences unreadable — listener not started")
        }.getOrDefault(false)
        if (ListenerRecoveryDecision.shouldStart(action, enabled)) {
            ListenerStartup.startSafely(context)
        }
    }
}
