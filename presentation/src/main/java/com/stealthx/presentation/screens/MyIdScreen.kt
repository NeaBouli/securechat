package com.stealthx.presentation.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.stealthx.data.identity.PublicKeyBundleQr
import com.stealthx.data.identity.StealthXIdentity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyIdScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var identity by remember { mutableStateOf(StealthXIdentity.get(context)) }
    val sxId = identity?.raw ?: "not initialized"
    val handle = identity?.customHandle
    var qrContent by remember(identity) {
        mutableStateOf(
            runCatching {
                if (identity != null)
                    PublicKeyBundleQr.toQrContent(StealthXIdentity.createPublicKeyBundle(context))
                else null
            }.getOrNull()
        )
    }
    val qrBitmap = remember(qrContent) { qrContent?.let(::qrBitmap) }

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

            Surface(
                modifier = Modifier.size(220.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Contact QR Code",
                            modifier = Modifier.size(196.dp)
                        )
                    } else {
                        Text("QR unavailable", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            if (identity == null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Identity not initialized", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("This can happen after a fresh install if the first launch failed. Tap below to generate your identity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        OutlinedButton(onClick = {
                            runCatching { StealthXIdentity.getOrCreateWithSeed(context) }
                                .onSuccess { id -> identity = id }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Generate / Repair Identity")
                        }
                    }
                }
            } else {
                Text("Your SecureChat identity. Share this to receive encrypted messages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }

            Spacer(Modifier.weight(1f))
            Button(onClick = {
                if (qrContent != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, qrContent)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share StealthX ID"))
                }
            },
                enabled = qrContent != null,
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Invite via Secure Link")
            }
        }
    }
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
