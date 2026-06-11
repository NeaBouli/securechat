/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.activation

import android.content.Context
import com.stealthx.data.identity.StealthXIdentity
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ActivationCodeClient {

    // Leaf pin: api.stealthx.tech (Let's Encrypt, expires 2026-08-14 — rotate before then)
    // Backup pin: Let's Encrypt R12 intermediate CA (stable across leaf rotations)
    private val certPinner = CertificatePinner.Builder()
        .add("api.stealthx.tech", "sha256/1e85xNSEj+dcImOJS0iNkfMZOrZdvJJzzPCqT1/CZDc=")
        .add("api.stealthx.tech", "sha256/kZwN96eHtZftBWrOZUsd6cA4es80n3NzSk/XtYz2EqQ=")
        .build()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .certificatePinner(certPinner)
            .build()
    }

    private const val SIGNAL_URL = "wss://api.stealthx.tech/signal"

    fun activate(context: Context, code: String, onResult: (tier: String?, error: String?) -> Unit) {
        val request = Request.Builder().url(SIGNAL_URL).build()
        client.newWebSocket(request, object : WebSocketListener() {
            private var activationSent = false

            override fun onOpen(ws: WebSocket, response: Response) {
                val clientId = runCatching { StealthXIdentity.getOrCreateWithSeed(context).raw }.getOrNull()
                if (clientId.isNullOrBlank()) {
                    ws.close(1008, "identity_missing")
                    onResult(null, "identity_missing")
                    return
                }
                ws.send(JSONObject().apply {
                    put("type", "REGISTER")
                    put("clientId", clientId)
                }.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "REGISTERED" -> if (!activationSent) {
                            activationSent = true
                            ws.send(JSONObject().apply {
                                put("type", "ACTIVATE_CODE")
                                put("code", code.trim())
                            }.toString())
                        }
                        "ACTIVATE_CODE_RESULT" -> {
                            ws.close(1000, null)
                            if (json.optBoolean("success", false)) {
                                onResult(json.optString("tier").takeIf { it.isNotEmpty() }, null)
                            } else {
                                onResult(null, json.optString("error", "invalid_code"))
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                onResult(null, "network_error")
            }
        })
    }
}
