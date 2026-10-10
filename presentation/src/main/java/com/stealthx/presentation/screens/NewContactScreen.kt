package com.stealthx.presentation.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.stealthx.data.nfc.NfcContactUri
import com.stealthx.data.nfc.NfcReaderSession
import com.stealthx.data.repository.ImportProvenance
import com.stealthx.presentation.R

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
    // In-person only for a camera scan or an NFC tag read by this screen's own reader callback;
    // prefilled links (deep link / external NFC intent), pasting or editing are remote (SCT-02).
    var inPerson by remember { mutableStateOf(false) }
    val limitState by vm.limitState.collectAsState()
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            qrContent = content
            inPerson = true
            // Auto-trigger: scan intent == add intent, skip manual button press
            if (content.startsWith("stealthx://add/") && !limitState.isAtLimit) {
                vm.addFromQrContent(content, ImportProvenance.IN_PERSON)
            }
        }
    }

    state.legacyCandidate?.let { candidate ->
        LegacyConsentDialog(
            sxId = candidate.sxId,
            keyFingerprint = candidate.keyFingerprint,
            onConfirm = { vm.confirmLegacyImport(candidate.generation) },
            onCancel = { vm.cancelLegacyImport(candidate.generation) }
        )
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
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
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
                                "Pro access enables unlimited contacts. Check VLABS availability.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        TextButton(onClick = onUpgrade) {
                            Icon(Icons.Default.Lock, null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Check Pro", color = Color(0xFF00E5FF),
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

            // SCT-02: in-person NFC provenance is minted only inside this platform ReaderCallback while
            // the screen is resumed and the session generation is current. External NFC/VIEW intents
            // handled by MainActivity stay REMOTE. Reader mode is activity-scoped; no global NFC change.
            val lifecycleOwner = LocalLifecycleOwner.current
            val readerSession = remember { NfcReaderSession() }
            val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
            DisposableEffect(nfcListening, lifecycleOwner) {
                val activity = context as? android.app.Activity
                val adapter = nfcAdapter
                if (!nfcListening || activity == null || adapter == null) {
                    return@DisposableEffect onDispose { readerSession.disarm() }
                }
                fun disable() {
                    readerSession.disarm()
                    runCatching { adapter.disableReaderMode(activity) }
                }
                fun enable() {
                    val gen = readerSession.arm()
                    runCatching {
                        adapter.enableReaderMode(activity, { tag ->
                            if (!readerSession.isCurrent(gen)) return@enableReaderMode
                            val uri = readContactUri(tag) ?: return@enableReaderMode
                            mainHandler.post {
                                if (readerSession.consume(gen)) {
                                    qrContent = uri
                                    inPerson = true
                                    vm.addFromQrContent(uri, ImportProvenance.IN_PERSON)
                                }
                            }
                        }, NFC_READER_FLAGS, null)
                    }.onFailure { readerSession.disarm() }
                }
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> enable()
                        Lifecycle.Event.ON_PAUSE -> disable()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer) // replays ON_RESUME if already resumed
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    disable()
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
                    if (pasted.isNotEmpty()) {
                        qrContent = pasted
                        inPerson = false
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = qrContent,
                onValueChange = {
                    qrContent = it
                    inPerson = false
                },
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
                onClick = {
                    vm.addFromQrContent(
                        qrContent,
                        if (inPerson) ImportProvenance.IN_PERSON else ImportProvenance.REMOTE
                    )
                },
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

private const val NFC_READER_FLAGS = android.nfc.NfcAdapter.FLAG_READER_NFC_A or
    android.nfc.NfcAdapter.FLAG_READER_NFC_B or
    android.nfc.NfcAdapter.FLAG_READER_NFC_F or
    android.nfc.NfcAdapter.FLAG_READER_NFC_V

/** Reads the NDEF message through the tag technology (never Activity extras); failures yield null. */
private fun readContactUri(tag: android.nfc.Tag): String? = runCatching {
    val ndef = android.nfc.tech.Ndef.get(tag) ?: return null
    ndef.connect()
    try {
        val message = ndef.ndefMessage ?: return null
        NfcContactUri.select(message.records.map { record -> runCatching { record.toUri()?.toString() }.getOrNull() })
    } finally {
        runCatching { ndef.close() }
    }
}.getOrNull()

/**
 * SCT-02 warning for an unbound legacy SecureID. Back / outside tap is a cancel; the text scrolls so
 * the full warning stays reachable at large font scales. Callers bind confirm/cancel to one candidate.
 */
@Composable
fun LegacyConsentDialog(
    sxId: String,
    keyFingerprint: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.testTag("legacy_consent_dialog"),
        icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.legacy_contact_title)) },
        text = {
            Text(
                stringResource(R.string.legacy_contact_message, sxId, keyFingerprint),
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("legacy_consent_confirm")) {
                Text(stringResource(R.string.legacy_contact_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("legacy_consent_cancel")) {
                Text(stringResource(R.string.legacy_contact_cancel))
            }
        }
    )
}
