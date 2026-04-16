package com.stealthx.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChatMessage(
    val id: String,
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactSxId: String,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showSafetyNumber by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            ChatMessage("1", "Hey, this is end-to-end encrypted.", false, 0L),
            ChatMessage("2", "Safety number verified.", true, 0L)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(contactSxId, fontWeight = FontWeight.SemiBold)
                        Text("End-to-end encrypted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSafetyNumber = true }) {
                        Icon(Icons.Default.Shield, "Safety number")
                    }
                }
            )
        },
        bottomBar = {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Encrypted message…") }
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            messages.add(ChatMessage(
                                id = System.currentTimeMillis().toString(),
                                text = input,
                                isOutgoing = true,
                                timestamp = System.currentTimeMillis()
                            ))
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, "Send",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
                Spacer(Modifier.height(4.dp))
            }
        }
    }

    if (showSafetyNumber) {
        AlertDialog(
            onDismissRequest = { showSafetyNumber = false },
            title = { Text("Safety Number") },
            text = {
                Text("12345 67890 12345 67890 12345 67890\n\nCompare with your contact to verify authenticity.",
                    style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = { showSafetyNumber = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (msg.isOutgoing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (msg.isOutgoing) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (msg.isOutgoing) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(msg.text, modifier = Modifier.padding(12.dp, 8.dp))
        }
    }
}
