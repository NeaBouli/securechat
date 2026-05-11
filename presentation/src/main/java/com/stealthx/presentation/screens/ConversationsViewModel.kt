package com.stealthx.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.repository.ContactRepository
import com.stealthx.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ConversationUiState(
    val items: List<ConversationItem> = emptyList()
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversationsViewModel @Inject constructor(
    contactRepository: ContactRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {
    val uiState = contactRepository.observeAll()
        .flatMapLatest { contacts ->
            if (contacts.isEmpty()) {
                flowOf(ConversationUiState())
            } else {
                conversationState(contacts)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConversationUiState()
        )

    private fun conversationState(contacts: List<ContactKeyEntity>): Flow<ConversationUiState> =
        combine(
            flowOf(contacts),
            messageRepository.observeConversationSummaries(contacts)
        ) { contactList, summaries ->
            val summaryByContact = summaries.associateBy { it.contactId }
            ConversationUiState(
                items = contactList.map { contact ->
                    val summary = summaryByContact[contact.id]
                    ConversationItem(
                        sxId = contact.id,
                        displayName = contact.displayName.ifBlank { contact.id },
                        lastMessage = summary?.lastMessage ?: "No messages yet",
                        unreadCount = summary?.unreadCount ?: 0
                    )
                }
            )
        }
}
