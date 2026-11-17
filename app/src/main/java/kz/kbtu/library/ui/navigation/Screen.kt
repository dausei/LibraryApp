package kz.kbtu.library.ui.navigation

// Addresses of the screens
sealed class Screen(val route: String) {
    data object Catalog : Screen("catalog")
    data object Members : Screen("members")
    data object Loans : Screen("loans")
    data object Stats : Screen("stats")

    // This route has an argument, for example item/3
    data object ItemDetail : Screen("item/{itemId}") {
        const val ARG_ITEM_ID = "itemId"
        fun createRoute(itemId: Int) = "item/$itemId"
    }

    // One form for both cases: item_form/-1 creates an item, item_form/3 edits item 3
    data object ItemForm : Screen("item_form/{itemId}") {
        const val ARG_ITEM_ID = "itemId"
        const val NEW = -1
        fun createRoute(itemId: Int = NEW) = "item_form/$itemId"
    }
}
