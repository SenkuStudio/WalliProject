package com.walli.wallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.walli.wallpaper.data.settings.AppTheme
import com.walli.wallpaper.data.settings.SettingsManager
import com.walli.wallpaper.ui.navigation.WalliNavGraph
import com.walli.wallpaper.ui.navigation.WalliRoute
import com.walli.wallpaper.ui.theme.WalliTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        MobileAds.initialize(this)

        setContent {
            val theme by settingsManager.theme.collectAsState(initial = AppTheme.SYSTEM)
            val dynamicColor by settingsManager.dynamicColor.collectAsState(initial = false)
            val onboardingCompleted by settingsManager.onboardingCompleted.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            WalliTheme(
                theme = theme,
                dynamicColor = dynamicColor
            ) {
                if (onboardingCompleted != null) {
                    val navController = rememberNavController()
                    val startDestination = if (onboardingCompleted == true) {
                        WalliRoute.Home.route
                    } else {
                        WalliRoute.Onboarding.route
                    }

                    WalliNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        onOnboardingComplete = {
                            scope.launch {
                                settingsManager.setOnboardingCompleted(true)
                            }
                        }
                    )

                    // Update onboarding status when Onboarding is finished via NavGraph's callback
                    // Wait, the callback is inside NavGraph. Let's pass a callback to NavGraph or just handle it here.
                    // Actually, WalliNavGraph handles navigation. We need to save the state.
                }
            }
        }
    }
}
