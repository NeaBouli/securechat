package com.stealthx.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IFRUnlockScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IFR Token Unlock") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp)) {
            Text("Lock once. Unlock forever.", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Same IFR lock works across SecureCall, SecureChat, and Chameleon.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(24.dp))

            TierCard("Free", "0 IFR", "Current tier", false)
            Spacer(Modifier.height(12.dp))
            TierCard("Pro", ">= 2,000 IFR", "Unlimited contacts, groups, Kaspa identity", true)
            Spacer(Modifier.height(12.dp))
            TierCard("Elite", ">= 6,000 IFR", "3-hop onion, decoy profile, relay access", true)
            Spacer(Modifier.height(12.dp))
            TierCard("Suite", ">= 8,000 IFR", "All StealthX products, lifetime", true)

            Spacer(Modifier.weight(1f))
            Button(onClick = { /* TODO: WalletConnect */ },
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Lock, null)
                Spacer(Modifier.width(8.dp))
                Text("Connect Wallet")
            }
        }
    }
}

@Composable
private fun TierCard(tier: String, threshold: String, description: String, active: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                             else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tier, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(threshold, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}
