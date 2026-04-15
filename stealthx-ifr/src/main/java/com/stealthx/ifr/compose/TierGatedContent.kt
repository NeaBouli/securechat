/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stealthx.shared.model.IfrTier

/**
 * TierGatedContent — THE ONLY composable wrapper for feature gating.
 *
 * Shows [content] if [currentTier] >= [requiredTier].
 * Otherwise shows a LockedFeatureCard with "Unlock with IFR" CTA.
 *
 * NO other Guard/Gate/Check composable may exist in the codebase.
 */
@Composable
fun TierGatedContent(
    currentTier: IfrTier,
    requiredTier: IfrTier,
    featureName: String,
    onUnlockClicked: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (currentTier >= requiredTier) {
        content()
    } else {
        LockedFeatureCard(
            featureName = featureName,
            requiredTier = requiredTier,
            onUnlockClicked = onUnlockClicked,
            modifier = modifier
        )
    }
}

@Composable
fun LockedFeatureCard(
    featureName: String,
    requiredTier: IfrTier,
    onUnlockClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = featureName,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Requires ${requiredTier.name} tier",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF00E5FF)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lock ${requiredTier.minLockAmount / 1_000_000_000L} IFR to unlock",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUnlockClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF)
                )
            ) {
                Text("Unlock with IFR", color = Color.Black)
            }
        }
    }
}
