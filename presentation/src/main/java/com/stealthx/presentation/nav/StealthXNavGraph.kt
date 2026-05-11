package com.stealthx.presentation.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.stealthx.features.broadcast.BroadcastLockedScreen
import com.stealthx.features.broadcast.BroadcastScreen
import com.stealthx.presentation.screens.*
import com.stealthx.shared.model.IfrTier

@Composable
fun StealthXNavGraph() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.Conversations.route) {
        composable(Screen.Conversations.route) {
            ConversationsScreen(
                onChatClick = { sxId -> navController.navigate("chat/$sxId") },
                onNewContact = { navController.navigate(Screen.NewContact.route) },
                onMyId = { navController.navigate(Screen.MyId.route) },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            route = Screen.Chat.ROUTE,
            arguments = listOf(navArgument("sxId") { type = NavType.StringType })
        ) { entry ->
            val sxId = entry.arguments?.getString("sxId") ?: return@composable
            ChatScreen(contactSxId = sxId, onBack = { navController.popBackStack() })
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
        composable(Screen.IFRUnlock.route) {
            IFRUnlockScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onIfrClick = { navController.navigate(Screen.IFRUnlock.route) }
            )
        }
        composable(Screen.Broadcast.route) {
            val vm: SettingsViewModel = hiltViewModel()
            val tier by vm.currentTier.collectAsState()
            if (tier >= IfrTier.ELITE) {
                BroadcastScreen(
                    onSend = { /* TODO: BroadcastManager.sendBroadcast(it) */ },
                    onBack = { navController.popBackStack() },
                    recipientCount = 0
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
