package com.stealthx.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NfcUriRelay {
    private val _uri = MutableStateFlow<String?>(null)
    val uri: StateFlow<String?> = _uri.asStateFlow()
    fun post(uri: String?) { _uri.value = uri }
    fun consume() { _uri.value = null }
}
