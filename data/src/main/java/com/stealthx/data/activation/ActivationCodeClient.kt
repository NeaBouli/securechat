/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.activation

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ActivationCodeClient {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private const val SIGNAL_URL = "wss://api.stealthx.tech/signal"

    fun activate(code: String, onResult: (tier: String?, error: String?) -> Unit) {
        val request = Request.Builder().url(SIGNAL_URL).build()
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                ws.send(JSONObject().apply {
                    put("type", "ACTIVATE_CODE")
                    put("code", code.trim())
                }.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    if (json.optString("type") == "ACTIVATE_CODE_RESULT") {
                        ws.close(1000, null)
                        if (json.optBoolean("success", false)) {
                            onResult(json.optString("tier").takeIf { it.isNotEmpty() }, null)
                        } else {
                            onResult(null, json.optString("error", "invalid_code"))
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
