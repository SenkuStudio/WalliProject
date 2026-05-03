package com.walli.wallpaper.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class AutoWallpaperSource {
    RANDOM, FAVORITES, CATEGORY
}

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeKey = stringPreferencesKey("app_theme")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val autoWallpaperKey = booleanPreferencesKey("auto_wallpaper")
    private val autoWallpaperSourceKey = stringPreferencesKey("auto_wallpaper_source")
    private val autoWallpaperCategoryIdKey = intPreferencesKey("auto_wallpaper_category_id")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")

    val theme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[themeKey] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[dynamicColorKey] ?: false
    }

    val autoWallpaper: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[autoWallpaperKey] ?: false
    }

    val autoWallpaperSource: Flow<AutoWallpaperSource> = context.dataStore.data.map { preferences ->
        val sourceName = preferences[autoWallpaperSourceKey] ?: AutoWallpaperSource.RANDOM.name
        try {
            AutoWallpaperSource.valueOf(sourceName)
        } catch (e: Exception) {
            AutoWallpaperSource.RANDOM
        }
    }

    val autoWallpaperCategoryId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[autoWallpaperCategoryIdKey]
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[onboardingCompletedKey] ?: false
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[dynamicColorKey] = enabled
        }
    }

    suspend fun setAutoWallpaper(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[autoWallpaperKey] = enabled
        }
    }

    suspend fun setAutoWallpaperSource(source: AutoWallpaperSource) {
        context.dataStore.edit { preferences ->
            preferences[autoWallpaperSourceKey] = source.name
        }
    }

    suspend fun setAutoWallpaperCategoryId(categoryId: Int?) {
        context.dataStore.edit { preferences ->
            if (categoryId == null) {
                preferences.remove(autoWallpaperCategoryIdKey)
            } else {
                preferences[autoWallpaperCategoryIdKey] = categoryId
            }
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[onboardingCompletedKey] = completed
        }
    }
}
