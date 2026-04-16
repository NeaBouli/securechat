package com.stealthx.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewContactScreen(
    onBack: () -> Unit,
    onContactAdded: () -> Unit
) {
    var manualId by remember { mutableStateOf("") }

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
            OptionCard(
                icon = Icons.Default.QrCodeScanner,
                title = "Scan QR Code",
                subtitle = "Fastest way to add a contact",
                onClick = { /* TODO: launch QR scanner */ }
            )
            OptionCard(
                icon = Icons.Default.Nfc,
                title = "NFC Tap",
                subtitle = "Hold phones together",
                onClick = { /* TODO: NFC */ }
            )
            OptionCard(
                icon = Icons.Default.Edit,
                title = "Enter sx_ ID manually",
                subtitle = "For verified contacts only",
                onClick = { /* focus input */ }
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = manualId,
                onValueChange = { manualId = it },
                label = { Text("sx_ID") },
                placeholder = { Text("sx_a7Kx9mPq2") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onContactAdded() },
                enabled = manualId.startsWith("sx_") && manualId.length >= 10,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Contact")
            }
        }
    }
}

@Composable
private fun OptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}
