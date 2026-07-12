/*
 * SecureChat — Bidirectional Contact Exchange + Message Relay via WebSocket
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * When A scans B's QR and saves B as contact, A sends its own
 * bundle to api.stealthx.tech/signal with type=CONTACT_EXCHANGE.
 * B's persistent listener receives it and auto-saves A.
 *
 * The same persistent connection is used for real-time message delivery
 * via type=MESSAGE / type=MESSAGE (incoming).
 */
package com.stealthx.data.exchange

import android.content.Context
import com.stealthx.data.identity.PublicKeyBundleQr
import com.stealthx.data.identity.RatchetMessageQr
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.data.repository.ContactRepository
import com.stealthx.data.repository.MessageRepository
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

import javax.inject.Inject
import javax.inject.Singleton

data class ContactExchangeEvent(
    val sxId: String,
    val displayName: String
)

internal class PendingFrameBuffer(
    private val maxSize: Int
) {
    private val frames = ArrayDeque<String>()

    @Synchronized
    fun offer(frame: String): Boolean {
        if (frames.size >= maxSize) return false
        frames.addLast(frame)
        return true
    }

    @Synchronized
    fun drain(send: (String) -> Boolean): Int {
        var sent = 0
        while (frames.isNotEmpty()) {
            val frame = frames.removeFirst()
            if (!send(frame)) {
                frames.addFirst(frame)
                return sent
            }
            sent++
        }
        return sent
    }

    @Synchronized
    fun clear() = frames.clear()

    @Synchronized
    internal fun snapshot(): List<String> = frames.toList()
}

@Singleton
class ContactExchangeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val messageRepository: Lazy<MessageRepository>
) {
    private companion object {
        const val SIGNAL_URL = "wss://api.stealthx.tech/signal"
        const val MAX_PENDING_FRAMES = 256
    }

    private val certPinner = CertificatePinner.Builder()
        .add("api.stealthx.tech", "sha256/1e85xNSEj+dcImOJS0iNkfMZOrZdvJJzzPCqT1/CZDc=")
        .add("api.stealthx.tech", "sha256/kZwN96eHtZftBWrOZUsd6cA4es80n3NzSk/XtYz2EqQ=")
        .build()

    private val listenClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .certificatePinner(certPinner)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile private var listenerWs: WebSocket? = null
    @Volatile private var identified = false
    private val pendingFrames = PendingFrameBuffer(MAX_PENDING_FRAMES)
    private val fireAndForgetDrops = AtomicInteger(0)
    private val _contactExchangeEvents = MutableSharedFlow<ContactExchangeEvent>(
        replay = 0,
        extraBufferCapacity = 8
    )

    val isConnected: Boolean get() = listenerWs != null
    val isIdentified: Boolean get() = identified
    val contactExchangeEvents: SharedFlow<ContactExchangeEvent> = _contactExchangeEvents.asSharedFlow()

    @Synchronized
    private fun sendOrQueue(frame: String): Boolean {
        val ws = listenerWs
        if (identified && ws != null && ws.send(frame)) {
            return true
        }

        if (!pendingFrames.offer(frame)) return false
        if (ws == null) startListening()
        return true
    }

    @Synchronized
    private fun drainPending(ws: WebSocket) {
        pendingFrames.drain(ws::send)
    }

    /** Send a raw JSON string — queued until IDENTIFY_ACK if not yet identified. */
    fun sendRaw(json: String): Boolean {
        return sendOrQueue(json)
    }

    fun sendReadReceipt(toSxId: String) {
        scope.launch {
            runCatching {
                val accepted = sendOrQueue(JSONObject().apply {
                    put("type", "READ_RECEIPT")
                    put("to", toSxId)
                }.toString())
                if (!accepted) fireAndForgetDrops.incrementAndGet()
            }
        }
    }

    /**
     * After A saves B's contact, A sends its own QR bundle to B via server relay.
     * Fire-and-forget — server routes CONTACT_EXCHANGE to B's listening session.
     */
    fun sendExchange(toSxId: String) {
        scope.launch {
            runCatching {
                val myBundle = PublicKeyBundleQr.toQrContent(
                    StealthXIdentity.createPublicKeyBundle(context)
                )
                val accepted = sendOrQueue(JSONObject().apply {
                    put("type", "CONTACT_EXCHANGE")
                    put("to", toSxId)
                    put("bundle", myBundle)
                }.toString())
                if (!accepted) fireAndForgetDrops.incrementAndGet()
            }
        }
    }

    /**
     * Open a persistent WebSocket, identify as self, and listen for incoming
     * CONTACT_EXCHANGE and MESSAGE frames. Idempotent — only one listener at a time.
     */
    fun startListening() {
        if (listenerWs != null) return
        val mySxId = runCatching { StealthXIdentity.get(context)?.raw }.getOrNull() ?: return
        val req = Request.Builder().url(SIGNAL_URL).build()
        listenerWs = listenClient.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                ws.send(JSONObject().apply {
                    put("type", "IDENTIFY")
                    put("sxId", mySxId)
                }.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "IDENTIFY_ACK" -> { identified = true; drainPending(ws) }
                        "CONTACT_EXCHANGE" -> handleContactExchange(json)
                        "MESSAGE" -> handleIncomingMessage(json)
                        "MESSAGE_ACK" -> handleMessageAck(json)
                        "READ_RECEIPT" -> handleReadReceipt(json)
                    }
                } catch (_: Exception) {}
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                if (listenerWs === ws) {
                    listenerWs = null
                    identified = false
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                if (listenerWs === ws) {
                    listenerWs = null
                    identified = false
                }
            }
        })
    }

    @Synchronized
    fun stopListening() {
        listenerWs?.close(1000, "listener disabled")
        listenerWs = null
        identified = false
        clearPending()
    }

    @Synchronized
    private fun clearPending() = pendingFrames.clear()

    internal val fireAndForgetDropCount: Int
        get() = fireAndForgetDrops.get()

    private fun handleContactExchange(json: JSONObject) {
        val bundle = json.optString("bundle").ifEmpty { return }
        scope.launch {
            runCatching {
                val parsed = PublicKeyBundleQr.fromQrContent(bundle).getOrThrow()
                if (contactRepository.getById(parsed.sxId) == null) {
                    contactRepository.addContactBundle(parsed)
                }
                val displayName = parsed.customHandle ?: parsed.sxId
                val event = ContactExchangeEvent(parsed.sxId, displayName)
                _contactExchangeEvents.tryEmit(event)
                showContactExchangeNotification(event)
            }
        }
    }

    private fun handleIncomingMessage(json: JSONObject) {
        val fromSxId = json.optString("from").ifEmpty { return }
        val payload = json.optString("payload").ifEmpty { return }
        scope.launch {
            runCatching {
                val ratchetMessage = RatchetMessageQr.fromQrContent(payload).getOrThrow()
                messageRepository.get().receiveLocalMessage(fromSxId, ratchetMessage)
                val displayName = contactRepository.getById(fromSxId)?.displayName
                showMessageNotification(fromSxId, displayName)
            }
        }
    }

    private fun handleMessageAck(json: JSONObject) {
        val to = json.optString("to").ifEmpty { return }
        val delivered = json.optBoolean("delivered", false)
        if (!delivered) return
        scope.launch { runCatching { messageRepository.get().markOutgoingDelivered(to) } }
    }

    private fun handleReadReceipt(json: JSONObject) {
        val fromSxId = json.optString("from").ifEmpty { return }
        scope.launch {
            runCatching {
                messageRepository.get().markOutgoingMessagesRead(fromSxId)
            }
        }
    }

    private fun showMessageNotification(fromSxId: String, displayName: String?) {
        val channelId = "securechat_messages"
        val nm = context.getSystemService(android.app.NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Encrypted Messages",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New message notifications"
                setShowBadge(true)
            }
            nm?.createNotificationChannel(channel)
        }

        val tapIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, fromSxId.hashCode(), tapIntent ?: android.content.Intent(),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val senderLabel = displayName ?: fromSxId.take(12)
        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(senderLabel)
            .setContentText("New encrypted message")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_SECRET)
            .build()
        nm?.notify(fromSxId.hashCode(), notification)
    }

    private fun showContactExchangeNotification(event: ContactExchangeEvent) {
        val channelId = "securechat_contacts"
        val nm = context.getSystemService(android.app.NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Contact Exchange",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Contact exchange confirmations"
                setShowBadge(true)
            }
            nm?.createNotificationChannel(channel)
        }

        val tapIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, event.sxId.hashCode(), tapIntent ?: android.content.Intent(),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Contact added you")
            .setContentText("${event.displayName} added you to SecureChat")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_SECRET)
            .build()
        nm?.notify(("contact:${event.sxId}").hashCode(), notification)
    }
}
