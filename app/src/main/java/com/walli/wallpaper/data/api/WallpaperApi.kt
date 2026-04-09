package com.walli.wallpaper.data.api

import com.walli.wallpaper.data.api.model.CategoryDto
import com.walli.wallpaper.data.api.model.DownloadAckDto
import com.walli.wallpaper.data.api.model.WallpaperPageDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WallpaperApi {
    @GET("api/v1/wallpapers")
    suspend fun getWallpapers(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("category") category: String? = null,
        @Query("query") query: String? = null,
        @Query("sort") sort: String? = null,
    ): WallpaperPageDto

    @GET("api/v1/categories")
    suspend fun getCategories(): List<CategoryDto>

    @POST("api/v1/wallpapers/{id}/download")
    suspend fun incrementDownload(@Path("id") wallpaperId: String): DownloadAckDto
}
