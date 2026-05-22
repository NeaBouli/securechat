package com.stealthx.presentation.screens

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.data.repository.ContactRepository
import com.stealthx.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessageUi(
    val id: String,
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: Long,
    val deliveryStatus: String,
    val expiresAt: Long? = null
)

data class ChatUiState(
    val contactSxId: String,
    val displayName: String,
    val messages: List<ChatMessageUi> = emptyList(),
    val isSending: Boolean = false,
    val exportedMessage: String? = null,
    val errorMessage: String? = null,
    val safetyNumber: String = "",
    val disappearTimerMs: Long? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val contactExchangeManager: ContactExchangeManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val contactSxId: String = checkNotNull(savedStateHandle["sxId"])
    private val sending = MutableStateFlow(false)
    private val exportedMessage = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val displayName = MutableStateFlow(contactSxId)
    private val safetyNumber = MutableStateFlow("")
    private val disappearTimer = MutableStateFlow(messageRepository.getDisappearTimer(contactSxId))

    val uiState: StateFlow<ChatUiState> = combine(
        combine(
            messageRepository.observeMessages(contactSxId),
            sending,
            exportedMessage,
            error,
            displayName
        ) { messages, isSending, exportContent, errorMessage, name ->
            Quintet(messages, isSending, exportContent, errorMessage, name)
        },
        combine(safetyNumber, disappearTimer) { safetyNum, timerMs ->
            Pair(safetyNum, timerMs)
        }
    ) { quintet, (safetyNum, timerMs) ->
        ChatUiState(
            contactSxId = contactSxId,
            displayName = quintet.name,
            messages = quintet.messages.map { dm ->
                ChatMessageUi(
                    id = dm.id,
                    text = dm.text,
                    isOutgoing = dm.isOutgoing,
                    timestamp = dm.timestamp,
                    deliveryStatus = dm.deliveryStatus,
                    expiresAt = dm.expiresAt
                )
            },
            isSending = quintet.isSending,
            exportedMessage = quintet.exportContent,
            errorMessage = quintet.errorMessage,
            safetyNumber = safetyNum,
            disappearTimerMs = timerMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState(contactSxId = contactSxId, displayName = contactSxId)
    )

    init {
        viewModelScope.launch {
            messageRepository.markRead(contactSxId)
            contactExchangeManager.sendReadReceipt(contactSxId)
            displayName.value = contactRepository.getById(contactSxId)?.displayName ?: contactSxId
        }
        viewModelScope.launch {
            val myBundle = runCatching { StealthXIdentity.createPublicKeyBundle(context) }.getOrNull()
            val contact = contactRepository.getById(contactSxId)
            if (myBundle != null && contact != null) {
                safetyNumber.value = computeSafetyNumber(myBundle.ed25519PublicKey, contact.identityKey)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                messageRepository.deleteExpiredMessages()
            }
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

    fun setDisappearTimer(durationMs: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            messageRepository.setDisappearTimer(contactSxId, durationMs)
            disappearTimer.value = durationMs
            messageRepository.deleteExpiredMessages()
        }
    }

    private fun computeSafetyNumber(myKey: ByteArray, theirKey: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val cmp = myKey.zip(theirKey).map { (a, b) -> (a.toInt() and 0xFF).compareTo(b.toInt() and 0xFF) }.firstOrNull { it != 0 } ?: 0
        val combined = if (cmp <= 0) myKey + theirKey else theirKey + myKey
        val hash = digest.digest(combined)
        return hash.take(30).chunked(5) { chunk ->
            chunk.fold(0L) { acc, b -> acc * 256 + (b.toInt() and 0xFF) }
                .toString().padStart(5, '0').takeLast(5)
        }.chunked(3).joinToString("\n") { it.joinToString("  ") }
    }
}

/** Internal helper to carry 5 values through the nested combine. */
private data class Quintet(
    val messages: List<com.stealthx.data.repository.DecryptedMessage>,
    val isSending: Boolean,
    val exportContent: String?,
    val errorMessage: String?,
    val name: String
)
