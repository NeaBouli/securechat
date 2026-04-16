/*
 * SecureChat — Emergency Broadcast Screen
 * Elite tier feature — gated via TierGate.
 */
package com.stealthx.features.broadcast

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Full broadcast UI — shown to Elite users only.
 * Non-Elite users see BroadcastLockedScreen (below).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    recipientCount: Int
) {
    var message by remember { mutableStateOf("") }
    var confirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Emergency Broadcast", fontWeight = FontWeight.Bold)
                        Text("Elite tier",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                )
            ) {
                Row(Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null,
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text("This sends an encrypted alert to all $recipientCount contacts at once. Each message is individually encrypted — no shared group key.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Broadcast message") },
                placeholder = { Text("Type your emergency alert...") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 4
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { confirmDialog = true },
                enabled = message.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Send, null)
                Spacer(Modifier.width(8.dp))
                Text("Send to All Contacts")
            }
        }
    }

    if (confirmDialog) {
        AlertDialog(
            onDismissRequest = { confirmDialog = false },
            icon = { Icon(Icons.Default.Warning, null,
                tint = MaterialTheme.colorScheme.error) },
            title = { Text("Send Broadcast?") },
            text = { Text("This will send your message to all $recipientCount contacts. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onSend(message)
                    confirmDialog = false
                    onBack()
                }) {
                    Text("Send Now", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Shown to Free/Pro users — Elite upgrade prompt.
 * Wire this via TierGate in the nav graph.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastLockedScreen(
    onUnlock: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Emergency Broadcast") })
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Icon(Icons.Default.Lock, null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Elite Feature",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Emergency Broadcast",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(24.dp))
            Text("Send an encrypted alert to ALL your contacts at once. One tap. Every contact notified. No group server. Each message individually encrypted.",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("Available with: >= 6,000 IFR or Elite Lifetime (EUR 19)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock with IFR Token")
            }
        }
    }
}
