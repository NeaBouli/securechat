package com.stealthx.presentation.nav

sealed class Screen(val route: String) {
    data object Conversations : Screen("conversations")
    data class Chat(val sxId: String) : Screen("chat/$sxId") {
        companion object { const val ROUTE = "chat/{sxId}" }
    }
    data object MyId : Screen("my_id")
    object NewContact : Screen("new_contact") {
        const val DEEP_LINK_ROUTE = "new_contact?link={link}"
        const val ARG_LINK = "link"
        fun withLink(uri: String) = "new_contact?link=${java.net.URLEncoder.encode(uri, "UTF-8")}"
    }
    data object IFRUnlock : Screen("ifr_unlock")
    data object Settings : Screen("settings")
    data object Broadcast : Screen("broadcast")
    data object Setup : Screen("setup")
}
