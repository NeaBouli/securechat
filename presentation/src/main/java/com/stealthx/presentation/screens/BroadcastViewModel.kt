/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.repository.ContactRepository
import com.stealthx.features.broadcast.BroadcastManager
import com.stealthx.features.broadcast.BroadcastResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BroadcastUiState(
    val recipientCount: Int = 0,
    val isSending: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val broadcastManager: BroadcastManager,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BroadcastUiState())
    val uiState: StateFlow<BroadcastUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(recipientCount = contactRepository.count())
        }
    }

    fun send(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, statusMessage = null)
            val result = broadcastManager.sendBroadcast(message)
            _uiState.value = _uiState.value.copy(
                isSending = false,
                statusMessage = result.toUserMessage()
            )
        }
    }

    private fun BroadcastResult.toUserMessage(): String = when (this) {
        is BroadcastResult.Success -> "Broadcast queued for $sentTo contacts"
        is BroadcastResult.PartialSuccess -> "Broadcast queued for $sent contacts, $failed failed"
        is BroadcastResult.Failure -> reason
    }
}
