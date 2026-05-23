package com.stealthx.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class NfcWriteState {
    object Idle : NfcWriteState()
    data class Pending(val uri: String) : NfcWriteState()
    object Success : NfcWriteState()
    data class Failure(val reason: String) : NfcWriteState()
}

object NfcWriteRelay {
    private val _state = MutableStateFlow<NfcWriteState>(NfcWriteState.Idle)
    val state: StateFlow<NfcWriteState> = _state.asStateFlow()

    val pendingUri: String? get() = (_state.value as? NfcWriteState.Pending)?.uri

    fun post(uri: String?) {
        _state.value = if (uri != null) NfcWriteState.Pending(uri) else NfcWriteState.Idle
    }

    fun reportSuccess() { _state.value = NfcWriteState.Success }
    fun reportFailure(reason: String) { _state.value = NfcWriteState.Failure(reason) }
    fun reset() { _state.value = NfcWriteState.Idle }
}
