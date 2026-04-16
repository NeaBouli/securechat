package com.stealthx.presentation.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.stealthx.presentation.screens.*

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
            NewContactScreen(onBack = { navController.popBackStack() }, onContactAdded = { navController.popBackStack() })
        }
        composable(Screen.IFRUnlock.route) {
            IFRUnlockScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() }, onIfrClick = { navController.navigate(Screen.IFRUnlock.route) })
        }
    }
}
