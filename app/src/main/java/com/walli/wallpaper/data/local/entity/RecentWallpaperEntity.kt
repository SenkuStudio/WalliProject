package com.walli.wallpaper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_wallpapers")
data class RecentWallpaperEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val downloads: Int,
    val createdAt: String,
    val isPremium: Boolean,
    val viewedAt: Long,
)
