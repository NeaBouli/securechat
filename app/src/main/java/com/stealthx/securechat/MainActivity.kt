package com.stealthx.securechat

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.stealthx.data.NfcUriRelay
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.data.security.WipeManager
import com.stealthx.presentation.nav.StealthXNavGraph
import com.stealthx.presentation.theme.StealthXTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var wipeManager: WipeManager

    private val authState = mutableStateOf<AuthState>(AuthState.Locked)
    private var pinInput by mutableStateOf("")
    private var showPinEntry by mutableStateOf(false)
    private var pinError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setLockedContent()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        if (prefs.biometricEnabled) {
            authenticate()
        } else {
            authState.value = AuthState.Unlocked
        }
        handleNfcIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (prefs.biometricEnabled && authState.value == AuthState.Locked) {
            authenticate()
        }
    }

    private fun setLockedContent() {
        setContent {
            StealthXTheme {
                when (authState.value) {
                    AuthState.Locked -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "SecureChat",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Biometric required",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(24.dp))
                                TextButton(onClick = { showPinEntry = true }) {
                                    Text("Enter PIN")
                                }
                            }

                            if (showPinEntry) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showPinEntry = false
                                        pinInput = ""
                                        pinError = null
                                    },
                                    title = { Text("Enter PIN") },
                                    text = {
                                        Column {
                                            OutlinedTextField(
                                                value = pinInput,
                                                onValueChange = { pinInput = it; pinError = null },
                                                label = { Text("PIN") },
                                                visualTransformation = PasswordVisualTransformation(),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                                singleLine = true
                                            )
                                            pinError?.let {
                                                Spacer(Modifier.height(8.dp))
                                                Text(it, color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            if (checkDuressPin(pinInput)) {
                                                showPinEntry = false
                                                wipeAndShowDecoy()
                                            } else {
                                                pinError = "Incorrect PIN"
                                                pinInput = ""
                                            }
                                        }) { Text("Confirm") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            showPinEntry = false
                                            pinInput = ""
                                            pinError = null
                                        }) { Text("Cancel") }
                                    }
                                )
                            }
                        }
                    }
                    AuthState.Unlocked -> StealthXNavGraph()
                    AuthState.Unavailable -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Biometric unlock unavailable",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    private fun wipeAndShowDecoy() {
        CoroutineScope(Dispatchers.IO).launch {
            wipeManager.wipeAll()
            withContext(Dispatchers.Main) {
                finishAffinity()
                exitProcess(0)
            }
        }
    }

    private fun authenticate() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            // No biometric/device credential enrolled — unlock directly instead of closing
            authState.value = AuthState.Unlocked
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authState.value = AuthState.Unlocked
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (authState.value != AuthState.Unlocked) {
                        finish()
                    }
                }

                override fun onAuthenticationFailed() {
                    authState.value = AuthState.Locked
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock SecureChat")
                .setSubtitle("Confirm your device credential to continue")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }

    private fun handleNfcIntent(intent: android.content.Intent?) {
        when (intent?.action) {
            android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED,
            android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED -> {
                // Write mode: if MyIdScreen posted a bundle, write it to the tag
                val writeUri = com.stealthx.data.NfcWriteRelay.pendingUri
                if (writeUri != null) {
                    val tag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(android.nfc.NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(android.nfc.NfcAdapter.EXTRA_TAG)
                    }
                    if (tag != null) {
                        val ok = tryWriteNdefTag(tag, writeUri)
                        if (ok) com.stealthx.data.NfcWriteRelay.reportSuccess()
                        else com.stealthx.data.NfcWriteRelay.reportFailure(writeUri, "Tag write failed — tag may be read-only or too small")
                    } else {
                        com.stealthx.data.NfcWriteRelay.reportFailure(writeUri, "No writable NFC tag detected")
                    }
                    return
                }
                // Read mode: parse incoming NDEF and route to NewContactScreen
                val rawMsgs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayExtra(android.nfc.NfcAdapter.EXTRA_NDEF_MESSAGES, android.nfc.NdefMessage::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayExtra(android.nfc.NfcAdapter.EXTRA_NDEF_MESSAGES)
                }
                val msgs = rawMsgs?.mapNotNull { it as? android.nfc.NdefMessage }
                val uri = msgs?.firstOrNull()?.records?.firstOrNull()
                    ?.let { record ->
                        if (record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                            record.type.contentEquals(android.nfc.NdefRecord.RTD_URI)) {
                            android.nfc.NdefRecord.createUri(record.toUri().toString()).toUri().toString()
                        } else if (record.tnf == android.nfc.NdefRecord.TNF_ABSOLUTE_URI) {
                            String(record.payload, Charsets.UTF_8)
                        } else null
                    }
                if (uri?.startsWith("stealthx://add/") == true) {
                    NfcUriRelay.post(uri)
                }
            }
        }
    }

    private fun tryWriteNdefTag(tag: android.nfc.Tag, uri: String): Boolean {
        val record = android.nfc.NdefRecord.createUri(uri)
        val msg = android.nfc.NdefMessage(arrayOf(record))
        val ndef = android.nfc.tech.Ndef.get(tag)
        if (ndef != null) {
            return runCatching {
                ndef.connect()
                try {
                    if (!ndef.isWritable) return@runCatching false
                    if (ndef.maxSize < msg.toByteArray().size) return@runCatching false
                    ndef.writeNdefMessage(msg)
                    true
                } finally {
                    runCatching { ndef.close() }
                }
            }.getOrDefault(false)
        }
        val formatable = android.nfc.tech.NdefFormatable.get(tag) ?: return false
        return runCatching {
            formatable.connect()
            try {
                formatable.format(msg)
                true
            } finally {
                runCatching { formatable.close() }
            }
        }.getOrDefault(false)
    }

    fun checkDuressPin(input: String): Boolean {
        return prefs.duressPin?.let { it == input } ?: false
    }

    private enum class AuthState {
        Locked,
        Unlocked,
        Unavailable
    }
}
