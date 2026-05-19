/*
 * SecureChat — Public Key Bundle QR / URI Codec
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Encodes / decodes a [PublicKeyBundle] to / from a
 * "stealthx://add/<sxId>?x=…&e=…&s=…" URI. URL-safe Base64
 * (no wrap, no padding) is used for all binary fields so the
 * result can be embedded in a QR code without escaping.
 */
package com.stealthx.data.identity

import com.stealthx.shared.SxIdValidator
import com.stealthx.shared.model.PublicKeyBundle
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

object PublicKeyBundleQr {

    private const val SCHEME_PREFIX = "stealthx://add/"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun toQrContent(bundle: PublicKeyBundle): String {
        val x = encoder.encodeToString(bundle.x25519PublicKey)
        val e = encoder.encodeToString(bundle.ed25519PublicKey)
        val s = encoder.encodeToString(bundle.signature)
        val handle = bundle.customHandle?.let { "&h=${encodeQueryValue(it)}" } ?: ""
        return "$SCHEME_PREFIX${bundle.sxId}?x=$x&e=$e&s=$s&c=${bundle.createdAt}$handle"
    }

    fun fromQrContent(content: String): Result<PublicKeyBundle> {
        return try {
            if (!content.startsWith(SCHEME_PREFIX)) {
                return Result.failure(IllegalArgumentException("Not a StealthX link"))
            }
            val uri = URI(content)
            val sxId = uri.path.removePrefix("/").takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalArgumentException("Missing sxId"))
            if (!SxIdValidator.isValid(sxId)) {
                return Result.failure(IllegalArgumentException("Invalid sx_ ID format: '$sxId'"))
            }
            val params = parseQuery(uri.rawQuery.orEmpty())
            val xB64 = params["x"]
                ?: return Result.failure(IllegalArgumentException("Missing x25519"))
            val eB64 = params["e"]
                ?: return Result.failure(IllegalArgumentException("Missing ed25519"))
            val sB64 = params["s"]
                ?: return Result.failure(IllegalArgumentException("Missing signature"))
            val createdAt = params["c"]?.toLongOrNull()
                ?: return Result.failure(IllegalArgumentException("Missing createdAt"))
            Result.success(
                PublicKeyBundle(
                    sxId = sxId,
                    customHandle = params["h"],
                    x25519PublicKey = decoder.decode(xB64),
                    ed25519PublicKey = decoder.decode(eB64),
                    signature = decoder.decode(sB64),
                    createdAt = createdAt
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&")
            .mapNotNull { part ->
                val separator = part.indexOf("=")
                if (separator <= 0) {
                    null
                } else {
                    val key = decodeQueryValue(part.substring(0, separator))
                    val value = decodeQueryValue(part.substring(separator + 1))
                    key to value
                }
            }
            .toMap()
    }

    private fun encodeQueryValue(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun decodeQueryValue(value: String): String =
        URLDecoder.decode(value, Charsets.UTF_8.name())
}
