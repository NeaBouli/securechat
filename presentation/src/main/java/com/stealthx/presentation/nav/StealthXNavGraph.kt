package com.stealthx.presentation.nav

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stealthx.features.broadcast.BroadcastLockedScreen
import com.stealthx.features.broadcast.BroadcastScreen
import com.stealthx.presentation.screens.*
import com.stealthx.shared.model.IfrTier

@Composable
fun StealthXNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val pendingDeepLink = remember {
        (context as? Activity)?.intent?.data?.toString()
            ?.takeIf { it.startsWith("stealthx://add/") }
    }
    LaunchedEffect(pendingDeepLink) {
        if (pendingDeepLink != null) {
            navController.navigate(Screen.NewContact.withLink(pendingDeepLink))
            (context as? Activity)?.intent?.data = null
        }
    }
    NavHost(navController, startDestination = Screen.Conversations.route) {
        composable(Screen.Conversations.route) {
            val conversationsVm: ConversationsViewModel = hiltViewModel()
            val state by conversationsVm.uiState.collectAsState()
            val activity = LocalContext.current as? Activity
            LaunchedEffect(state.wipeCompleted) {
                if (state.wipeCompleted) {
                    activity?.finishAffinity()
                    kotlin.system.exitProcess(0)
                }
            }
            ConversationsScreen(
                state = state,
                onChatClick = { sxId -> navController.navigate("chat/$sxId") },
                onNewContact = { navController.navigate(Screen.NewContact.route) },
                onMyId = { navController.navigate(Screen.MyId.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onStealthDelete = conversationsVm::triggerStealthDelete
            )
        }
        composable(
            route = Screen.Chat.ROUTE,
            arguments = listOf(navArgument("sxId") { type = NavType.StringType })
        ) { entry ->
            val sxId = entry.arguments?.getString("sxId") ?: return@composable
            val chatVm: ChatViewModel = hiltViewModel()
            val state by chatVm.uiState.collectAsState()
            ChatScreen(
                state = state,
                onSend = chatVm::send,
                onExportLatest = chatVm::exportLatestMessage,
                onClearExport = chatVm::clearExportedMessage,
                onImport = chatVm::importMessage,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MyId.route) {
            MyIdScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.NewContact.route) {
            NewContactScreen(
                onBack = { navController.popBackStack() },
                onContactAdded = { navController.popBackStack() },
                onUpgrade = { navController.navigate(Screen.IFRUnlock.route) }
            )
        }
        composable(
            route = Screen.NewContact.DEEP_LINK_ROUTE,
            arguments = listOf(navArgument(Screen.NewContact.ARG_LINK) {
                type = NavType.StringType; defaultValue = ""
            })
        ) { entry ->
            val rawLink = entry.arguments?.getString(Screen.NewContact.ARG_LINK) ?: ""
            val decoded = java.net.URLDecoder.decode(rawLink, "UTF-8")
            NewContactScreen(
                initialContent = decoded,
                onBack = { navController.popBackStack() },
                onContactAdded = { navController.popBackStack() },
                onUpgrade = { navController.navigate(Screen.IFRUnlock.route) }
            )
        }
        composable(Screen.IFRUnlock.route) {
            IFRUnlockScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Setup.route) {
            SetupScreen(onContinue = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onIfrClick = { navController.navigate(Screen.IFRUnlock.route) },
                onBroadcastClick = { navController.navigate(Screen.Broadcast.route) },
                onSetupClick = { navController.navigate(Screen.Setup.route) }
            )
        }
        composable(Screen.Broadcast.route) {
            val vm: SettingsViewModel = hiltViewModel()
            val tier by vm.currentTier.collectAsState()
            if (tier >= IfrTier.ELITE) {
                val broadcastVm: BroadcastViewModel = hiltViewModel()
                val state by broadcastVm.uiState.collectAsState()
                BroadcastScreen(
                    onSend = broadcastVm::send,
                    onBack = { navController.popBackStack() },
                    recipientCount = state.recipientCount,
                    isSending = state.isSending,
                    statusMessage = state.statusMessage
                )
            } else {
                BroadcastLockedScreen(
                    onUnlock = { navController.navigate(Screen.IFRUnlock.route) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
