package kz.kbtu.library.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kz.kbtu.library.model.Library
import kz.kbtu.library.ui.LibraryUiState
import kz.kbtu.library.ui.LibraryViewModel
import kz.kbtu.library.ui.screens.CatalogScreen
import kz.kbtu.library.ui.screens.ItemDetailScreen
import kz.kbtu.library.ui.screens.MembersScreen
import kz.kbtu.library.ui.screens.StatsScreen

// Which screen is shown for which route
@Composable
fun LibraryNavHost(
    navController: NavHostController,
    state: LibraryUiState,
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Catalog.route,
        modifier = modifier
    ) {
        composable(Screen.Catalog.route) {
            CatalogScreen(
                state = state,
                onQueryChange = viewModel::onQueryChange,
                onTypeFilterChange = viewModel::onTypeFilterChange,
                onOnlyAvailableChange = viewModel::onOnlyAvailableChange,
                onAddItem = viewModel::addItem,
                onBorrow = viewModel::borrow,
                onReturn = viewModel::returnItem,
                onItemClick = { itemId -> navController.navigate(Screen.ItemDetail.createRoute(itemId)) }
            )
        }

        composable(Screen.Members.route) {
            MembersScreen(
                state = state,
                onAddMember = viewModel::addMember,
                onReturn = viewModel::returnItem
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(stats = state.stats)
        }

        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(navArgument(Screen.ItemDetail.ARG_ITEM_ID) { type = NavType.IntType })
        ) { backStackEntry ->
            // Only the id is passed, the item is found in the shared state
            val itemId = backStackEntry.arguments?.getInt(Screen.ItemDetail.ARG_ITEM_ID)
            val item = state.items.find { it.id == itemId }
            ItemDetailScreen(
                item = item,
                borrower = item?.let { state.borrowerOf(it.id) },
                members = state.members,
                loanCountOf = { member -> state.itemsBorrowedBy(member.id).size },
                maxLoans = Library.MAX_LOANS,
                onBorrow = { memberId -> item?.let { viewModel.borrow(memberId, it.id) } },
                onReturn = { item?.let { viewModel.returnItem(it.id) } }
            )
        }
    }
}
