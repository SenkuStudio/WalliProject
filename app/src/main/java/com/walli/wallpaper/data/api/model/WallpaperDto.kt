package com.walli.wallpaper.data.api.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T,
    @SerializedName("pagination") val pagination: PaginationDto? = null
)

data class PaginationDto(
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("has_next") val hasNext: Boolean,
    @SerializedName("has_prev") val hasPrev: Boolean
)

data class WallpaperDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("thumb_url") val thumbnailUrl: String,
    @SerializedName("download_count") val downloads: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_featured") val isFeatured: Boolean = false,
    @SerializedName("category") val category: CategoryDto,
)

data class CategoryDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("cover_url") val coverUrl: String? = null,
)

data class DownloadAckDto(
    @SerializedName("ok") val ok: Boolean = true,
)
