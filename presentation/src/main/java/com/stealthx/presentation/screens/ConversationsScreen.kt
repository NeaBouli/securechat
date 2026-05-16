package com.stealthx.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ConversationItem(
    val sxId: String,
    val displayName: String,
    val lastMessage: String,
    val timestamp: Long? = null,
    val unreadCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    state: ConversationUiState,
    onChatClick: (String) -> Unit,
    onNewContact: () -> Unit,
    onMyId: () -> Unit,
    onSettings: () -> Unit,
    onStealthDelete: () -> Unit
) {
    var logoTapCount by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null,
                            modifier = Modifier.clickable { logoTapCount++ })
                        Spacer(Modifier.width(8.dp))
                        Text("SecureChat", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onMyId) { Text("ID", fontWeight = FontWeight.Bold) }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewContact) {
                Icon(Icons.Default.Add, "New contact")
            }
        }
    ) { padding ->
        LaunchedEffect(logoTapCount) {
            if (logoTapCount >= 5) {
                onStealthDelete()
                logoTapCount = 0
            } else if (logoTapCount > 0) {
                delay(3_000)
                logoTapCount = 0
            }
        }

        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            if (state.wipeInProgress) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            if (state.items.isEmpty()) {
                item {
                    Text(
                        "Noch keine Gespräche",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            items(state.items) { item ->
                ConversationRow(item = item, onClick = { onChatClick(item.sxId) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ConversationRow(item: ConversationItem, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Lock, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.displayName,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                item.timestamp?.let {
                    Text(
                        formatConversationTimestamp(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
            Text(item.lastMessage, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        if (item.unreadCount > 0) {
            Badge { Text(item.unreadCount.toString()) }
        }
    }
}

private fun formatConversationTimestamp(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(zone)
    val today = LocalDate.now(zone)
    val formatter = if (dateTime.toLocalDate() == today) {
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    } else {
        DateTimeFormatter.ofPattern("dd.MM.yy", Locale.getDefault())
    }
    return dateTime.format(formatter)
}
