/*
 * SecureChat — Bidirectional Contact Exchange via WebSocket
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * When A scans B's QR and saves B as contact, A sends its own
 * bundle to api.stealthx.tech/signal with type=CONTACT_EXCHANGE.
 * B's persistent listener receives it and auto-saves A.
 */
package com.stealthx.data.exchange

import android.content.Context
import com.stealthx.data.identity.PublicKeyBundleQr
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.data.repository.ContactRepository
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactExchangeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository
) {
    private companion object {
        const val SIGNAL_URL = "wss://api.stealthx.tech/signal"
    }

    private val certPinner = CertificatePinner.Builder()
        .add("api.stealthx.tech", "sha256/1e85xNSEj+dcImOJS0iNkfMZOrZdvJJzzPCqT1/CZDc=")
        .add("api.stealthx.tech", "sha256/kZwN96eHtZftBWrOZUsd6cA4es80n3NzSk/XtYz2EqQ=")
        .build()

    private val sendClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .certificatePinner(certPinner)
        .build()

    private val listenClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // no timeout — persistent connection
        .pingInterval(30, TimeUnit.SECONDS) // keep-alive
        .certificatePinner(certPinner)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile private var listenerWs: WebSocket? = null

    /**
     * After A saves B's contact, A sends its own QR bundle to B via server relay.
     * Fire-and-forget — server routes CONTACT_EXCHANGE to B's listening session.
     */
    fun sendExchange(toSxId: String) {
        scope.launch {
            try {
                val myBundle = PublicKeyBundleQr.toQrContent(
                    StealthXIdentity.createPublicKeyBundle(context)
                )
                val req = Request.Builder().url(SIGNAL_URL).build()
                sendClient.newWebSocket(req, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        ws.send(JSONObject().apply {
                            put("type", "CONTACT_EXCHANGE")
                            put("to", toSxId)
                            put("bundle", myBundle)
                        }.toString())
                        ws.close(1000, null)
                    }
                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) { /* silent */ }
                })
            } catch (_: Exception) {}
        }
    }

    /**
     * Open a persistent WebSocket, identify as self, and listen for incoming
     * CONTACT_EXCHANGE messages. Idempotent — only one listener at a time.
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
                    if (json.optString("type") == "CONTACT_EXCHANGE") {
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
                } catch (_: Exception) {}
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                listenerWs = null
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                listenerWs = null
            }
        })
    }
}
