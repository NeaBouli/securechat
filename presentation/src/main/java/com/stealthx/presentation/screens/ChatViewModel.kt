package com.stealthx.presentation.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessageUi(
    val id: String,
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: Long,
    val deliveryStatus: String
)

data class ChatUiState(
    val contactSxId: String,
    val messages: List<ChatMessageUi> = emptyList(),
    val isSending: Boolean = false,
    val exportedMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository
) : ViewModel() {
    private val contactSxId: String = checkNotNull(savedStateHandle["sxId"])
    private val sending = MutableStateFlow(false)
    private val exportedMessage = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChatUiState> = combine(
        messageRepository.observeMessages(contactSxId),
        sending,
        exportedMessage,
        error
    ) { messages, isSending, exportContent, errorMessage ->
        ChatUiState(
            contactSxId = contactSxId,
            messages = messages.map {
                ChatMessageUi(
                    id = it.id,
                    text = it.text,
                    isOutgoing = it.isOutgoing,
                    timestamp = it.timestamp,
                    deliveryStatus = it.deliveryStatus
                )
            },
            isSending = isSending,
            exportedMessage = exportContent,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState(contactSxId = contactSxId)
    )

    init {
        viewModelScope.launch {
            messageRepository.markRead(contactSxId)
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            sending.value = true
            error.value = null
            try {
                messageRepository.sendLocalMessage(contactSxId, trimmed)
                exportedMessage.value = messageRepository.exportLatestOutgoingMessage(contactSxId)
            } catch (e: Exception) {
                error.value = e.message ?: "Could not send message"
            } finally {
                sending.value = false
            }
        }
    }

    fun exportLatestMessage() {
        viewModelScope.launch {
            error.value = null
            exportedMessage.value = messageRepository.exportLatestOutgoingMessage(contactSxId)
            if (exportedMessage.value == null) {
                error.value = "No outgoing message to export"
            }
        }
    }

    fun clearExportedMessage() {
        exportedMessage.value = null
    }

    fun importMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            error.value = null
            try {
                messageRepository.importRatchetMessage(contactSxId, trimmed)
            } catch (e: Exception) {
                error.value = e.message ?: "Could not import message"
            }
        }
    }
}
