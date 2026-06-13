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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet for IFR wallet connection and verification.
 *
 * Wallet app deep link for WalletConnect-based IFR verification.
 */
@Composable
fun IFRUnlockSheet(
    onWalletConnectClicked: () -> Unit,
    isVerifying: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
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
            text = "Hold IFR tokens in your Ethereum wallet to unlock premium features.\nOne-time read-only verification — no data leaves your device.",
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
                text = if (isVerifying) "Verifying..." else "Open Wallet",
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }
    }
}
