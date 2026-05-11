package com.stealthx.presentation.screens

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onSend: (String) -> Unit,
    onExportLatest: () -> Unit,
    onClearExport: () -> Unit,
    onImport: (String) -> Unit,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var importContent by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var showSafetyNumber by remember { mutableStateOf(false) }
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(onImport)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.contactSxId, fontWeight = FontWeight.SemiBold)
                        Text("Encrypted local queue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                        Icon(Icons.Default.Shield, "Safety number")
                    }
                }
            )
        },
        bottomBar = {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Encrypted message…") }
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            onSend(input)
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank() && !state.isSending
                ) {
                    Icon(Icons.Default.Send, "Send",
                        tint = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onExportLatest) {
                    Text("QR")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            reverseLayout = false
        ) {
            if (state.errorMessage != null) {
                item {
                    Text(
                        state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            if (state.messages.isEmpty()) {
                item {
                    Text(
                        "No messages yet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
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
                Text("12345 67890 12345 67890 12345 67890\n\nCompare with your contact to verify authenticity.",
                    style = MaterialTheme.typography.bodyMedium)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (msg.isOutgoing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (msg.isOutgoing) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (msg.isOutgoing) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp, 8.dp)) {
                Text(msg.text)
                if (msg.isOutgoing) {
                    Text(
                        msg.deliveryStatus.lowercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
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
