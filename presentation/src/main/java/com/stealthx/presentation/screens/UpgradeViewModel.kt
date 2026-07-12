package com.stealthx.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.AccessTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpgradeState(
    val currentTier: AccessTier = AccessTier.FREE,
    val status: String = "Paid activation is launch-gated; check VLABS for availability."
)

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    private val tierGate: TierGate
) : ViewModel() {

    private val _state = MutableStateFlow(UpgradeState())
    val state: StateFlow<UpgradeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            tierGate.currentTier.collect { tier ->
                _state.update { it.copy(currentTier = tier) }
            }
        }
    }

    fun buy() {
        _state.update {
            it.copy(status = "Google Play purchases are disabled until server verification and refund revocation are available.")
        }
    }

    fun restorePurchases() {
        _state.update {
            it.copy(status = "Google Play restore is disabled until server verification is available.")
        }
    }

}
