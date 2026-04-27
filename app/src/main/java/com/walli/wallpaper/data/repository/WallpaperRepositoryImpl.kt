package com.walli.wallpaper.data.repository

import com.walli.wallpaper.data.api.WallpaperApi
import com.walli.wallpaper.domain.model.PagedResult
import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.model.WallpaperCategory
import com.walli.wallpaper.domain.model.WallpaperSort
import com.walli.wallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepositoryImpl @Inject constructor(
    private val api: WallpaperApi,
) : WallpaperRepository {

    override suspend fun getWallpapers(
        page: Int,
        limit: Int,
        category: String?,
        query: String?,
        sort: WallpaperSort,
    ): Result<PagedResult<Wallpaper>> = runCatching {
        val response = api.getWallpapers(
            page = page,
            limit = limit,
            category = category?.takeIf { it.isNotBlank() && it != "All" },
            query = query?.takeIf { it.isNotBlank() },
            sort = sort.apiValue,
        )
        PagedResult(
            items = response.data.mapNotNull { it.toDomainOrNull() },
            page = response.pagination?.page ?: page,
            limit = response.pagination?.perPage ?: limit,
            hasNext = response.pagination?.hasNext ?: false,
        )
    }

    override suspend fun getCategories(): Result<List<WallpaperCategory>> = runCatching {
        api.getCategories().data
            .map { it.toDomain() }
            .sortedBy { it.name }
    }

    override suspend fun incrementDownload(wallpaperId: String): Result<Unit> = runCatching {
        api.incrementDownload(wallpaperId)
        Unit
    }
}
