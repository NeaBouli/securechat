package com.stealthx.presentation.screens

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.stealthx.presentation.theme.ScCyan
import com.stealthx.presentation.theme.ScGreen
import com.stealthx.presentation.theme.ScSurface2
import com.stealthx.presentation.theme.ScSurface3
import com.stealthx.presentation.theme.ScTextDim
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onSend: (String) -> Unit,
    onExportLatest: () -> Unit,
    onClearExport: () -> Unit,
    onImport: (String) -> Unit,
    onBack: () -> Unit,
    onSetDisappearTimer: (Long?) -> Unit = {}
) {
    var input by remember { mutableStateOf("") }
    var importContent by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var showSafetyNumber by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(onImport)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            state.contactSxId,
                            style = MaterialTheme.typography.labelSmall,
                            color = ScTextDim,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scanLauncher.launch(
                            ScanOptions()
                                .setPrompt("Scan StealthX message")
                                .setBeepEnabled(false)
                                .setOrientationLocked(false)
                        )
                    }) {
                        Icon(Icons.Default.QrCodeScanner, "Scan message")
                    }
                    IconButton(onClick = { showImport = true }) {
                        Icon(Icons.Default.FileUpload, "Import message")
                    }
                    IconButton(onClick = { showSafetyNumber = true }) {
                        Icon(Icons.Default.Shield, "Safety number",
                            tint = ScGreen)
                    }
                    Box {
                        IconButton(onClick = { showTimerMenu = true }) {
                            Icon(
                                Icons.Default.Timer,
                                "Disappearing messages",
                                tint = if (state.disappearTimerMs != null) ScCyan
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showTimerMenu,
                            onDismissRequest = { showTimerMenu = false }
                        ) {
                            val timerOptions = listOf(
                                null to "Off",
                                1 * 60 * 60 * 1000L to "1 hour",
                                24 * 60 * 60 * 1000L to "24 hours",
                                7 * 24 * 60 * 60 * 1000L to "7 days"
                            )
                            timerOptions.forEach { (duration, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            fontWeight = if (state.disappearTimerMs == duration) FontWeight.Bold
                                                         else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSetDisappearTimer(duration)
                                        showTimerMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Encrypted message…", color = ScTextDim) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank()) { onSend(input); input = "" }
                    })
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            if (input.isNotBlank()) Brush.linearGradient(listOf(ScGreen, ScCyan))
                            else Brush.linearGradient(listOf(ScSurface3, ScSurface3))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { if (input.isNotBlank()) { onSend(input); input = "" } },
                        enabled = input.isNotBlank() && !state.isSending
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send",
                            tint = if (input.isNotBlank())
                                androidx.compose.ui.graphics.Color.Black
                            else ScTextDim)
                    }
                }
            }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (state.errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            state.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            if (state.messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, null,
                            modifier = Modifier.size(48.dp),
                            tint = ScGreen.copy(alpha = 0.4f))
                        Text("End-to-end encrypted",
                            style = MaterialTheme.typography.bodySmall,
                            color = ScTextDim)
                        Text("Messages are secured with Double Ratchet + XChaCha20-Poly1305",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScTextDim.copy(alpha = 0.6f))
                    }
                }
            }
            items(state.messages) { msg ->
                MessageBubble(msg)
                Spacer(Modifier.height(4.dp))
            }
        }
    }

    if (showSafetyNumber) {
        AlertDialog(
            onDismissRequest = { showSafetyNumber = false },
            title = { Text("Safety Number") },
            text = {
                Column {
                    Text(
                        state.safetyNumber.ifEmpty { "Computing…" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Compare this number with your contact out-of-band. A mismatch indicates a man-in-the-middle attack.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSafetyNumber = false }) { Text("OK") }
            }
        )
    }

    if (state.exportedMessage != null) {
        AlertDialog(
            onDismissRequest = onClearExport,
            title = { Text("Message QR") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        bitmap = qrBitmap(state.exportedMessage).asImageBitmap(),
                        contentDescription = "Message QR code",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    )
                    SelectionContainer {
                        Text(
                            state.exportedMessage,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onClearExport) { Text("OK") }
            }
        )
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Import Message") },
            text = {
                OutlinedTextField(
                    value = importContent,
                    onValueChange = { importContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onImport(importContent)
                    importContent = ""
                    showImport = false
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMessageUi) {
    val outgoingGradient = Brush.linearGradient(listOf(ScGreen, ScCyan.copy(alpha = 0.85f)))
    val timeText = remember(msg.timestamp) { formatMsgTime(msg.timestamp) }

    // Live countdown for disappearing messages
    var remainingMs by remember(msg.expiresAt) {
        mutableLongStateOf(msg.expiresAt?.let { it - System.currentTimeMillis() } ?: -1L)
    }
    if (msg.expiresAt != null) {
        LaunchedEffect(msg.expiresAt) {
            while (remainingMs > 0) {
                delay(1_000)
                remainingMs = msg.expiresAt - System.currentTimeMillis()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        val bubbleShape = RoundedCornerShape(
            topStart = 18.dp, topEnd = 18.dp,
            bottomStart = if (msg.isOutgoing) 18.dp else 4.dp,
            bottomEnd   = if (msg.isOutgoing) 4.dp  else 18.dp
        )
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(bubbleShape)
                .then(
                    if (msg.isOutgoing)
                        Modifier.background(outgoingGradient)
                    else
                        Modifier.background(ScSurface3)
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(
                    msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.isOutgoing) androidx.compose.ui.graphics.Color.Black
                            else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Disappear countdown
                    if (msg.expiresAt != null && remainingMs > 0) {
                        val countdownColor = when {
                            remainingMs < 60_000 -> MaterialTheme.colorScheme.error
                            remainingMs < 300_000 -> ScCyan
                            else -> if (msg.isOutgoing)
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
                            else ScTextDim
                        }
                        Icon(
                            Icons.Default.LocalFireDepartment, null,
                            modifier = Modifier.size(11.dp),
                            tint = countdownColor
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            formatCountdown(remainingMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = countdownColor
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        timeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (msg.isOutgoing)
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)
                        else ScTextDim
                    )
                    if (msg.isOutgoing) {
                        Spacer(Modifier.width(3.dp))
                        val (icon, tint) = when (msg.deliveryStatus) {
                            "FAILED" -> Icons.Default.Close to MaterialTheme.colorScheme.error
                            "QUEUED" -> Icons.Default.Schedule to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)
                            "READ"   -> Icons.Default.DoneAll  to ScCyan
                            else     -> Icons.Default.Done     to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
                        }
                        Icon(icon, null, modifier = Modifier.size(13.dp), tint = tint)
                    }
                }
            }
        }
    }
}

private fun formatCountdown(ms: Long): String {
    if (ms <= 0) return "0s"
    val totalSeconds = ms / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        days > 0    -> "${days}d ${hours}h"
        hours > 0   -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else        -> "${seconds}s"
    }
}

private fun formatMsgTime(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val dt = Instant.ofEpochMilli(timestamp).atZone(zone)
    val fmt = if (dt.toLocalDate() == LocalDate.now(zone))
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    else
        DateTimeFormatter.ofPattern("dd.MM  HH:mm", Locale.getDefault())
    return dt.format(fmt)
}

private fun qrBitmap(content: String): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 720, 720)
    return Bitmap.createBitmap(720, 720, Bitmap.Config.RGB_565).also { bitmap ->
        for (x in 0 until 720) {
            for (y in 0 until 720) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
    }
}
