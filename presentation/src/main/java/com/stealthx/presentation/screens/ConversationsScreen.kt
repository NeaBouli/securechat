package com.stealthx.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stealthx.presentation.theme.ScBg
import com.stealthx.presentation.theme.ScCyan
import com.stealthx.presentation.theme.ScGold
import com.stealthx.presentation.theme.ScGreen
import com.stealthx.presentation.theme.ScRed
import com.stealthx.presentation.theme.ScSurface3
import com.stealthx.presentation.theme.ScTextDim
import com.stealthx.presentation.theme.ScWineShimmer
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
    val unreadCount: Int = 0,
    val isPinned: Boolean = false
)

private fun recencyAlpha(timestamp: Long?): Float {
    if (timestamp == null) return 0.04f
    val ageMs = System.currentTimeMillis() - timestamp
    return when {
        ageMs < 3_600_000L       -> 0.30f  // < 1 hour
        ageMs < 86_400_000L      -> 0.20f  // < 1 day
        ageMs < 604_800_000L     -> 0.12f  // < 1 week
        else                     -> 0.05f
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationsScreen(
    state: ConversationUiState,
    onChatClick: (String) -> Unit,
    onNewContact: () -> Unit,
    onMyId: () -> Unit,
    onSettings: () -> Unit,
    onStealthDelete: () -> Unit,
    onDeleteContact: (String) -> Unit = {},
    onRenameContact: (String, String) -> Unit = { _, _ -> },
    onClearMessages: (String) -> Unit = {},
    onTogglePin: (String) -> Unit = {}
) {
    var logoTapCount by remember { mutableIntStateOf(0) }
    var contextTarget by remember { mutableStateOf<ConversationItem?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationItem?>(null) }
    var clearTarget by remember { mutableStateOf<ConversationItem?>(null) }
    var renameTarget by remember { mutableStateOf<ConversationItem?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(ScBg, ScWineShimmer, ScBg, ScWineShimmer.copy(alpha = 0.6f), ScBg),
        start = Offset(0f, 0f),
        end = Offset(1200f, 1800f)
    )

    // Rename dialog
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Name ändern") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Anzeigename") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameContact(target.sxId, renameInput.trim())
                        }
                        renameTarget = null
                    }
                ) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Abbrechen") }
            }
        )
    }

    // Clear chat confirmation
    clearTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { clearTarget = null },
            title = { Text("Chat leeren") },
            text = { Text("Alle Nachrichten mit ${target.displayName} löschen? Der Kontakt bleibt erhalten.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearMessages(target.sxId)
                    clearTarget = null
                }) { Text("Leeren", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { clearTarget = null }) { Text("Abbrechen") }
            }
        )
    }

    // Delete contact confirmation
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Kontakt löschen") },
            text = { Text("${target.displayName} und alle Nachrichten unwiderruflich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteContact(target.sxId)
                    deleteTarget = null
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Abbrechen") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BadgedBox(
                            badge = {
                                if (logoTapCount in 1..4) {
                                    Badge(containerColor = ScRed) {
                                        Text("${5 - logoTapCount}", color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Stealth Delete (tap 5×)",
                                tint = if (logoTapCount > 0) ScRed else ScGreen.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { logoTapCount++ }
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("SecureChat", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onMyId) { Text("ID", fontWeight = FontWeight.Bold) }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScBg,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewContact) {
                Icon(Icons.Default.Add, "New contact")
            }
        },
        containerColor = Color.Transparent
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shimmerBrush)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (state.wipeInProgress) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = ScRed
                        )
                    }
                }

                if (state.items.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(ScWineShimmer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock, null,
                                    modifier = Modifier.size(36.dp),
                                    tint = ScGreen.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                "No conversations yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Tap + to add your first secure contact",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScTextDim
                            )
                        }
                    }
                }

                items(state.items, key = { it.sxId }) { item ->
                    Box {
                        ConversationRow(
                            item = item,
                            onClick = { onChatClick(item.sxId) },
                            onLongClick = {
                                renameInput = item.displayName
                                contextTarget = item
                            }
                        )
                        DropdownMenu(
                            expanded = contextTarget?.sxId == item.sxId,
                            onDismissRequest = { contextTarget = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Umbenennen") },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                                onClick = {
                                    renameTarget = item
                                    contextTarget = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (item.isPinned) "Lospinnen" else "Anpinnen") },
                                leadingIcon = { Icon(Icons.Default.PushPin, null) },
                                onClick = {
                                    onTogglePin(item.sxId)
                                    contextTarget = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Chat leeren") },
                                leadingIcon = { Icon(Icons.Default.ChatBubbleOutline, null) },
                                onClick = {
                                    clearTarget = item
                                    contextTarget = null
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Löschen", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete, null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    deleteTarget = item
                                    contextTarget = null
                                }
                            )
                        }
                    }
                }

                // Stealth delete hint — always visible at bottom
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { logoTapCount++ }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = ScRed.copy(alpha = 0.55f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "STEALTH DELETE — tap 🔒 5×  (${logoTapCount}/5)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScRed.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(item: ConversationItem, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val avatarColors = remember(item.sxId) {
        val palette = listOf(
            0xFF1565C0L to 0xFF42A5F5L,
            0xFF2E7D32L to 0xFF66BB6AL,
            0xFF6A1B9AL to 0xFFAB47BCL,
            0xFF00695CL to 0xFF26A69AL,
            0xFF4E342EL to 0xFF8D6E63L,
            0xFF37474FL to 0xFF78909CL,
        )
        val idx = kotlin.math.abs(item.sxId.hashCode()) % palette.size
        palette[idx]
    }

    val greenAlpha = recencyAlpha(item.timestamp)
    val rowBg = when {
        item.isPinned -> Brush.linearGradient(
            listOf(ScGreen.copy(alpha = greenAlpha + 0.08f), ScGreen.copy(alpha = greenAlpha))
        )
        else -> Brush.linearGradient(
            listOf(ScGreen.copy(alpha = greenAlpha), ScGreen.copy(alpha = greenAlpha * 0.5f))
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(rowBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(modifier = Modifier.size(50.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(avatarColors.first), Color(avatarColors.second))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
            if (item.isPinned) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ScGold)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PushPin, null,
                        modifier = Modifier.size(10.dp),
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (item.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.timestamp?.let {
                    Text(
                        formatConversationTimestamp(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.unreadCount > 0) ScGreen else ScTextDim
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.unreadCount > 0)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    else ScTextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ScGreen)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            item.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
