package com.stealthx.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stealthx.ifr.compose.TierStatusCard
import com.stealthx.shared.model.IfrTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onIfrClick: () -> Unit,
    onBroadcastClick: () -> Unit,
    onSetupClick: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val tier by vm.currentTier.collectAsState()
    val biometricsEnabled by vm.biometricEnabled.collectAsState()
    val stealthDeleteEnabled by vm.stealthDeleteEnabled.collectAsState()
    val activationState by vm.activationState.collectAsState()
    val context = LocalContext.current
    var showActivationDialog by remember { mutableStateOf(false) }
    fun openUrl(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    if (showActivationDialog) {
        ActivationCodeDialog(
            state = activationState,
            onDismiss = { showActivationDialog = false; vm.resetActivationState() },
            onSubmit = vm::activateCode
        )
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Tier card
            TierStatusCard(
                tier = tier,
                ifrBalance = 0,
                walletAddress = null,
                expiresIn = null,
                modifier = Modifier.fillMaxWidth()
            )

            if (tier != IfrTier.ELITE) {
                Button(
                    onClick = onIfrClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tier == IfrTier.FREE) Color(0xFF00E5FF) else Color(0xFFFFD700)
                    )
                ) {
                    Icon(Icons.Default.Lock, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (tier == IfrTier.FREE) "Upgrade to Pro — Lock 2,000 IFR" else "Upgrade to Elite — Lock 6,000 IFR",
                        color = Color.Black
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Security section
            SectionHeader("Security")
            ToggleRow(Icons.Default.Fingerprint, "Biometric Unlock", biometricsEnabled) { vm.setBiometricEnabled(it) }
            ToggleRow(Icons.Default.Shield, "STEALTH-DELETE (5-tap)", stealthDeleteEnabled, "Tap the lock icon in the chat list 5× to wipe") { vm.setStealthDeleteEnabled(it) }

            // Free features
            SectionHeader("Free")
            FeatureRow(Icons.Default.Message, "E2E Encrypted Messaging", "XChaCha20-Poly1305 + Double Ratchet", false)
            FeatureRow(Icons.Default.QrCode, "QR Key Exchange", "Device-to-device, no server", false)
            ContactLimitRow(tier)

            // Pro features
            SectionHeader("Pro  ≥ 2,000 IFR")
            GatedFeatureRow(Icons.Default.Group, "Group Messaging", "Encrypted group chats", tier, IfrTier.PRO, onIfrClick, comingSoon = true)
            GatedFeatureRow(Icons.Default.AttachFile, "Encrypted File Transfer", "E2E via Kaspa XFTP", tier, IfrTier.PRO, onIfrClick, comingSoon = true)
            GatedFeatureRow(Icons.Default.AccountTree, "Kaspa Identity Anchor", "Public key on BlockDAG", tier, IfrTier.PRO, onIfrClick, comingSoon = true)
            GatedFeatureRow(Icons.Default.Security, "Chameleon Integration", "Context-aware overlay", tier, IfrTier.PRO, onIfrClick, comingSoon = true)

            // Elite features
            SectionHeader("Elite  ≥ 6,000 IFR")
            GatedFeatureRow(Icons.Default.Router, "Onion Routing (3-hop)", "Full IP protection", tier, IfrTier.ELITE, onIfrClick, comingSoon = true)
            GatedFeatureRow(Icons.Default.FaceRetouchingNatural, "Decoy Chat Profiles", "Fake conversations on demand", tier, IfrTier.ELITE, onIfrClick, comingSoon = true)
            GatedFeatureRow(Icons.Default.Radar, "Advanced Threat Detection", "Real-time behavioral analysis", tier, IfrTier.ELITE, onIfrClick, comingSoon = true)
            GatedFeatureRow(Icons.Default.Send, "Emergency Broadcast", "Encrypted alert to all contacts", tier, IfrTier.ELITE, onIfrClick, onBroadcastClick, comingSoon = true)

            SectionHeader("Access")
            ClickRow(Icons.Default.Lock, "IFR Token Unlock", "Lock tokens for lifetime access", onIfrClick)
            ClickRow(Icons.Default.Key, "Activation Code", "Enter code to unlock Pro or Elite tier") { showActivationDialog = true }

            SectionHeader("Help")
            ClickRow(Icons.Default.MenuBook, "User Manual", "How SecureChat works + first setup") {
                openUrl("https://securechat.stealthx.tech/wiki/user-manual.html")
            }
            ClickRow(Icons.Default.RocketLaunch, "Getting Started", "Step-by-step setup guide", onSetupClick)

            SectionHeader("About")
            ClickRow(Icons.Default.Shield, "Version 0.1.0-alpha", "SecureChat — StealthX Platform") {}
        }
    }
}

@Composable
private fun ContactLimitRow(tier: IfrTier) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Contacts, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Contacts", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (tier >= IfrTier.PRO) "Unlimited" else "10 max (Free tier)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun GatedFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    currentTier: IfrTier,
    requiredTier: IfrTier,
    onUnlock: () -> Unit,
    onOpen: (() -> Unit)? = null,
    comingSoon: Boolean = false
) {
    val locked = currentTier < requiredTier
    val eliteColor = Color(0xFFFFD700)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!locked && !comingSoon && onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = if (locked || comingSoon) Color.Gray.copy(alpha = 0.4f)
                   else if (requiredTier == IfrTier.ELITE) eliteColor
                   else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = if (locked || comingSoon) Color.Gray else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        if (comingSoon) {
            Text(
                "SOON",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF888888),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        } else if (locked) {
            TextButton(onClick = onUnlock) {
                Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Unlock", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF))
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String, locked: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00FF88), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
    )
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, checked: Boolean, subtitle: String? = null, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ClickRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Icon(Icons.Default.ChevronRight, null)
    }
}

@Composable
private fun ActivationCodeDialog(
    state: ActivationState,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val isLoading = state is ActivationState.Loading
    val isDone = state is ActivationState.Success

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Enter Activation Code") },
        text = {
            Column {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Code") },
                    placeholder = { Text("XXXX-XXXX-XXXX") },
                    singleLine = true,
                    enabled = !isLoading && !isDone,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                when (state) {
                    is ActivationState.Error -> Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    is ActivationState.Success -> Text(
                        "Unlocked: ${state.tier.name}",
                        color = Color(0xFF00E676),
                        style = MaterialTheme.typography.bodySmall
                    )
                    is ActivationState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (isDone) {
                TextButton(onClick = onDismiss) { Text("Done") }
            } else {
                TextButton(
                    onClick = { onSubmit(code) },
                    enabled = code.isNotBlank() && !isLoading
                ) { Text("Activate") }
            }
        },
        dismissButton = {
            if (!isDone) {
                TextButton(onClick = { if (!isLoading) onDismiss() }) { Text("Cancel") }
            }
        }
    )
}
