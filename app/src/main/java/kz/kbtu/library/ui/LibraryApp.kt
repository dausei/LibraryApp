package kz.kbtu.library.ui

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kz.kbtu.library.LibraryApplication
import kz.kbtu.library.R
import kz.kbtu.library.ui.navigation.LibraryNavHost
import kz.kbtu.library.ui.navigation.Screen

// Screens shown in the bottom bar
private enum class TopLevelTab(val screen: Screen, val label: String, @DrawableRes val icon: Int) {
    CATALOG(Screen.Catalog, "Catalog", R.drawable.ic_library),
    MEMBERS(Screen.Members, "Members", R.drawable.ic_people),
    LOANS(Screen.Loans, "Loans", R.drawable.ic_loans),
    STATS(Screen.Stats, "Stats", R.drawable.ic_stats)
}

private fun titleFor(route: String?, arguments: Bundle?): String = when (route) {
    Screen.Members.route -> "Members"
    Screen.Loans.route -> "Loans"
    Screen.Stats.route -> "Statistics"
    Screen.ItemDetail.route -> "Item details"
    Screen.ItemForm.route ->
        if (arguments?.getInt(Screen.ItemForm.ARG_ITEM_ID, Screen.ItemForm.NEW) == Screen.ItemForm.NEW) "Add item"
        else "Edit item"
    else -> "Library Manager"
}

// Top bar, bottom bar, Snackbar and the navigation host
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryApp() {
    val snackbarHostState = remember { SnackbarHostState() }

    // ViewModels send short messages to the Messenger; they are shown one after another
    val messenger = (LocalContext.current.applicationContext as LibraryApplication).container.messenger
    LaunchedEffect(messenger) {
        // A newer message replaces the one on screen
        messenger.messages.collectLatest { text -> snackbarHostState.showSnackbar(text) }
    }

    // Keeps the back stack
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = TopLevelTab.entries.find { it.screen.route == currentRoute }
    val isTopLevel = currentTab != null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(titleFor(currentRoute, backStackEntry?.arguments)) },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = {
                                navController.navigate(tab.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                LibraryNavHost(navController = navController)
            }
        }
    }
}
