package com.walli.wallpaper.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.walli.wallpaper.data.local.dao.FavoritesDao
import com.walli.wallpaper.data.local.dao.RecentDao
import com.walli.wallpaper.data.local.entity.FavoriteWallpaperEntity
import com.walli.wallpaper.data.local.entity.RecentWallpaperEntity

@Database(
    entities = [FavoriteWallpaperEntity::class, RecentWallpaperEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun recentDao(): RecentDao
}
