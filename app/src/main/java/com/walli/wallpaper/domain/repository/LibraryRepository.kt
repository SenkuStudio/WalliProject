package com.walli.wallpaper.domain.repository

import com.walli.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeFavorites(): Flow<List<Wallpaper>>
    fun observeFavoriteIds(): Flow<Set<String>>
    fun observeRecents(): Flow<List<Wallpaper>>
    suspend fun toggleFavorite(wallpaper: Wallpaper): Boolean
    suspend fun addRecent(wallpaper: Wallpaper)
    suspend fun isFavorite(wallpaperId: String): Boolean
}
