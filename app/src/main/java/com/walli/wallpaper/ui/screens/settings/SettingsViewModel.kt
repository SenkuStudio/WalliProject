package com.walli.wallpaper.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.walli.wallpaper.data.settings.AppTheme
import com.walli.wallpaper.data.settings.AutoWallpaperSource
import com.walli.wallpaper.data.settings.SettingsManager
import com.walli.wallpaper.domain.model.WallpaperCategory
import com.walli.wallpaper.domain.repository.WallpaperRepository
import com.walli.wallpaper.worker.AutoWallpaperWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import coil3.SingletonImageLoader
import android.text.format.Formatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = false,
    val autoWallpaper: Boolean = false,
    val autoWallpaperSource: AutoWallpaperSource = AutoWallpaperSource.RANDOM,
    val autoWallpaperCategoryId: Int? = null,
    val categories: List<WallpaperCategory> = emptyList(),
    val cacheSize: String = "0 MB"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val repository: WallpaperRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _categories = MutableStateFlow<List<WallpaperCategory>>(emptyList())
    private val _cacheSize = MutableStateFlow("0 MB")

    init {
        viewModelScope.launch {
            _categories.value = repository.getCategories().getOrDefault(emptyList())
        }
        updateCacheSize()
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsManager.theme,
        settingsManager.dynamicColor,
        settingsManager.autoWallpaper,
        settingsManager.autoWallpaperSource,
        settingsManager.autoWallpaperCategoryId,
        _categories,
        _cacheSize
    ) { args ->
        SettingsUiState(
            theme = args[0] as AppTheme,
            dynamicColor = args[1] as Boolean,
            autoWallpaper = args[2] as Boolean,
            autoWallpaperSource = args[3] as AutoWallpaperSource,
            autoWallpaperCategoryId = args[4] as Int?,
            categories = args[5] as List<WallpaperCategory>,
            cacheSize = args[6] as String
        )
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
            if (enabled) {
                scheduleAutoWallpaper(
                    source = uiState.value.autoWallpaperSource,
                    categoryId = uiState.value.autoWallpaperCategoryId
                )
            } else {
                workManager.cancelUniqueWork("AutoWallpaperWork")
            }
        }
    }

    fun setAutoWallpaperSource(source: AutoWallpaperSource) {
        viewModelScope.launch {
            settingsManager.setAutoWallpaperSource(source)
            if (uiState.value.autoWallpaper) {
                scheduleAutoWallpaper(
                    source = source,
                    categoryId = uiState.value.autoWallpaperCategoryId
                )
            }
        }
    }

    fun setAutoWallpaperCategory(categoryId: Int?) {
        viewModelScope.launch {
            settingsManager.setAutoWallpaperCategoryId(categoryId)
            if (uiState.value.autoWallpaper) {
                scheduleAutoWallpaper(
                    source = uiState.value.autoWallpaperSource,
                    categoryId = categoryId
                )
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val imageLoader = SingletonImageLoader.get(context)
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
            updateCacheSize()
        }
    }

    private fun updateCacheSize() {
        val size = SingletonImageLoader.get(context).diskCache?.size ?: 0L
        _cacheSize.value = Formatter.formatShortFileSize(context, size)
    }

    private fun scheduleAutoWallpaper(source: AutoWallpaperSource, categoryId: Int?) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoWallpaperWorker>(
            4, TimeUnit.HOURS
        ).setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "AutoWallpaperWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}
