package com.stealthx.securechat.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.stealthx.data.repository.LegacyConsentGate
import com.stealthx.presentation.screens.LegacyConsentDialog
import com.stealthx.presentation.theme.StealthXTheme

/**
 * Debug-only visual QA harness (SCT-02): renders the real legacy-consent dialog with a synthetic
 * candidate so its layout can be checked at every font scale. It has no import path: the buttons
 * do nothing and no contact is ever written. Not part of release builds.
 */
class LegacyConsentPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val syntheticKey = ByteArray(32) { (it * 31 + 7).toByte() }
        setContent {
            StealthXTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) {
                        LegacyConsentDialog(
                            sxId = "sx_LegacyAbc",
                            keyFingerprint = LegacyConsentGate.fingerprint(syntheticKey),
                            onConfirm = {},
                            onCancel = {}
                        )
                    }
                }
            }
        }
    }
}
