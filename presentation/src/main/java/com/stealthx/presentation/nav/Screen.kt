package com.stealthx.presentation.nav

sealed class Screen(val route: String) {
    data object Conversations : Screen("conversations")
    data class Chat(val sxId: String) : Screen("chat/$sxId") {
        companion object { const val ROUTE = "chat/{sxId}" }
    }
    data object MyId : Screen("my_id")
    data object NewContact : Screen("new_contact")
    data object IFRUnlock : Screen("ifr_unlock")
    data object Settings : Screen("settings")
}
