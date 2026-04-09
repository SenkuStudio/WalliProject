package com.walli.wallpaper.domain.repository

import com.walli.wallpaper.domain.model.PagedResult
import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.model.WallpaperCategory
import com.walli.wallpaper.domain.model.WallpaperSort

interface WallpaperRepository {
    suspend fun getWallpapers(
        page: Int,
        limit: Int,
        category: String?,
        query: String?,
        sort: WallpaperSort,
    ): Result<PagedResult<Wallpaper>>

    suspend fun getCategories(): Result<List<WallpaperCategory>>

    suspend fun incrementDownload(wallpaperId: String): Result<Unit>
}
