package com.stealthx.presentation.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stealthx.presentation.theme.ScGold
import com.stealthx.presentation.theme.ScGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
    vm: UpgradeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, "No browser available for this link", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Access") },
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
                        "Paid activation is launch-gated. VLABS controls current availability; signed activation codes remain in Settings.",
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
                            onClick = vm::buy,
                            enabled = false,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ScGreen
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PRO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Gated", fontWeight = FontWeight.Black, fontSize = 22.sp, color = ScGreen)
                                Text("VLABS", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // Elite
                        OutlinedButton(
                            onClick = vm::buy,
                            enabled = false,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ScGold
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ELITE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Gated", fontWeight = FontWeight.Black, fontSize = 22.sp, color = ScGold)
                                Text("VLABS", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = vm::buy,
                            enabled = false,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ScGreen)
                        ) {
                            Text("Pro Monthly")
                        }
                        OutlinedButton(
                            onClick = vm::buy,
                            enabled = false,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ScGold)
                        ) {
                            Text("Elite Monthly")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = vm::buy,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF635BFF))
                    ) {
                        Icon(Icons.Default.CreditCard, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buy Elite Activation Code")
                    }
                    TextButton(onClick = vm::restorePurchases, enabled = false) {
                        Text("Google Play activation not available")
                    }
                    TextButton(onClick = { openUrl("https://vlabs.gr/en/shop?focus=security") }) {
                        Text("Check VLABS availability")
                    }
                    Text(
                        state.status,
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
