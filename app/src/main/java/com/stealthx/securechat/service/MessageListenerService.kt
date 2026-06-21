package com.stealthx.securechat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.prefs.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MessageListenerService : Service() {

    @Inject lateinit var contactExchangeManager: ContactExchangeManager
    @Inject lateinit var appPreferences: AppPreferences

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        if (!appPreferences.backgroundListenerEnabled) {
            stopListeningService()
            return
        }
        contactExchangeManager.startListening()
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        serviceScope.launch {
            while (true) {
                delay(30_000)
                if (!appPreferences.backgroundListenerEnabled) {
                    stopListeningService()
                    return@launch
                }
                if (!contactExchangeManager.isConnected) {
                    contactExchangeManager.startListening()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (appPreferences.backgroundListenerEnabled) {
            START_STICKY
        } else {
            stopListeningService()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        contactExchangeManager.stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildForegroundNotification(): Notification {
        val channelId = "securechat_listener"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Message Listener",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps encrypted message delivery active"
                setShowBadge(false)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("SecureChat")
            .setContentText("End-to-end encrypted • messages protected")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun stopListeningService() {
        contactExchangeManager.stopListening()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    companion object {
        private const val NOTIFICATION_ID = 7331
    }
}
