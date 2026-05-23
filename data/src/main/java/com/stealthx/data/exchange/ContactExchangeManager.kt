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
import kotlinx.coroutines.launch
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactExchangeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val messageRepository: Lazy<MessageRepository>
) {
    private companion object {
        const val SIGNAL_URL = "wss://api.stealthx.tech/signal"
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
    private val pendingFrames = ConcurrentLinkedQueue<String>()

    val isConnected: Boolean get() = listenerWs != null
    val isIdentified: Boolean get() = identified

    private fun sendOrQueue(frame: String) {
        if (identified) listenerWs?.send(frame) else pendingFrames.add(frame)
    }

    private fun drainPending(ws: WebSocket) {
        var frame = pendingFrames.poll()
        while (frame != null) { ws.send(frame); frame = pendingFrames.poll() }
    }

    /** Send a raw JSON string — queued until IDENTIFY_ACK if not yet identified. */
    fun sendRaw(json: String): Boolean {
        sendOrQueue(json)
        return true
    }

    fun sendReadReceipt(toSxId: String) {
        scope.launch {
            runCatching {
                sendOrQueue(JSONObject().apply {
                    put("type", "READ_RECEIPT")
                    put("to", toSxId)
                }.toString())
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
                sendOrQueue(JSONObject().apply {
                    put("type", "CONTACT_EXCHANGE")
                    put("to", toSxId)
                    put("bundle", myBundle)
                }.toString())
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
                listenerWs = null; identified = false
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                listenerWs = null; identified = false
            }
        })
    }

    private fun handleContactExchange(json: JSONObject) {
        val bundle = json.optString("bundle").ifEmpty { return }
        scope.launch {
            runCatching {
                val parsed = PublicKeyBundleQr.fromQrContent(bundle).getOrThrow()
                if (contactRepository.getById(parsed.sxId) == null) {
                    contactRepository.addContactBundle(parsed)
                }
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
}
