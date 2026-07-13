package com.walli.wallpaper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_wallpapers")
data class FavoriteWallpaperEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val downloads: Int,
    val createdAt: String,
    val isPremium: Boolean,
    val addedAt: Long,
    val blurHash: String? = null,
)
