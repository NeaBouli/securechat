package com.stealthx.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyIdScreen(onBack: () -> Unit) {
    // TODO: pull from StealthXIdentity.get(context)
    val sxId = "sx_a7Kx9mPq2"
    val handle: String? = null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My ID") },
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

            // QR placeholder — real QR via zxing in contacts module
            Surface(
                modifier = Modifier.size(220.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.QrCode, "QR Code",
                        modifier = Modifier.size(180.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Works in SecureCall AND SecureChat",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            Text("Share once — your contact can reach you on both.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

            Spacer(Modifier.weight(1f))
            Button(onClick = { /* TODO: share intent */ },
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Share Deep Link")
            }
        }
    }
}
