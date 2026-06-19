package com.stealthx.presentation.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewContactScreen(
    onBack: () -> Unit,
    onContactAdded: () -> Unit,
    onUpgrade: () -> Unit = {},
    initialContent: String = "",
    vm: NewContactViewModel = hiltViewModel()
) {
    var qrContent by remember { mutableStateOf(initialContent) }
    val limitState by vm.limitState.collectAsState()
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            qrContent = content
            // Auto-trigger: scan intent == add intent, skip manual button press
            if (content.startsWith("stealthx://add/") && !limitState.isAtLimit) {
                vm.addFromQrContent(content)
            }
        }
    }

    LaunchedEffect(state.contactAdded) {
        if (state.contactAdded) {
            onContactAdded()
            vm.consumeContactAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (limitState.isAtLimit) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null,
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Contact limit reached (${limitState.count}/${limitState.limit})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Buy Pro Lifetime for unlimited contacts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        TextButton(onClick = onUpgrade) {
                            Icon(Icons.Default.Lock, null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Buy Pro", color = Color(0xFF00E5FF),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else if (limitState.isLimitEnforced) {
                Text(
                    "${limitState.count}/${limitState.limit} contacts used (Free tier)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            OptionCard(
                icon = Icons.Default.QrCodeScanner,
                title = "Scan QR Code",
                subtitle = "Fastest way to add a contact",
                enabled = !limitState.isAtLimit,
                onClick = {
                    scanLauncher.launch(
                        ScanOptions()
                            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            .setPrompt("Scan StealthX contact QR")
                            .setBeepEnabled(false)
                    )
                }
            )
            val nfcAdapter = remember { android.nfc.NfcAdapter.getDefaultAdapter(context) }
            var nfcListening by remember { mutableStateOf(false) }

            OptionCard(
                icon = Icons.Default.Nfc,
                title = "NFC Tap",
                subtitle = if (nfcAdapter != null) "Tap devices together to exchange" else "NFC not available on this device",
                enabled = nfcAdapter != null && !limitState.isAtLimit,
                onClick = {
                    if (nfcAdapter != null) {
                        nfcListening = !nfcListening
                    }
                }
            )

            DisposableEffect(nfcListening) {
                if (nfcListening) {
                    val activity = context as? android.app.Activity ?: return@DisposableEffect onDispose {}
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 0,
                        android.content.Intent(context, activity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        android.app.PendingIntent.FLAG_MUTABLE
                    )
                    val filters = arrayOf(android.content.IntentFilter(android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED))
                    nfcAdapter!!.enableForegroundDispatch(activity, pendingIntent, filters, null)
                    onDispose { runCatching { nfcAdapter.disableForegroundDispatch(activity) } }
                } else {
                    onDispose {}
                }
            }

            if (nfcListening) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "NFC active — hold devices together",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            OptionCard(
                icon = Icons.Default.Edit,
                title = "Paste QR content",
                subtitle = "stealthx://add/... signed bundle",
                enabled = !limitState.isAtLimit,
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val pasted = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                    if (pasted.isNotEmpty()) qrContent = pasted
                }
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = qrContent,
                onValueChange = { qrContent = it },
                label = { Text("Contact QR content") },
                placeholder = { Text("stealthx://add/sx_...?...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !limitState.isAtLimit
            )
            state.statusMessage?.let {
                Text(it, color = Color(0xFF00C853), style = MaterialTheme.typography.bodySmall)
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { vm.addFromQrContent(qrContent) },
                enabled = !limitState.isAtLimit && !state.isSaving && qrContent.startsWith("stealthx://add/"),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Adding..." else "Add Contact")
            }
        }
    }
}

@Composable
private fun OptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}
