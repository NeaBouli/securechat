/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screens

import androidx.lifecycle.ViewModel
import com.stealthx.shared.model.IfrTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class IFRViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(IFRUiState())
    val uiState: StateFlow<IFRUiState> = _uiState.asStateFlow()
}
