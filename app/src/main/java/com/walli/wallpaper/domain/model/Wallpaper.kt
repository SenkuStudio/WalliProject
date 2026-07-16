package com.walli.wallpaper.domain.model

data class Wallpaper(
    val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val downloads: Int,
    val createdAt: String,
    val isPremium: Boolean = false,
    val isUnlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val blurHash: String? = null,
)
