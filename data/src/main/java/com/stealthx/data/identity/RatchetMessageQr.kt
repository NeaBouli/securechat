/*
 * SecureChat — Ratchet Message QR / URI Codec
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.identity

import android.net.Uri
import android.util.Base64
import com.stealthx.shared.model.EncryptedPayload
import com.stealthx.shared.model.RatchetMessage

object RatchetMessageQr {

    private const val FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    private const val SCHEME_PREFIX = "stealthx://msg"

    fun toQrContent(message: RatchetMessage): String {
        val dh = message.dhPublicKey.toBase64()
        val c = message.payload.ciphertext.toBase64()
        val n = message.payload.nonce.toBase64()
        val a = message.payload.aad.toBase64()
        return "$SCHEME_PREFIX?dh=$dh&ctr=${message.counter}&prev=${message.prevCounter}" +
            "&c=$c&n=$n&a=$a&pl=${message.payload.paddedLength}" +
            "&alg=${message.payload.algorithm}&v=${message.payload.version}"
    }

    fun fromQrContent(content: String): Result<RatchetMessage> {
        return try {
            if (!content.startsWith(SCHEME_PREFIX)) {
                return Result.failure(IllegalArgumentException("Not a StealthX message"))
            }
            val uri = Uri.parse(content)
            val dh = uri.required("dh").fromBase64()
            val ciphertext = uri.required("c").fromBase64()
            val nonce = uri.required("n").fromBase64()
            val aad = uri.required("a").fromBase64()
            val payload = EncryptedPayload(
                ciphertext = ciphertext,
                nonce = nonce,
                paddedLength = uri.required("pl").toInt(),
                aad = aad,
                algorithm = uri.required("alg"),
                version = uri.required("v").toInt()
            )
            Result.success(
                RatchetMessage(
                    dhPublicKey = dh,
                    counter = uri.required("ctr").toInt(),
                    prevCounter = uri.required("prev").toInt(),
                    payload = payload
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Uri.required(name: String): String =
        getQueryParameter(name) ?: throw IllegalArgumentException("Missing $name")

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, FLAGS)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, FLAGS)
}
