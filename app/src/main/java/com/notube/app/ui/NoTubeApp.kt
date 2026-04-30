package com.notube.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notube.app.ui.screens.HomeScreen
import com.notube.app.ui.screens.PlayerScreen
import com.notube.app.ui.screens.SearchScreen
import com.notube.app.ui.theme.YouTubeRed

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val PLAYER = "player/{videoId}"
    fun player(videoId: String) = "player/$videoId"
}

@Composable
fun NoTubeApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute == Routes.HOME || currentRoute == Routes.LIBRARY

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (showBottomBar) BottomBar(nav, currentRoute) },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onSearchClick = { nav.navigate(Routes.SEARCH) },
                    onVideoClick = { id -> nav.navigate(Routes.player(id)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onVideoClick = { id -> nav.navigate(Routes.player(id)) },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryStub()
            }
            composable(Routes.PLAYER) { entry ->
                val id = entry.arguments?.getString("videoId").orEmpty()
                PlayerScreen(videoId = id, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomBar(nav: androidx.navigation.NavHostController, currentRoute: String?) {
    val items = listOf(
        BottomItem(Routes.HOME, "Home", Icons.Outlined.Home),
        BottomItem(Routes.LIBRARY, "Library", Icons.Outlined.VideoLibrary),
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            val selected = currentRoute == item.route ||
                (nav.currentDestination?.hierarchy?.any { it.route == item.route } == true)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        nav.navigate(item.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, null) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = YouTubeRed,
                    selectedTextColor = YouTubeRed,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun LibraryStub() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(
            "Your library is empty.\nWatch something on the home tab.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
