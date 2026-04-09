package com.walli.wallpaper.domain.usecase

import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.repository.LibraryRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(wallpaper: Wallpaper): Boolean = repository.toggleFavorite(wallpaper)
}
