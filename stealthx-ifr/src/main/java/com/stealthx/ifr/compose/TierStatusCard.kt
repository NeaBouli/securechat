/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stealthx.shared.model.IfrTier

@Composable
fun TierStatusCard(
    tier: IfrTier,
    ifrBalance: Long,
    walletAddress: String?,
    expiresIn: String?,
    modifier: Modifier = Modifier
) {
    val tierColor = when (tier) {
        IfrTier.FREE -> Color.Gray
        IfrTier.PRO -> Color(0xFF00E5FF)
        IfrTier.ELITE -> Color(0xFFFFD700)
    }

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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "IFR Tier",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = tier.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = tierColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (walletAddress != null) {
                Text(
                    text = "${walletAddress.take(6)}...${walletAddress.takeLast(4)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (ifrBalance > 0) {
                val displayBalance = ifrBalance / 1_000_000_000L
                Text(
                    text = "$displayBalance IFR locked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            if (expiresIn != null) {
                Text(
                    text = "Expires: $expiresIn",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
