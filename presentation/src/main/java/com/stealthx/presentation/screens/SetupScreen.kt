package com.stealthx.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.stealthx.data.identity.StealthXIdentity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onContinue: () -> Unit) {
    val context = LocalContext.current

    var identity by remember { mutableStateOf(StealthXIdentity.get(context)) }
    val hasIdentity = identity != null

    val hasNotification = remember {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Getting Started") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Complete these steps to start using SecureChat.",
                style = MaterialTheme.typography.bodyLarge
            )

            // Step 1: Identity
            SetupCheckItem(
                icon = Icons.Default.Key,
                title = "Your SecureChat Identity",
                description = if (hasIdentity)
                    "Identity ready: ${identity!!.raw}"
                else
                    "No identity found. Tap to generate.",
                done = hasIdentity,
                action = if (!hasIdentity) "Generate Identity" else null,
                onAction = {
                    runCatching { StealthXIdentity.getOrCreateWithSeed(context) }
                        .onSuccess { identity = it }
                }
            )

            // Step 2: Notifications
            SetupCheckItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = if (hasNotification)
                    "Notifications enabled — you'll be alerted on new messages."
                else
                    "Enable notifications to receive message alerts.",
                done = hasNotification
            )

            // Step 3: Add first contact
            SetupCheckItem(
                icon = Icons.Default.ContactPage,
                title = "Add your first contact",
                description = "Go to Contacts and scan a contact's QR code, or share your own ID link so they can add you.",
                done = false,
                action = null
            )

            Spacer(Modifier.height(8.dp))

            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            Text(
                "SecureChat and SecureCall use separate identities — they share the same format " +
                "but NOT the same storage. To connect both, share your SecureChat QR in My ID.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(8.dp))

            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Continue to SecureChat")
            }
        }
    }
}

@Composable
private fun SetupCheckItem(
    icon: ImageVector,
    title: String,
    description: String,
    done: Boolean,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (done) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                if (action != null) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = onAction) { Text(action) }
                }
            }
        }
    }
}
