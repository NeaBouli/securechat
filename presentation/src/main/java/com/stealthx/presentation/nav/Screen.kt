package com.stealthx.presentation.nav

sealed class Screen(val route: String) {
    data object Conversations : Screen("conversations")
    data class Chat(val sxId: String) : Screen("chat/$sxId") {
        companion object { const val ROUTE = "chat/{sxId}" }
    }
    data object MyId : Screen("my_id")
    object NewContact : Screen("new_contact") {
        const val DEEP_LINK_ROUTE = "new_contact?link={link}&source={source}"
        const val ARG_LINK = "link"
        const val ARG_SOURCE = "source"

        /** Set only by the local router: NFC tag read in the app vs. external deep link. */
        const val SOURCE_NFC = "nfc"
        const val SOURCE_LINK = "link"
        fun withLink(uri: String, source: String) =
            "new_contact?link=${java.net.URLEncoder.encode(uri, "UTF-8")}&source=$source"
    }
    data object Upgrade : Screen("upgrade")
    data object Settings : Screen("settings")
    data object Broadcast : Screen("broadcast")
    data object Setup : Screen("setup")
}
