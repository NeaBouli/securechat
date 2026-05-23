package com.stealthx.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Signals MainActivity to write the pending bundle URI to the next NFC tag tapped. */
object NfcWriteRelay {
    private val _pendingUri = MutableStateFlow<String?>(null)
    val pendingUri: StateFlow<String?> = _pendingUri.asStateFlow()
    fun post(uri: String?) { _pendingUri.value = uri }
}
