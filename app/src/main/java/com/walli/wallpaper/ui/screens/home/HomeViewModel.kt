package com.walli.wallpaper.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.model.WallpaperCategory
import com.walli.wallpaper.domain.model.WallpaperSort
import com.walli.wallpaper.domain.usecase.GetCategoriesUseCase
import com.walli.wallpaper.domain.usecase.GetWallpapersUseCase
import com.walli.wallpaper.domain.usecase.ObserveFavoriteIdsUseCase
import com.walli.wallpaper.domain.usecase.ObserveLatestCachedWallpapersUseCase
import com.walli.wallpaper.domain.usecase.ObserveRecentsUseCase
import com.walli.wallpaper.domain.usecase.ToggleFavoriteUseCase
import com.walli.wallpaper.util.NetworkMonitor
import com.walli.wallpaper.preview.PreviewSessionManager
import com.walli.wallpaper.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.walli.wallpaper.data.local.datastore.WallpaperPreferences
import kotlinx.coroutines.flow.combine

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getWallpapersUseCase: GetWallpapersUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val observeFavoriteIdsUseCase: ObserveFavoriteIdsUseCase,
    private val observeLatestCachedWallpapersUseCase: ObserveLatestCachedWallpapersUseCase,
    private val observeRecentsUseCase: ObserveRecentsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val previewSessionManager: PreviewSessionManager,
    private val networkMonitor: NetworkMonitor,
    private val wallpaperPreferences: WallpaperPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialCategoryId: Int? = savedStateHandle.get<Int>("categoryId")?.takeIf { it != -1 }

    private val _uiState = MutableStateFlow(HomeUiState(selectedCategoryId = initialCategoryId))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val pageSize = 30
    private var currentPage = 0
    private var requestInFlight = false

    init {
        observeNetwork()
        observeFavoritesAndUnlocks()
        observeRecents()
        observeCache()
        loadCategories()
        refresh()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                _uiState.update { it.copy(isOnline = isOnline) }
                if (isOnline && _uiState.value.wallpapers.isEmpty()) {
                    refresh()
                }
            }
        }
    }

    private fun observeCache() {
        viewModelScope.launch {
            // Only show cache if it's the default "All" + "Latest" view
            observeLatestCachedWallpapersUseCase(pageSize).collect { cached ->
                val state = _uiState.value
                if (state.wallpapers.isEmpty() && state.selectedCategoryId == null && state.sort == WallpaperSort.LATEST) {
                    _uiState.update { it.copy(wallpapers = cached) }
                }
            }
        }
    }

    fun selectCategory(category: WallpaperCategory) {
        if (_uiState.value.selectedCategoryId == category.id) return
        _uiState.update { it.copy(selectedCategoryId = category.id) }
        refresh()
    }

    fun changeSort(sort: WallpaperSort) {
        if (_uiState.value.sort == sort) return
        _uiState.update { it.copy(sort = sort) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            currentPage = 0
            fetchPage(reset = true)
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            fetchPage(reset = false)
        }
    }

    fun openPreview(index: Int) {
        previewSessionManager.open(
            items = _uiState.value.wallpapers,
            initialIndex = index,
            source = "home",
        )
    }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch {
            toggleFavoriteUseCase(wallpaper)
        }
    }

    private fun observeFavoritesAndUnlocks() {
        viewModelScope.launch {
            combine(
                observeFavoriteIdsUseCase(),
                wallpaperPreferences.unlockedWallpaperIds
            ) { favorites, unlocks ->
                _uiState.update { state ->
                    state.copy(
                        favoriteIds = favorites,
                        unlockedIds = unlocks
                    )
                }
            }.collect {}
        }
    }

    // Remove markMetadata as we now handle this in the UI/Composables
    // private fun markMetadata(items: List<Wallpaper>): List<Wallpaper> = ...

    fun onWallpaperClick(index: Int) {
        val wallpaper = _uiState.value.wallpapers.getOrNull(index) ?: return
        val isUnlocked = wallpaper.id in _uiState.value.unlockedIds
        if (wallpaper.isPremium && !isUnlocked) {
            _uiState.update { it.copy(wallpaperToUnlock = wallpaper) }
        } else {
            openPreview(index)
        }
    }

    fun dismissUnlockDialog() {
        _uiState.update { it.copy(wallpaperToUnlock = null) }
    }

    fun unlockWallpaper(wallpaper: Wallpaper) {
        viewModelScope.launch {
            wallpaperPreferences.unlockWallpaper(wallpaper.id)
            _uiState.update { it.copy(wallpaperToUnlock = null) }
            val index = _uiState.value.wallpapers.indexOfFirst { it.id == wallpaper.id }
            if (index != -1) {
                openPreview(index)
            }
        }
    }

    private fun observeRecents() {
        viewModelScope.launch {
            observeRecentsUseCase().collect { recents ->
                _uiState.update { it.copy(recentWallpapers = recents) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase()
                .onSuccess { categories ->
                    _uiState.update { state ->
                        state.copy(categories = listOf(WallpaperCategory(id = null, name = "All")) + categories)
                    }
                }
        }
    }

    private suspend fun fetchPage(reset: Boolean) {
        if (requestInFlight) return
        if (!reset && !_uiState.value.hasNext) return
        requestInFlight = true

        _uiState.update { state ->
            state.copy(
                loadState = when {
                    reset && state.wallpapers.isEmpty() -> LoadState.Loading
                    reset -> LoadState.Refreshing
                    else -> LoadState.Appending
                },
            )
        }

        val nextPage = if (reset) 1 else currentPage + 1
        val state = _uiState.value
        getWallpapersUseCase(
            page = nextPage,
            limit = pageSize,
            categoryId = state.selectedCategoryId,
            query = null,
            sort = state.sort,
        )
            .onSuccess { page ->
                currentPage = page.page
                val merged = if (reset) {
                    page.items
                } else {
                    (_uiState.value.wallpapers + page.items).distinctBy { it.id }
                }
                _uiState.update {
                    it.copy(
                        wallpapers = merged,
                        hasNext = page.hasNext,
                        loadState = if (merged.isEmpty()) LoadState.Empty else LoadState.Idle,
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        loadState = LoadState.Error(
                            throwable.message ?: "Unable to load wallpapers",
                        ),
                    )
                }
            }

        requestInFlight = false
    }
}
