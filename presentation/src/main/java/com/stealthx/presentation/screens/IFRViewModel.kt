/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screens

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.ifr.activator.IFRTierActivator
import com.stealthx.ifr.wallet.WalletConnectManager
import com.stealthx.ifr.wallet.WalletConnectResult
import com.stealthx.shared.model.IfrTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IFRUiState(
    val tier: IfrTier = IfrTier.FREE,
    val lockedAmount: Long = 0,
    val walletAddress: String? = null,
    val expiresIn: String? = null,
    val isVerifying: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class IFRViewModel @Inject constructor(
    private val activator: IFRTierActivator,
    private val walletManager: WalletConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(IFRUiState())
    val uiState: StateFlow<IFRUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            walletManager.walletCallbacks.collect { result ->
                when (result) {
                    is WalletConnectResult.Success -> activateTier(result.walletAddress)
                    is WalletConnectResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    is WalletConnectResult.Cancelled -> _uiState.value = _uiState.value.copy(error = null)
                }
            }
        }
    }

    fun createWalletConnectIntent(): Intent? {
        return if (walletManager.isWalletAppInstalled()) {
            _uiState.value = _uiState.value.copy(
                error = "Wallet opened. Approve the connection in your wallet to verify IFR."
            )
            walletManager.createWalletOpenIntent()
        } else {
            _uiState.value = _uiState.value.copy(error = "No compatible wallet app found. Install MetaMask or another WalletConnect-compatible wallet.")
            null
        }
    }

    fun handleWalletConnectResult(resultCode: Int, data: Intent?) {
        when (val result = walletManager.processActivityResult(resultCode, data)) {
            is WalletConnectResult.Success -> activateTier(result.walletAddress)
            is WalletConnectResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            is WalletConnectResult.Cancelled -> _uiState.value = _uiState.value.copy(error = null)
        }
    }

    private fun activateTier(walletAddress: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifying = true, error = null)
            val result = activator.activate(walletAddress)
            _uiState.value = IFRUiState(
                tier = result.tier,
                lockedAmount = result.lockedAmount,
                walletAddress = result.walletAddress,
                expiresIn = if (result.fromCache) "From cache" else "30 days",
                isVerifying = false,
                error = result.error
            )
        }
    }
}
