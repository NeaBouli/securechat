/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.identity.PublicKeyBundleQr
import com.stealthx.data.repository.ContactRepository
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.AccessTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactLimitState(
    val count: Int = 0,
    val limit: Int = ContactRepository.FREE_CONTACT_LIMIT,
    val isAtLimit: Boolean = false,
    val isLimitEnforced: Boolean = false
)

data class NewContactUiState(
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val contactAdded: Boolean = false
)

@HiltViewModel
class NewContactViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val tierGate: TierGate,
    private val contactExchangeManager: ContactExchangeManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(NewContactUiState())
    val uiState: StateFlow<NewContactUiState> = _uiState.asStateFlow()

    val limitState: StateFlow<ContactLimitState> = combine(
        tierGate.currentTier,
        contactRepository.observeAll()
    ) { tier, contacts ->
        val enforced = tier < AccessTier.PRO
        ContactLimitState(
            count = contacts.size,
            limit = ContactRepository.FREE_CONTACT_LIMIT,
            isAtLimit = enforced && contacts.size >= ContactRepository.FREE_CONTACT_LIMIT,
            isLimitEnforced = enforced
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactLimitState())

    fun addFromQrContent(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            _uiState.value = NewContactUiState(errorMessage = "QR content is required")
            return
        }

        val bundle = PublicKeyBundleQr.fromQrContent(trimmed).getOrElse {
            _uiState.value = NewContactUiState(errorMessage = it.message ?: "Invalid StealthX QR content")
            return
        }

        viewModelScope.launch {
            _uiState.value = NewContactUiState(isSaving = true)
            try {
                contactRepository.addContactBundle(bundle)
                contactExchangeManager.sendExchange(bundle.sxId)
                _uiState.value = NewContactUiState(
                    statusMessage = "Contact added",
                    contactAdded = true
                )
            } catch (e: Exception) {
                _uiState.value = NewContactUiState(errorMessage = e.message ?: "Could not add contact")
            }
        }
    }

    fun consumeContactAdded() {
        if (_uiState.value.contactAdded) {
            _uiState.value = _uiState.value.copy(contactAdded = false)
        }
    }
}
