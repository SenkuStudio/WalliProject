package com.walli.wallpaper.domain.usecase

import com.walli.wallpaper.domain.model.WallpaperSort
import com.walli.wallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject

class GetWallpapersUseCase @Inject constructor(
    private val repository: WallpaperRepository,
) {
    suspend operator fun invoke(
        page: Int,
        limit: Int,
        category: String?,
        query: String?,
        sort: WallpaperSort,
    ) = repository.getWallpapers(page, limit, category, query, sort)
}
