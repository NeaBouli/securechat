package com.stealthx.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onIfrClick: () -> Unit) {
    var biometricsEnabled by remember { mutableStateOf(true) }
    var defaultSecurity by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SectionHeader("Security")
            ToggleRow(icon = Icons.Default.Fingerprint, title = "Biometric Unlock",
                checked = biometricsEnabled, onCheckedChange = { biometricsEnabled = it })
            ToggleRow(icon = Icons.Default.Shield, title = "Maximum Security by Default",
                checked = defaultSecurity, onCheckedChange = { defaultSecurity = it })

            SectionHeader("Access")
            ClickRow(icon = Icons.Default.Lock, title = "IFR Token Unlock",
                subtitle = "Lock tokens for lifetime access", onClick = onIfrClick)

            SectionHeader("About")
            ClickRow(icon = Icons.Default.Shield, title = "Version 0.1.0-alpha",
                subtitle = "SecureChat — StealthX Platform", onClick = {})
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp))
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ClickRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Icon(Icons.Default.ChevronRight, null)
    }
}
