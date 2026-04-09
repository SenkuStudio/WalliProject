package com.walli.wallpaper.ui.screens.onboarding

import androidx.compose.runtime.Composable
import com.walli.wallpaper.ui.components.EmptyState

@Composable
fun OnboardingScreen() {
    EmptyState(
        title = "Onboarding scaffold ready",
        subtitle = "Hook this screen to DataStore if you want first-launch walkthroughs.",
    )
}
