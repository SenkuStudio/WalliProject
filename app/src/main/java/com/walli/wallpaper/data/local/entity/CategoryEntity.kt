package com.walli.wallpaper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walli.wallpaper.domain.model.WallpaperCategory

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val name: String?,
    val slug: String?,
    val coverUrl: String?,
) {
    fun toDomain() = WallpaperCategory(
        id = id,
        name = name,
        slug = slug,
        coverUrl = coverUrl
    )

    companion object {
        fun fromDomain(category: WallpaperCategory) = CategoryEntity(
            id = category.id ?: 0,
            name = category.name,
            slug = category.slug,
            coverUrl = category.coverUrl
        )
    }
}
