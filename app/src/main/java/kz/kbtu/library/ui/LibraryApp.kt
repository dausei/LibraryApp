package kz.kbtu.library.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kz.kbtu.library.R
import kz.kbtu.library.ui.navigation.LibraryNavHost
import kz.kbtu.library.ui.navigation.Screen

// Screens shown in the bottom bar
private enum class TopLevelTab(val screen: Screen, val label: String, @DrawableRes val icon: Int) {
    CATALOG(Screen.Catalog, "Catalog", R.drawable.ic_library),
    MEMBERS(Screen.Members, "Members", R.drawable.ic_people),
    STATS(Screen.Stats, "Stats", R.drawable.ic_stats)
}

private fun titleFor(route: String?): String = when (route) {
    Screen.Members.route -> "Members"
    Screen.Stats.route -> "Statistics"
    Screen.ItemDetail.route -> "Item details"
    else -> "Library Manager"
}

// Top bar, bottom bar, Snackbar and the navigation host
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryApp(viewModel: LibraryViewModel = viewModel()) {
    // Redraws the UI whenever the state changes
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Shows the message once, then tells the ViewModel
    LaunchedEffect(state.message) {
        state.message?.let { text ->
            snackbarHostState.showSnackbar(text)
            viewModel.onMessageShown()
        }
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
                title = { Text(titleFor(currentRoute)) },
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
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                LibraryNavHost(
                    navController = navController,
                    state = state,
                    viewModel = viewModel
                )
                if (state.isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
