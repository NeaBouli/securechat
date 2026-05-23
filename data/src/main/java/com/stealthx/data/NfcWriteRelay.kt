package com.stealthx.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class NfcWriteState {
    object Idle : NfcWriteState()
    data class Pending(val uri: String) : NfcWriteState()
    object Success : NfcWriteState()
    data class Failure(val uri: String, val reason: String) : NfcWriteState()
}

object NfcWriteRelay {
    private val _state = MutableStateFlow<NfcWriteState>(NfcWriteState.Idle)
    val state: StateFlow<NfcWriteState> = _state.asStateFlow()

    /** Non-null while a write attempt should be made on the next NFC tap (Pending or retryable Failure). */
    val pendingUri: String? get() = when (val s = _state.value) {
        is NfcWriteState.Pending -> s.uri
        is NfcWriteState.Failure -> s.uri
        else -> null
    }

    fun post(uri: String?) {
        _state.value = if (uri != null) NfcWriteState.Pending(uri) else NfcWriteState.Idle
    }

    fun reportSuccess() { _state.value = NfcWriteState.Success }
    fun reportFailure(uri: String, reason: String) { _state.value = NfcWriteState.Failure(uri, reason) }
    fun reset() { _state.value = NfcWriteState.Idle }
}
