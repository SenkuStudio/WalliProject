package com.walli.wallpaper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.graphics.vector.ImageVector

sealed class WalliRoute(
    val route: String,
    val label: String? = null,
    val icon: ImageVector? = null,
) {
    data object Home : WalliRoute("home", "Home", Icons.Rounded.Home)
    data object Categories : WalliRoute("categories", "Categories", Icons.Rounded.Collections)
    data object Favorites : WalliRoute("favorites", "Favorites", Icons.Rounded.Favorite)
    data object About : WalliRoute("about", "About", Icons.Rounded.Info)
    data object Preview : WalliRoute("preview")
    data object Search : WalliRoute("search")
    data object Onboarding : WalliRoute("onboarding")
}

val bottomBarRoutes = listOf(
    WalliRoute.Home,
    WalliRoute.Categories,
    WalliRoute.Favorites,
    WalliRoute.About,
)
