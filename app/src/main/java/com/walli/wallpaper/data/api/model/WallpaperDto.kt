package com.walli.wallpaper.data.api.model

import com.google.gson.annotations.SerializedName

data class WallpaperDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String,
    @SerializedName("downloads") val downloads: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("premium") val premium: Boolean = false,
)

data class WallpaperPageDto(
    @SerializedName("items") val items: List<WallpaperDto> = emptyList(),
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 20,
    @SerializedName("hasNext") val hasNext: Boolean = false,
)

data class CategoryDto(
    @SerializedName("name") val name: String,
    @SerializedName("cover_url") val coverUrl: String? = null,
)

data class DownloadAckDto(
    @SerializedName("ok") val ok: Boolean = true,
)
