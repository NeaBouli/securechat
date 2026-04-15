/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet for IFR wallet connection and verification.
 *
 * Options:
 * 1. WalletConnect deep link (opens MetaMask/Trust Wallet)
 * 2. Manual wallet address entry (30-day expiry)
 */
@Composable
fun IFRUnlockSheet(
    onWalletConnectClicked: () -> Unit,
    onManualAddressSubmit: (String) -> Unit,
    isVerifying: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    var manualAddress by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Unlock with IFR Token",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Lock IFR tokens on Ethereum to unlock premium features.\nOne-time verification — no data leaves your device.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onWalletConnectClicked,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isVerifying,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
        ) {
            Text(
                text = if (isVerifying) "Verifying..." else "Connect Wallet",
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showManualInput = !showManualInput },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter Address Manually", color = Color(0xFF00E5FF))
        }

        if (showManualInput) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = manualAddress,
                onValueChange = { manualAddress = it },
                label = { Text("Ethereum Address (0x...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onManualAddressSubmit(manualAddress) },
                enabled = manualAddress.startsWith("0x") && manualAddress.length == 42 && !isVerifying,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify")
            }
            Text(
                text = "Manual verification expires after 30 days",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }
    }
}
