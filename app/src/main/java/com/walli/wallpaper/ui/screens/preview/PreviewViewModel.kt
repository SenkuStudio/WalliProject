package com.walli.wallpaper.ui.screens.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.model.WallpaperTarget
import com.walli.wallpaper.domain.usecase.IncrementDownloadUseCase
import com.walli.wallpaper.domain.usecase.ObserveFavoriteIdsUseCase
import com.walli.wallpaper.domain.usecase.SaveRecentWallpaperUseCase
import com.walli.wallpaper.domain.usecase.ToggleFavoriteUseCase
import com.walli.wallpaper.preview.PreviewSessionManager
import com.walli.wallpaper.wallpaper.ShareManager
import com.walli.wallpaper.wallpaper.WallpaperApplier
import com.walli.wallpaper.wallpaper.WallpaperDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PreviewUiState(
    val items: List<Wallpaper> = emptyList(),
    val initialIndex: Int = 0,
    val controlsVisible: Boolean = true,
    val isWorking: Boolean = false,
    val workingLabel: String? = null,
    val message: String? = null,
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    previewSessionManager: PreviewSessionManager,
    observeFavoriteIdsUseCase: ObserveFavoriteIdsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val saveRecentWallpaperUseCase: SaveRecentWallpaperUseCase,
    private val incrementDownloadUseCase: IncrementDownloadUseCase,
    private val wallpaperDownloader: WallpaperDownloader,
    private val wallpaperApplier: WallpaperApplier,
    private val shareManager: ShareManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(previewSessionManager.session, observeFavoriteIdsUseCase()) { session, favoriteIds ->
                session to favoriteIds
            }.collect { (session, favoriteIds) ->
                _uiState.update { current ->
                    current.copy(
                        items = session.items.map { it.copy(isFavorite = it.id in favoriteIds) },
                        initialIndex = session.initialIndex,
                    )
                }
            }
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(controlsVisible = !it.controlsVisible) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun onPageSettled(index: Int) {
        viewModelScope.launch {
            _uiState.value.items.getOrNull(index)?.let { wallpaper ->
                saveRecentWallpaperUseCase(wallpaper)
            }
        }
    }

    fun toggleFavorite(index: Int) {
        val wallpaper = _uiState.value.items.getOrNull(index) ?: return
        viewModelScope.launch {
            toggleFavoriteUseCase(wallpaper)
        }
    }

    fun download(index: Int) {
        val wallpaper = _uiState.value.items.getOrNull(index) ?: return
        viewModelScope.launch {
            setWorking(label = "Downloading…")
            wallpaperDownloader.download(wallpaper.imageUrl, wallpaper.title)
                .onSuccess {
                    incrementDownloadUseCase(wallpaper.id)
                    setMessage("Saved to Downloads")
                }
                .onFailure { throwable ->
                    setMessage(throwable.message ?: "Download failed")
                }
            clearWorking()
        }
    }

    fun applyWallpaper(index: Int, target: WallpaperTarget) {
        val wallpaper = _uiState.value.items.getOrNull(index) ?: return
        viewModelScope.launch {
            setWorking(label = "Applying wallpaper…")
            wallpaperApplier.apply(wallpaper.imageUrl, target)
                .onSuccess {
                    setMessage("Wallpaper applied to ${target.label.lowercase()}")
                }
                .onFailure { throwable ->
                    setMessage(throwable.message ?: "Wallpaper apply failed")
                }
            clearWorking()
        }
    }

    fun share(index: Int) {
        val wallpaper = _uiState.value.items.getOrNull(index) ?: return
        viewModelScope.launch {
            setWorking(label = "Preparing share…")
            shareManager.share(wallpaper.imageUrl, wallpaper.title)
                .onFailure { throwable ->
                    setMessage(throwable.message ?: "Share failed")
                }
            clearWorking()
        }
    }

    private fun setWorking(label: String) {
        _uiState.update { it.copy(isWorking = true, workingLabel = label) }
    }

    private fun clearWorking() {
        _uiState.update { it.copy(isWorking = false, workingLabel = null) }
    }

    private fun setMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }
}
