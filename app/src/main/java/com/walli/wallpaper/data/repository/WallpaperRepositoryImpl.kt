package com.walli.wallpaper.data.repository

import com.walli.wallpaper.data.api.WallpaperApi
import com.walli.wallpaper.data.local.dao.CategoryDao
import com.walli.wallpaper.data.local.dao.WallpaperDao
import com.walli.wallpaper.data.local.entity.CategoryEntity
import com.walli.wallpaper.data.local.entity.WallpaperEntity
import com.walli.wallpaper.domain.model.PagedResult
import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.model.WallpaperCategory
import com.walli.wallpaper.domain.model.WallpaperSort
import com.walli.wallpaper.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepositoryImpl @Inject constructor(
    private val api: WallpaperApi,
    private val categoryDao: CategoryDao,
    private val wallpaperDao: WallpaperDao
) : WallpaperRepository {

    override suspend fun getWallpapers(
        page: Int,
        limit: Int,
        categoryId: Int?,
        query: String?,
        sort: WallpaperSort,
    ): Result<PagedResult<Wallpaper>> = runCatching {
        val response = api.getWallpapers(
            page = page,
            limit = limit,
            categoryId = categoryId,
            query = query?.takeIf { it.isNotBlank() },
            sort = sort.apiValue,
        )
        
        val wallpapers = response.data.mapIndexedNotNull { index, dto ->
            val absoluteIndex = ((response.pagination?.page ?: page) - 1) * (response.pagination?.perPage ?: limit) + index
            dto.toDomainOrNull()?.copy(
                isPremium = (absoluteIndex + 1) % 3 == 0
            )
        }

        // Cache the first page of "Latest" for fast startup
        if (page == 1 && categoryId == null && query.isNullOrBlank() && sort == WallpaperSort.LATEST) {
            wallpaperDao.deleteAll()
            wallpaperDao.insertWallpapers(wallpapers.map { WallpaperEntity.fromDomain(it) })
        }

        PagedResult(
            items = wallpapers,
            page = response.pagination?.page ?: page,
            limit = response.pagination?.perPage ?: limit,
            hasNext = response.pagination?.hasNext ?: false,
        )
    }

    override fun getLatestCachedWallpapers(limit: Int): Flow<List<Wallpaper>> {
        return wallpaperDao.getLatestWallpapers(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCategories(): Result<List<WallpaperCategory>> = runCatching {
        val cached = categoryDao.getAllCategories().firstOrNull()
        if (!cached.isNullOrEmpty()) {
            // Background refresh categories
            try {
                val remote = api.getCategories().data.map { it.toDomain() }
                categoryDao.insertCategories(remote.map { CategoryEntity.fromDomain(it) })
            } catch (e: Exception) {
                // Ignore background refresh failure if we have cache
            }
            return@runCatching cached.map { it.toDomain() }.sortedBy { it.name }
        }

        val remote = api.getCategories().data
            .map { it.toDomain() }
            .sortedBy { it.name }
        
        categoryDao.insertCategories(remote.map { CategoryEntity.fromDomain(it) })
        remote
    }

    override suspend fun incrementDownload(wallpaperId: String): Result<Unit> = runCatching {
        api.incrementDownload(wallpaperId)
        Unit
    }
}
