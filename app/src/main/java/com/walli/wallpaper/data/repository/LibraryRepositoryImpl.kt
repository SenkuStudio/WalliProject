package com.walli.wallpaper.data.repository

import com.walli.wallpaper.data.local.dao.FavoritesDao
import com.walli.wallpaper.data.local.dao.RecentDao
import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.repository.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val favoritesDao: FavoritesDao,
    private val recentDao: RecentDao,
) : LibraryRepository {

    override fun observeFavorites(): Flow<List<Wallpaper>> =
        favoritesDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeFavoriteIds(): Flow<Set<String>> =
        favoritesDao.observeIds().map { it.toSet() }

    override fun observeRecents(): Flow<List<Wallpaper>> =
        recentDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun toggleFavorite(wallpaper: Wallpaper): Boolean {
        val isFavorite = favoritesDao.isFavorite(wallpaper.id)
        return if (isFavorite) {
            favoritesDao.deleteById(wallpaper.id)
            false
        } else {
            favoritesDao.upsert(wallpaper.toFavoriteEntity(now = System.currentTimeMillis()))
            true
        }
    }

    override suspend fun addRecent(wallpaper: Wallpaper) {
        recentDao.upsert(wallpaper.toRecentEntity(now = System.currentTimeMillis()))
        recentDao.trimTo(limit = 40)
    }

    override suspend fun isFavorite(wallpaperId: String): Boolean =
        favoritesDao.isFavorite(wallpaperId)
}
