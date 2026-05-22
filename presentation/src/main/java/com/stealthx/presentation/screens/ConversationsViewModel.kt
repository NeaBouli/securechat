package com.stealthx.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.data.repository.ContactRepository
import com.stealthx.data.repository.MessageRepository
import com.stealthx.data.security.WipeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationUiState(
    val items: List<ConversationItem> = emptyList(),
    val wipeInProgress: Boolean = false,
    val wipeCompleted: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val appPreferences: AppPreferences,
    private val wipeManager: WipeManager,
    private val contactExchangeManager: ContactExchangeManager
) : ViewModel() {
    private val wipeState = MutableStateFlow(WipeState())
    private val pinnedIds = MutableStateFlow(appPreferences.pinnedContactIds)

    init {
        contactExchangeManager.startListening()
    }

    val uiState: StateFlow<ConversationUiState> = combine(
        contactRepository.observeAll()
            .flatMapLatest { contacts ->
                if (contacts.isEmpty()) flowOf(emptyList())
                else conversationItems(contacts)
            },
        wipeState,
        pinnedIds
    ) { items, wipe, pinned ->
        val sorted = items
            .map { it.copy(isPinned = it.sxId in pinned) }
            .sortedWith(
                compareByDescending<ConversationItem> { it.isPinned }
                    .thenByDescending { it.timestamp ?: Long.MIN_VALUE }
                    .thenBy { it.displayName.lowercase() }
            )
        ConversationUiState(
            items = sorted,
            wipeInProgress = wipe.inProgress,
            wipeCompleted = wipe.completed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConversationUiState()
    )

    fun deleteContact(sxId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.deleteById(sxId)
        }
    }

    fun renameContact(sxId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.renameContact(sxId, newName)
        }
    }

    fun clearMessages(sxId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            messageRepository.clearMessages(sxId)
        }
    }

    fun togglePin(sxId: String) {
        val current = pinnedIds.value.toMutableSet()
        if (sxId in current) current.remove(sxId) else current.add(sxId)
        appPreferences.pinnedContactIds = current
        pinnedIds.value = current
    }

    fun triggerStealthDelete() {
        if (!appPreferences.stealthDeleteEnabled || wipeState.value.inProgress) return
        viewModelScope.launch(Dispatchers.IO) {
            wipeState.value = WipeState(inProgress = true)
            wipeManager.wipeAll()
            wipeState.value = WipeState(completed = true)
        }
    }

    private fun conversationItems(contacts: List<ContactKeyEntity>): Flow<List<ConversationItem>> =
        combine(
            flowOf(contacts),
            messageRepository.observeConversationSummaries(contacts)
        ) { contactList, summaries ->
            val summaryByContact = summaries.associateBy { it.contactId }
            contactList.map { contact ->
                val summary = summaryByContact[contact.id]
                ConversationItem(
                    sxId = contact.id,
                    displayName = contact.displayName.ifBlank { contact.id },
                    lastMessage = summary?.lastMessage ?: "No messages yet",
                    timestamp = summary?.timestamp,
                    unreadCount = summary?.unreadCount ?: 0
                )
            }
        }

    private data class WipeState(
        val inProgress: Boolean = false,
        val completed: Boolean = false
    )
}
