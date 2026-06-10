package com.stealthx.presentation.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.stealthx.data.exchange.ContactExchangeEvent
import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.identity.PublicKeyBundleQr
import com.stealthx.data.identity.StealthXId
import com.stealthx.data.identity.StealthXIdentity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyIdScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val exchangeManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MyIdScreenEntryPoint::class.java
        ).contactExchangeManager()
    }
    var identity by remember { mutableStateOf<StealthXId?>(null) }
    var qrContent by remember { mutableStateOf<String?>(null) }
    var inviteUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var lastContactExchange by remember { mutableStateOf<ContactExchangeEvent?>(null) }

    suspend fun loadIdentity() {
        val (id, qr, url) = withContext(Dispatchers.IO) {
            val id = runCatching { StealthXIdentity.getOrCreateWithSeed(context) }.getOrNull()
            val bundle = if (id != null) {
                runCatching { StealthXIdentity.createPublicKeyBundle(context) }.getOrNull()
            } else null
            val qr = bundle?.let { runCatching { PublicKeyBundleQr.toQrContent(it) }.getOrNull() }
            val url = bundle?.let { runCatching { PublicKeyBundleQr.toInviteUrl(it) }.getOrNull() }
            Triple(id, qr, url)
        }
        identity = id
        qrContent = qr
        inviteUrl = url
        isLoading = false
    }

    LaunchedEffect(Unit) { loadIdentity() }
    LaunchedEffect(exchangeManager) {
        exchangeManager.contactExchangeEvents.collect { event ->
            lastContactExchange = event
            snackbarHostState.showSnackbar("${event.displayName} added you")
        }
    }

    val sxId = identity?.raw ?: "not initialized"
    val handle = identity?.customHandle
    val qrBitmap = remember(qrContent) { qrContent?.let(::qrBitmap) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My ID") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your StealthX ID",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Text(handle ?: sxId,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary)
            if (handle != null) {
                Text(sxId,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier.size(220.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> CircularProgressIndicator()
                        qrBitmap != null -> Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Contact QR Code",
                            modifier = Modifier.size(196.dp)
                        )
                        else -> Text("QR unavailable", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            when {
                isLoading -> {}
                identity == null -> {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Identity not initialized", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("This can happen after a fresh install if the first launch failed. Tap below to generate your identity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            OutlinedButton(onClick = {
                                scope.launch { loadIdentity() }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("Generate / Repair Identity")
                            }
                        }
                    }
                }
                else -> Text("Your SecureChat identity. Share this to receive encrypted messages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            lastContactExchange?.let { event ->
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Contact added you",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${event.displayName} added you to SecureChat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Button(onClick = {
                val link = inviteUrl ?: qrContent
                if (link != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, link)
                    }
                    context.startActivity(Intent.createChooser(intent, "Invite to SecureChat"))
                }
            },
                enabled = inviteUrl != null || qrContent != null,
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Invite via Secure Link")
            }

            // NFC share button
            val nfcAdapter = remember { android.nfc.NfcAdapter.getDefaultAdapter(context) }
            if (nfcAdapter != null && qrContent != null) {
                var nfcReady by remember { mutableStateOf(false) }
                val nfcWriteState by com.stealthx.data.NfcWriteRelay.state.collectAsState()
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        nfcReady = !nfcReady
                        if (!nfcReady) com.stealthx.data.NfcWriteRelay.reset()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Nfc, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (nfcReady) "NFC ready — tap to cancel" else "Share via NFC Tap")
                }
                if (nfcReady) {
                    val (statusText, statusColor) = when (val s = nfcWriteState) {
                        is com.stealthx.data.NfcWriteState.Pending ->
                            "Touch an NFC tag to write your contact" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        is com.stealthx.data.NfcWriteState.Success ->
                            "Tag written — recipient can tap to add you" to MaterialTheme.colorScheme.primary
                        is com.stealthx.data.NfcWriteState.Failure ->
                            s.reason to MaterialTheme.colorScheme.error
                        else -> "Initialising NFC..." to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    DisposableEffect(Unit) {
                        val activity = context as? android.app.Activity ?: return@DisposableEffect onDispose {}
                        com.stealthx.data.NfcWriteRelay.post(qrContent)
                        val pendingIntent = android.app.PendingIntent.getActivity(
                            context, 0,
                            android.content.Intent(context, activity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP),
                            android.app.PendingIntent.FLAG_MUTABLE
                        )
                        val filters = arrayOf(
                            android.content.IntentFilter(android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED),
                            android.content.IntentFilter(android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED)
                        )
                        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, null)
                        onDispose {
                            com.stealthx.data.NfcWriteRelay.reset()
                            runCatching { nfcAdapter.disableForegroundDispatch(activity) }
                        }
                    }
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface MyIdScreenEntryPoint {
    fun contactExchangeManager(): ContactExchangeManager
}

private fun qrBitmap(content: String): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
    val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
