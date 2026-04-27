package com.walli.wallpaper.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.walli.wallpaper.data.settings.AppTheme
import com.walli.wallpaper.data.settings.SettingsManager
import com.walli.wallpaper.worker.AutoWallpaperService
import com.walli.wallpaper.worker.AutoWallpaperWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = false,
    val autoWallpaper: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsManager.theme,
        settingsManager.dynamicColor,
        settingsManager.autoWallpaper
    ) { theme, dynamicColor, autoWallpaper ->
        SettingsUiState(theme, dynamicColor, autoWallpaper)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsManager.setTheme(theme) }
    }

    fun setDynamicColor(enabled: Boolean) {
        if (uiState.value.dynamicColor == enabled) return
        viewModelScope.launch { settingsManager.setDynamicColor(enabled) }
    }

    fun setAutoWallpaper(enabled: Boolean) {
        if (uiState.value.autoWallpaper == enabled) return
        viewModelScope.launch { 
            settingsManager.setAutoWallpaper(enabled) 
            val intent = Intent(context, AutoWallpaperService::class.java)
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        }
    }
}
