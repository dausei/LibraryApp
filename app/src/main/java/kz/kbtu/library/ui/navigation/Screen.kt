package kz.kbtu.library.ui.navigation

// Addresses of the screens
sealed class Screen(val route: String) {
    data object Catalog : Screen("catalog")
    data object Members : Screen("members")
    data object Stats : Screen("stats")

    // This route has an argument, for example item/3
    data object ItemDetail : Screen("item/{itemId}") {
        const val ARG_ITEM_ID = "itemId"
        fun createRoute(itemId: Int) = "item/$itemId"
    }
}
