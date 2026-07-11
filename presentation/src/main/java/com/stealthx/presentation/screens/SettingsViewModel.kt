package com.stealthx.presentation.screens

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.activation.ActivationCodeClient
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.domain.repository.AccessTierRepository
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.AccessTier
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    data class Success(val tier: AccessTier) : ActivationState()
    data class Error(val message: String) : ActivationState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tierGate: TierGate,
    private val tierRepository: AccessTierRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    val currentTier: StateFlow<AccessTier> = tierGate.currentTier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccessTier.FREE)

    private val _biometricEnabled = MutableStateFlow(prefs.biometricEnabled)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _stealthDeleteEnabled = MutableStateFlow(prefs.stealthDeleteEnabled)
    val stealthDeleteEnabled: StateFlow<Boolean> = _stealthDeleteEnabled.asStateFlow()

    private val _backgroundListenerEnabled = MutableStateFlow(prefs.backgroundListenerEnabled)
    val backgroundListenerEnabled: StateFlow<Boolean> = _backgroundListenerEnabled.asStateFlow()

    private val _activationState = MutableStateFlow<ActivationState>(ActivationState.Idle)
    val activationState: StateFlow<ActivationState> = _activationState.asStateFlow()

    private val _duressPin = MutableStateFlow(prefs.duressPin)
    val duressPin: StateFlow<String?> = _duressPin.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        _biometricEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) { prefs.biometricEnabled = enabled }
    }

    fun setStealthDeleteEnabled(enabled: Boolean) {
        _stealthDeleteEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) { prefs.stealthDeleteEnabled = enabled }
    }

    fun setBackgroundListenerEnabled(enabled: Boolean) {
        _backgroundListenerEnabled.value = enabled
        prefs.backgroundListenerEnabled = enabled
        val intent = Intent().setClassName(context.packageName, LISTENER_SERVICE_CLASS)
        runCatching {
            if (enabled) context.startForegroundService(intent) else context.stopService(intent)
        }
    }

    fun activateCode(code: String) {
        if (code.isBlank()) {
            _activationState.value = ActivationState.Error("Code cannot be empty")
            return
        }
        _activationState.value = ActivationState.Loading
        ActivationCodeClient.activate(context, code) { activation, error ->
            viewModelScope.launch(Dispatchers.IO) {
                if (activation != null) {
                    val accessTier = activation.tier
                    if (accessTier > AccessTier.FREE) {
                        prefs.entitlementToken = activation.entitlementToken
                        tierRepository.saveTierResult(
                            sourceId = "fiat_entitlement:${activation.productId}",
                            accessWeight = 0L,
                            tier = accessTier,
                            expiresAtEpochSeconds = activation.expiresAtEpochSeconds
                        )
                        tierGate.getTier()
                        _activationState.value = ActivationState.Success(accessTier)
                    } else {
                        _activationState.value = ActivationState.Error("Unknown tier received")
                    }
                } else {
                    val msg = when (error) {
                        "invalid_code" -> "Invalid or expired code"
                        "already_used" -> "Code already used"
                        "entitlement_missing" -> "Server entitlement is missing"
                        "entitlement_not_configured" -> "Secure purchase activation is not configured"
                        "entitlement_invalid" -> "Entitlement verification failed"
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

    fun setDuressPin(pin: String?) {
        prefs.duressPin = pin
        _duressPin.value = pin
    }

    private companion object {
        const val LISTENER_SERVICE_CLASS = "com.stealthx.securechat.service.MessageListenerService"
    }
}
