package com.walli.wallpaper.ui.screens.home

import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.model.WallpaperCategory
import com.walli.wallpaper.domain.model.WallpaperSort
import com.walli.wallpaper.ui.common.LoadState

data class HomeUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val recentWallpapers: List<Wallpaper> = emptyList(),
    val categories: List<WallpaperCategory> = listOf(WallpaperCategory(id = null, name = "All")),
    val selectedCategoryId: Int? = null,
    val sort: WallpaperSort = WallpaperSort.LATEST,
    val loadState: LoadState = LoadState.Loading,
    val hasNext: Boolean = true,
    val isOnline: Boolean = true,
)
