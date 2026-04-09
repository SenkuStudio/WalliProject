package com.walli.wallpaper.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.walli.wallpaper.ui.navigation.WalliRoute

@Composable
fun WalliBottomBar(
    navController: NavHostController,
    items: List<WalliRoute>,
) {
    val currentBackStack = navController.currentBackStackEntryAsState().value
    val currentDestination = currentBackStack?.destination

    NavigationBar {
        items.forEach { route ->
            val selected = currentDestination?.hierarchy?.any { it.route == route.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(route.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = route.icon as ImageVector,
                        contentDescription = route.label,
                    )
                },
                label = { Text(route.label ?: "") },
            )
        }
    }
}
