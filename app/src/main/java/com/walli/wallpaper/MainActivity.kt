package com.walli.wallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.walli.wallpaper.data.settings.AppTheme
import com.walli.wallpaper.data.settings.SettingsManager
import com.walli.wallpaper.ui.navigation.WalliNavGraph
import com.walli.wallpaper.ui.theme.WalliTheme
import dagger.hilt.android.AndroidEntryPoint
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
            
            WalliTheme(
                theme = theme,
                dynamicColor = dynamicColor
            ) {
                val navController = rememberNavController()
                WalliNavGraph(navController = navController)
            }
        }
    }
}
