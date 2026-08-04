package com.stealthx.securechat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
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
        // FOREGROUND_SERVICE_TYPE_MANIFEST keeps the manifest-declared
        // foregroundServiceType (remoteMessaging) unchanged.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildForegroundNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        )
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Background messages",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while background message listening is active"
                setShowBadge(false)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("SecureChat")
            .setContentText("Background message listening is active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
        private const val NOTIFICATION_CHANNEL_ID = "securechat_background_messages_v2"
    }
}
