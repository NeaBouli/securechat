/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.repository.ContactRepository
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.IfrTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ContactLimitState(
    val count: Int = 0,
    val limit: Int = ContactRepository.FREE_CONTACT_LIMIT,
    val isAtLimit: Boolean = false,
    val isLimitEnforced: Boolean = false
)

@HiltViewModel
class NewContactViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val tierGate: TierGate
) : ViewModel() {

    val limitState: StateFlow<ContactLimitState> = combine(
        tierGate.currentTier,
        contactRepository.observeAll()
    ) { tier, contacts ->
        val enforced = tier < IfrTier.PRO
        ContactLimitState(
            count = contacts.size,
            limit = ContactRepository.FREE_CONTACT_LIMIT,
            isAtLimit = enforced && contacts.size >= ContactRepository.FREE_CONTACT_LIMIT,
            isLimitEnforced = enforced
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactLimitState())
}
