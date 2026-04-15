/*
 * Chameleon / SecureChat — App Signature Verifier
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Computes SHA-256 of the app's signing certificate at runtime.
 * Sent with every server connection for fork protection.
 */
package com.stealthx.ifr

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object AppSignature {

    fun getSha256(context: Context): String {
        return try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            val sig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners?.get(0)
                    ?: return "unknown"
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.get(0)
                    ?: return "unknown"
            }
            val hash = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "unknown"
        }
    }
}
