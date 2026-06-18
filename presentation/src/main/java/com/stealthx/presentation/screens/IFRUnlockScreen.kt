package com.stealthx.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stealthx.presentation.theme.ScGold
import com.stealthx.presentation.theme.ScGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IFRUnlockScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "SecureChat Access",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "WalletConnect was removed from the Android app. Buy normally or verify IFR in the browser for a 50% Stripe discount.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Pro
                        OutlinedButton(
                            onClick = { openUrl("https://securechat.stealthx.tech/#lifetime") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ScGreen
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PRO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("€9", fontWeight = FontWeight.Black, fontSize = 22.sp, color = ScGreen)
                                Text("one-time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // Elite
                        OutlinedButton(
                            onClick = { openUrl("https://securechat.stealthx.tech/#lifetime") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ScGold
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ELITE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("€19", fontWeight = FontWeight.Black, fontSize = 22.sp, color = ScGold)
                                Text("one-time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { openUrl("https://securechat.stealthx.tech/#ifr") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Verify IFR for 50% Stripe Discount", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { openUrl("https://app.uniswap.org/explore/tokens/ethereum/0x77e99917Eca8539c62F509ED1193ac36580A6e7B") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ScGold)
                    ) {
                        Text("Buy IFR on Uniswap")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { openUrl("https://securechat.stealthx.tech/#lifetime") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF635BFF))
                    ) {
                        Icon(Icons.Default.CreditCard, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buy with Card (Stripe)")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You will receive an activation code by email. Enter it in Settings → Activation Code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp
                    )
                }
            }

        }
    }
}
