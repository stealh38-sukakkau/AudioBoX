package com.audiobox.player.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Rounded.Home)
    data object Library : Screen("library", "Library", Icons.Rounded.LibraryMusic)
    data object Search : Screen("search", "Search", Icons.Rounded.Search)
    data object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
}
