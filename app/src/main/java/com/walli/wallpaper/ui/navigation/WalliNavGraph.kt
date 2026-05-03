package com.walli.wallpaper.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.walli.wallpaper.ui.components.WalliBottomBar
import com.walli.wallpaper.ui.screens.categories.CategoriesRoute
import com.walli.wallpaper.ui.screens.favorites.FavoritesRoute
import com.walli.wallpaper.ui.screens.home.HomeRoute
import com.walli.wallpaper.ui.screens.preview.PreviewRoute
import com.walli.wallpaper.ui.screens.settings.SettingsRoute
import com.walli.wallpaper.ui.screens.onboarding.OnboardingScreen
import com.walli.wallpaper.ui.screens.category_wallpapers.CategoryWallpapersRoute

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun WalliNavGraph(
    navController: NavHostController,
    startDestination: String = WalliRoute.Home.route,
    onOnboardingComplete: () -> Unit = {}
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute != null && bottomBarRoutes.any { 
        currentRoute.startsWith(it.route) 
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                WalliBottomBar(navController = navController, items = bottomBarRoutes)
            }
        },
    ) { _: PaddingValues ->
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                composable(WalliRoute.Onboarding.route) {
                    OnboardingScreen(
                        onFinish = {
                            onOnboardingComplete()
                            navController.navigate(WalliRoute.Home.route) {
                                popUpTo(WalliRoute.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(
                    route = WalliRoute.Home.route + "?categoryId={categoryId}",
                    arguments = listOf(
                        navArgument("categoryId") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getInt("categoryId")?.takeIf { it != -1 }
                    HomeRoute(
                        initialCategoryId = categoryId,
                        onOpenPreview = { navController.navigate(WalliRoute.Preview.route) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable(WalliRoute.Categories.route) {
                    CategoriesRoute(
                        onCategoryClick = { category ->
                            navController.navigate(WalliRoute.CategoryWallpapers.route + "/${category.id}/${category.name}")
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable(
                    route = WalliRoute.CategoryWallpapers.route + "/{categoryId}/{categoryName}",
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.IntType },
                        navArgument("categoryName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0
                    val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                    CategoryWallpapersRoute(
                        categoryId = categoryId,
                        categoryName = categoryName,
                        onBack = { navController.popBackStack() },
                        onOpenPreview = { navController.navigate(WalliRoute.Preview.route) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable(WalliRoute.Favorites.route) {
                    FavoritesRoute(
                        onOpenPreview = { navController.navigate(WalliRoute.Preview.route) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable(WalliRoute.Settings.route) {
                    SettingsRoute(onBack = { navController.popBackStack() })
                }
                composable(WalliRoute.Preview.route) {
                    PreviewRoute(
                        onBack = { navController.popBackStack() },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
            }
        }
    }
}
