package com.walli.wallpaper.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.walli.wallpaper.ui.components.WalliBottomBar
import com.walli.wallpaper.ui.screens.categories.CategoriesRoute
import com.walli.wallpaper.ui.screens.favorites.FavoritesRoute
import com.walli.wallpaper.ui.screens.home.HomeRoute
import com.walli.wallpaper.ui.screens.preview.PreviewRoute
import com.walli.wallpaper.ui.screens.about.AboutRoute

@Composable
fun WalliNavGraph(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in bottomBarRoutes.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                WalliBottomBar(navController = navController, items = bottomBarRoutes)
            }
        },
    ) { _: PaddingValues ->
        NavHost(
            navController = navController,
            startDestination = WalliRoute.Home.route,
        ) {
            composable(WalliRoute.Home.route) {
                HomeRoute(onOpenPreview = { navController.navigate(WalliRoute.Preview.route) })
            }
            composable(WalliRoute.Categories.route) {
                CategoriesRoute()
            }
            composable(WalliRoute.Favorites.route) {
                FavoritesRoute(onOpenPreview = { navController.navigate(WalliRoute.Preview.route) })
            }
            composable(WalliRoute.About.route) {
                AboutRoute(onBack = { navController.popBackStack() })
            }
            composable(WalliRoute.Preview.route) {
                PreviewRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
