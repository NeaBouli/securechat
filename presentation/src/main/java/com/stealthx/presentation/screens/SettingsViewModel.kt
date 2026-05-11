package com.stealthx.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.IfrTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    tierGate: TierGate,
    private val prefs: AppPreferences
) : ViewModel() {

    val currentTier: StateFlow<IfrTier> = tierGate.currentTier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IfrTier.FREE)

    private val _biometricEnabled = MutableStateFlow(prefs.biometricEnabled)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _stealthDeleteEnabled = MutableStateFlow(prefs.stealthDeleteEnabled)
    val stealthDeleteEnabled: StateFlow<Boolean> = _stealthDeleteEnabled.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        _biometricEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) { prefs.biometricEnabled = enabled }
    }

    fun setStealthDeleteEnabled(enabled: Boolean) {
        _stealthDeleteEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) { prefs.stealthDeleteEnabled = enabled }
    }
}
