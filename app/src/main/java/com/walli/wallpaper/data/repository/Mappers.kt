package com.walli.wallpaper.data.repository

import com.walli.wallpaper.data.api.model.CategoryDto
import com.walli.wallpaper.data.api.model.WallpaperDto
import com.walli.wallpaper.data.local.entity.FavoriteWallpaperEntity
import com.walli.wallpaper.data.local.entity.RecentWallpaperEntity
import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.model.WallpaperCategory

internal fun WallpaperDto.toDomainOrNull(): Wallpaper? {
    if (id.isBlank() || imageUrl.isBlank() || thumbnailUrl.isBlank()) return null
    return Wallpaper(
        id = id,
        title = title.ifBlank { "Untitled" },
        category = category.ifBlank { "Uncategorized" },
        imageUrl = imageUrl,
        thumbnailUrl = thumbnailUrl,
        downloads = downloads,
        createdAt = createdAt,
        isPremium = premium,
    )
}

internal fun CategoryDto.toDomain(): WallpaperCategory = WallpaperCategory(
    name = name,
    coverUrl = coverUrl,
)

internal fun Wallpaper.toFavoriteEntity(now: Long): FavoriteWallpaperEntity = FavoriteWallpaperEntity(
    id = id,
    title = title,
    category = category,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    downloads = downloads,
    createdAt = createdAt,
    isPremium = isPremium,
    addedAt = now,
)

internal fun FavoriteWallpaperEntity.toDomain(): Wallpaper = Wallpaper(
    id = id,
    title = title,
    category = category,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    downloads = downloads,
    createdAt = createdAt,
    isPremium = isPremium,
    isFavorite = true,
)

internal fun Wallpaper.toRecentEntity(now: Long): RecentWallpaperEntity = RecentWallpaperEntity(
    id = id,
    title = title,
    category = category,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    downloads = downloads,
    createdAt = createdAt,
    isPremium = isPremium,
    viewedAt = now,
)

internal fun RecentWallpaperEntity.toDomain(): Wallpaper = Wallpaper(
    id = id,
    title = title,
    category = category,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    downloads = downloads,
    createdAt = createdAt,
    isPremium = isPremium,
)
