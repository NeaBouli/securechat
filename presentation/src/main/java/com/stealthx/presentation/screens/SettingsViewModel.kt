package com.stealthx.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.IfrTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    tierGate: TierGate
) : ViewModel() {
    val currentTier: StateFlow<IfrTier> = tierGate.currentTier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IfrTier.FREE)
}
