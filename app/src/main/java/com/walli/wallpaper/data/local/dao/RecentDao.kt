package com.walli.wallpaper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.walli.wallpaper.data.local.entity.RecentWallpaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_wallpapers ORDER BY viewedAt DESC LIMIT 40")
    fun observeAll(): Flow<List<RecentWallpaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentWallpaperEntity)

    @Query(
        "DELETE FROM recent_wallpapers WHERE id NOT IN (SELECT id FROM recent_wallpapers ORDER BY viewedAt DESC LIMIT :limit)"
    )
    suspend fun trimTo(limit: Int)
}
