package com.walli.wallpaper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.walli.wallpaper.data.local.entity.WallpaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpapers ORDER BY sortOrder DESC LIMIT :limit")
    fun getLatestWallpapers(limit: Int): Flow<List<WallpaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpapers(wallpapers: List<WallpaperEntity>)

    @Query("DELETE FROM wallpapers WHERE id NOT IN (SELECT id FROM wallpapers ORDER BY sortOrder DESC LIMIT 100)")
    suspend fun clearOldWallpapers()

    @Query("DELETE FROM wallpapers")
    suspend fun deleteAll()
}
