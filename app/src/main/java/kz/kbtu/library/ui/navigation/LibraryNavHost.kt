package kz.kbtu.library.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kz.kbtu.library.ui.screens.CatalogScreen
import kz.kbtu.library.ui.screens.ItemDetailScreen
import kz.kbtu.library.ui.screens.ItemFormScreen
import kz.kbtu.library.ui.screens.LoansScreen
import kz.kbtu.library.ui.screens.MembersScreen
import kz.kbtu.library.ui.screens.StatsScreen
import kz.kbtu.library.ui.viewmodel.AppViewModelProvider
import kz.kbtu.library.ui.viewmodel.CatalogViewModel
import kz.kbtu.library.ui.viewmodel.ItemDetailViewModel
import kz.kbtu.library.ui.viewmodel.ItemFormViewModel
import kz.kbtu.library.ui.viewmodel.LoansViewModel
import kz.kbtu.library.ui.viewmodel.MembersViewModel
import kz.kbtu.library.ui.viewmodel.StatsViewModel

// Which screen is shown for which route. Every screen gets its own ViewModel, created for its
// back stack entry; the screens themselves only receive state and lambdas.
@Composable
fun LibraryNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    fun openItem(itemId: Int) = navController.navigate(Screen.ItemDetail.createRoute(itemId))

    NavHost(
        navController = navController,
        startDestination = Screen.Catalog.route,
        modifier = modifier
    ) {
        composable(Screen.Catalog.route) {
            val viewModel: CatalogViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CatalogScreen(
                state = state,
                onQueryChange = viewModel::onQueryChange,
                onTypeFilterChange = viewModel::onTypeFilterChange,
                onOnlyAvailableChange = viewModel::onOnlyAvailableChange,
                onAddItem = { navController.navigate(Screen.ItemForm.createRoute()) },
                onBorrow = viewModel::borrow,
                onReturn = viewModel::returnItem,
                onItemClick = ::openItem
            )
        }

        composable(Screen.Members.route) {
            val viewModel: MembersViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MembersScreen(
                state = state,
                onAddMember = viewModel::startAdding,
                onRenameMember = viewModel::startRenaming,
                onEditorNameChange = viewModel::onEditorNameChange,
                onSaveEditor = viewModel::saveEditor,
                onDismissEditor = viewModel::dismissEditor,
                onDeleteMember = viewModel::deleteMember,
                onReturn = viewModel::returnItem,
                onPayFines = viewModel::payFines
            )
        }

        composable(Screen.Loans.route) {
            val viewModel: LoansViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LoansScreen(
                state = state,
                onRenew = viewModel::renew,
                onReturn = viewModel::returnItem,
                onPickUp = viewModel::pickUp,
                onCancelReservation = viewModel::cancelReservation,
                onItemClick = ::openItem
            )
        }

        composable(Screen.Stats.route) {
            val viewModel: StatsViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val stats by viewModel.stats.collectAsStateWithLifecycle()
            StatsScreen(stats = stats)
        }

        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(navArgument(Screen.ItemDetail.ARG_ITEM_ID) { type = NavType.IntType })
        ) { backStackEntry ->
            val viewModel: ItemDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // The item was deleted: leave the screen
            LaunchedEffect(viewModel) {
                viewModel.closeEvents.collect { navController.popBackStack() }
            }

            ItemDetailScreen(
                state = state,
                onBorrow = viewModel::borrow,
                onReturn = viewModel::returnItem,
                onRenew = viewModel::renew,
                onReserve = viewModel::reserve,
                onCancelReservation = viewModel::cancelReservation,
                onEdit = {
                    val itemId = backStackEntry.arguments?.getInt(Screen.ItemDetail.ARG_ITEM_ID) ?: return@ItemDetailScreen
                    navController.navigate(Screen.ItemForm.createRoute(itemId))
                },
                onDelete = viewModel::deleteItem
            )
        }

        composable(
            route = Screen.ItemForm.route,
            arguments = listOf(navArgument(Screen.ItemForm.ARG_ITEM_ID) { type = NavType.IntType })
        ) {
            val viewModel: ItemFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Saved: go back to where the form was opened from
            LaunchedEffect(viewModel) {
                viewModel.closeEvents.collect { navController.popBackStack() }
            }

            ItemFormScreen(
                state = state,
                onTypeChange = viewModel::onTypeChange,
                onTitleChange = viewModel::onTitleChange,
                onYearChange = viewModel::onYearChange,
                onAuthorChange = viewModel::onAuthorChange,
                onPagesChange = viewModel::onPagesChange,
                onGenreChange = viewModel::onGenreChange,
                onIssueChange = viewModel::onIssueChange,
                onMinutesChange = viewModel::onMinutesChange,
                onSave = viewModel::save,
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
