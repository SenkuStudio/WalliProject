package com.walli.wallpaper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.walli.wallpaper.data.local.entity.FavoriteWallpaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorite_wallpapers ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteWallpaperEntity>>

    @Query("SELECT id FROM favorite_wallpapers")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_wallpapers WHERE id = :wallpaperId)")
    suspend fun isFavorite(wallpaperId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteWallpaperEntity)

    @Query("DELETE FROM favorite_wallpapers WHERE id = :wallpaperId")
    suspend fun deleteById(wallpaperId: String)
}
