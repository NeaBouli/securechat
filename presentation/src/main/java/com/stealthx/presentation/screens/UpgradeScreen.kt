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
import androidx.compose.runtime.LaunchedEffect
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
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
    vm: UpgradeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.connect()
    }

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
                        "Buy Pro or Elite with Google Play. Activation codes still work from Settings.",
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
                            onClick = { vm.buy(activity, "securechat_pro_lifetime") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ScGreen
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PRO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(state.products["securechat_pro_lifetime"]?.price ?: "€9", fontWeight = FontWeight.Black, fontSize = 22.sp, color = ScGreen)
                                Text("lifetime", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // Elite
                        OutlinedButton(
                            onClick = { vm.buy(activity, "securechat_elite_lifetime") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ScGold
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ELITE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(state.products["securechat_elite_lifetime"]?.price ?: "€19", fontWeight = FontWeight.Black, fontSize = 22.sp, color = ScGold)
                                Text("lifetime", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { vm.buy(activity, "securechat_pro_monthly") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ScGreen)
                        ) {
                            Text("Pro Monthly")
                        }
                        OutlinedButton(
                            onClick = { vm.buy(activity, "securechat_elite_monthly") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ScGold)
                        ) {
                            Text("Elite Monthly")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { vm.buy(activity, "securechat_elite_activation_code") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF635BFF))
                    ) {
                        Icon(Icons.Default.CreditCard, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buy Elite Activation Code")
                    }
                    TextButton(onClick = vm::restorePurchases, enabled = !state.isConnecting) {
                        Text("Restore Google Play purchases")
                    }
                    TextButton(onClick = { openUrl("https://securechat.stealthx.tech/#lifetime") }) {
                        Text("Website checkout")
                    }
                    if (state.isConnecting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
