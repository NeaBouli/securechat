/*
 * SecureChat — Clipboard Cleaner
 * Auto-clears clipboard 60 seconds after sensitive data is copied.
 */
package com.stealthx.presentation.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper

object ClipboardCleaner {
    private const val AUTO_CLEAR_MS = 60_000L
    private val handler = Handler(Looper.getMainLooper())

    fun copyWithAutoClear(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        handler.postDelayed({
            val current = cm.primaryClip?.getItemAt(0)?.text?.toString()
            if (current == text) {
                cm.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }, AUTO_CLEAR_MS)
    }
}
