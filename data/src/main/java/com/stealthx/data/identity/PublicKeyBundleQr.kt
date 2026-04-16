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

import android.net.Uri
import android.util.Base64
import com.stealthx.shared.model.PublicKeyBundle

object PublicKeyBundleQr {

    private const val FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    private const val SCHEME_PREFIX = "stealthx://add/"

    fun toQrContent(bundle: PublicKeyBundle): String {
        val x = Base64.encodeToString(bundle.x25519PublicKey, FLAGS)
        val e = Base64.encodeToString(bundle.ed25519PublicKey, FLAGS)
        val s = Base64.encodeToString(bundle.signature, FLAGS)
        val handle = bundle.customHandle?.let { "&h=$it" } ?: ""
        return "$SCHEME_PREFIX${bundle.sxId}?x=$x&e=$e&s=$s$handle"
    }

    fun fromQrContent(content: String): Result<PublicKeyBundle> {
        return try {
            if (!content.startsWith(SCHEME_PREFIX)) {
                return Result.failure(IllegalArgumentException("Not a StealthX link"))
            }
            val uri = Uri.parse(content)
            val sxId = uri.pathSegments.lastOrNull()
                ?: return Result.failure(IllegalArgumentException("Missing sxId"))
            val xB64 = uri.getQueryParameter("x")
                ?: return Result.failure(IllegalArgumentException("Missing x25519"))
            val eB64 = uri.getQueryParameter("e")
                ?: return Result.failure(IllegalArgumentException("Missing ed25519"))
            val sB64 = uri.getQueryParameter("s")
                ?: return Result.failure(IllegalArgumentException("Missing signature"))
            Result.success(
                PublicKeyBundle(
                    sxId = sxId,
                    customHandle = uri.getQueryParameter("h"),
                    x25519PublicKey = Base64.decode(xB64, FLAGS),
                    ed25519PublicKey = Base64.decode(eB64, FLAGS),
                    signature = Base64.decode(sB64, FLAGS),
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
