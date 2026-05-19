package com.stealthx.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.activation.ActivationCodeClient
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.domain.repository.IfrTierRepository
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

sealed class ActivationState {
    data object Idle : ActivationState()
    data object Loading : ActivationState()
    data class Success(val tier: IfrTier) : ActivationState()
    data class Error(val message: String) : ActivationState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tierGate: TierGate,
    private val tierRepository: IfrTierRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    val currentTier: StateFlow<IfrTier> = tierGate.currentTier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IfrTier.FREE)

    private val _biometricEnabled = MutableStateFlow(prefs.biometricEnabled)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _stealthDeleteEnabled = MutableStateFlow(prefs.stealthDeleteEnabled)
    val stealthDeleteEnabled: StateFlow<Boolean> = _stealthDeleteEnabled.asStateFlow()

    private val _activationState = MutableStateFlow<ActivationState>(ActivationState.Idle)
    val activationState: StateFlow<ActivationState> = _activationState.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        _biometricEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) { prefs.biometricEnabled = enabled }
    }

    fun setStealthDeleteEnabled(enabled: Boolean) {
        _stealthDeleteEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) { prefs.stealthDeleteEnabled = enabled }
    }

    fun activateCode(code: String) {
        if (code.isBlank()) {
            _activationState.value = ActivationState.Error("Code cannot be empty")
            return
        }
        _activationState.value = ActivationState.Loading
        ActivationCodeClient.activate(code) { tierName, error ->
            viewModelScope.launch(Dispatchers.IO) {
                if (tierName != null) {
                    val ifrTier = try { IfrTier.valueOf(tierName) } catch (_: Exception) { null }
                    if (ifrTier != null && ifrTier > IfrTier.FREE) {
                        tierRepository.saveTierResult("activation_code", 0L, ifrTier)
                        tierGate.getTier()
                        _activationState.value = ActivationState.Success(ifrTier)
                    } else {
                        _activationState.value = ActivationState.Error("Unknown tier received")
                    }
                } else {
                    val msg = when (error) {
                        "invalid_code" -> "Invalid or expired code"
                        "already_used" -> "Code already used"
                        "network_error" -> "Connection failed — try again"
                        else -> error ?: "Unknown error"
                    }
                    _activationState.value = ActivationState.Error(msg)
                }
            }
        }
    }

    fun resetActivationState() {
        _activationState.value = ActivationState.Idle
    }
}
