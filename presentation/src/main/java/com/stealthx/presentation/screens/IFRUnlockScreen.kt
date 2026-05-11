package com.stealthx.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stealthx.ifr.compose.IFRUnlockSheet
import com.stealthx.ifr.compose.TierStatusCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IFRUnlockScreen(
    onBack: () -> Unit,
    vm: IFRViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TierStatusCard(
                tier = state.tier,
                ifrBalance = state.lockedAmount,
                walletAddress = state.walletAddress,
                expiresIn = state.expiresIn,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            IFRUnlockSheet(
                onWalletConnectClicked = { vm.connectWallet() },
                onManualAddressSubmit = { vm.verifyManualAddress(it) },
                isVerifying = state.isVerifying,
                error = state.error
            )
        }
    }
}
