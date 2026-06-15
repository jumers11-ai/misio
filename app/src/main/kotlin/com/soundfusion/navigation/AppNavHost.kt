package com.soundfusion.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.soundfusion.feature.home.HomeScreen
import com.soundfusion.feature.search.SearchScreen
import com.soundfusion.feature.library.LibraryScreen
import com.soundfusion.feature.player.MiniPlayer
import com.soundfusion.feature.player.PlayerScreen
import com.soundfusion.feature.settings.SettingsScreen
import com.soundfusion.feature.downloads.DownloadsScreen
import kotlinx.serialization.Serializable

@Serializable sealed interface Route {
    @Serializable data object Home : Route
    @Serializable data object Search : Route
    @Serializable data object Library : Route
    @Serializable data object Downloads : Route
    @Serializable data object Settings : Route
    @Serializable data class Player(val trackId: String) : Route
    @Serializable data class Album(val albumId: String) : Route
    @Serializable data class Artist(val artistId: String) : Route
    @Serializable data class Playlist(val playlistId: String) : Route
}

private val bottomNavItems = listOf(
    BottomNavItem(Route.Home, "Home", "home"),
    BottomNavItem(Route.Search, "Search", "search"),
    BottomNavItem(Route.Library, "Library", "library"),
    BottomNavItem(Route.Downloads, "Downloads", "downloads"),
    BottomNavItem(Route.Settings, "Settings", "settings"),
)

data class BottomNavItem(val route: Route, val label: String, val icon: String)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any {
                            it.route == item.route::class.qualifiedName
                        } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(Route.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(item.label) },
                        icon = { /* TODO: Material icons */ }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = Modifier.padding(padding),
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            composable<Route.Home> {
                HomeScreen(
                    onNavigateToPlayer = { trackId -> navController.navigate(Route.Player(trackId)) },
                    onNavigateToAlbum = { albumId -> navController.navigate(Route.Album(albumId)) },
                )
            }
            composable<Route.Search> {
                SearchScreen(
                    onNavigateToPlayer = { trackId -> navController.navigate(Route.Player(trackId)) },
                )
            }
            composable<Route.Library> {
                LibraryScreen(
                    onNavigateToPlaylist = { id -> navController.navigate(Route.Playlist(id)) },
                    onNavigateToAlbum = { id -> navController.navigate(Route.Album(id)) },
                )
            }
            composable<Route.Downloads> { DownloadsScreen() }
            composable<Route.Settings> { SettingsScreen() }
            composable<Route.Player> { PlayerScreen() }
        }

        MiniPlayer(
            onExpand = { /* navigate to full player */ },
            modifier = Modifier.padding(padding),
        )
    }
}
