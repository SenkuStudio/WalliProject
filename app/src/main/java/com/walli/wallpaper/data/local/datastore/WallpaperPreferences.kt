package com.walli.wallpaper.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "wallpaper_prefs")

@Singleton
class WallpaperPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val unlockedWallpapersKey = stringSetPreferencesKey("unlocked_wallpapers")

    val unlockedWallpaperIds: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[unlockedWallpapersKey] ?: emptySet()
        }

    suspend fun unlockWallpaper(id: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[unlockedWallpapersKey] ?: emptySet()
            preferences[unlockedWallpapersKey] = current + id
        }
    }
}
