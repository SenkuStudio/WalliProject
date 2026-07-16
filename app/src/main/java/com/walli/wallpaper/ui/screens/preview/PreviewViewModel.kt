package com.walli.wallpaper.ui.screens.preview

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walli.wallpaper.data.local.datastore.WallpaperPreferences
import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.model.WallpaperTarget
import com.walli.wallpaper.domain.usecase.IncrementDownloadUseCase
import com.walli.wallpaper.domain.usecase.ObserveFavoriteIdsUseCase
import com.walli.wallpaper.domain.usecase.SaveRecentWallpaperUseCase
import com.walli.wallpaper.domain.usecase.ToggleFavoriteUseCase
import com.walli.wallpaper.util.NetworkMonitor
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

data class ImageTransformation(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    val rotation: Float = 0f,
)

data class PreviewUiState(
    val items: List<Wallpaper> = emptyList(),
    val initialIndex: Int = 0,
    val controlsVisible: Boolean = true,
    val isWorking: Boolean = false,
    val workingLabel: String? = null,
    val message: String? = null,
    val isOnline: Boolean = true,
    val transformations: Map<Int, ImageTransformation> = emptyMap(),
    val wallpaperToUnlock: Wallpaper? = null,
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
    private val networkMonitor: NetworkMonitor,
    private val wallpaperPreferences: WallpaperPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    private var favoriteIds = emptySet<String>()
    private var unlockedIds = emptySet<String>()

    init {
        observeNetwork()
        viewModelScope.launch {
            combine(
                previewSessionManager.session,
                observeFavoriteIdsUseCase(),
                wallpaperPreferences.unlockedWallpaperIds
            ) { session, favorites, unlocks ->
                Triple(session, favorites, unlocks)
            }.collect { (session, favorites, unlocks) ->
                favoriteIds = favorites
                unlockedIds = unlocks
                _uiState.update { current ->
                    current.copy(
                        items = session.items.map { wallpaper ->
                            wallpaper.copy(
                                isFavorite = wallpaper.id in favorites,
                                isUnlocked = wallpaper.id in unlocks
                            )
                        },
                        initialIndex = session.initialIndex,
                    )
                }
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                _uiState.update { it.copy(isOnline = isOnline) }
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
            val wallpaper = _uiState.value.items.getOrNull(index) ?: return@launch
            saveRecentWallpaperUseCase(wallpaper)
        }
    }

    fun requestUnlock(index: Int) {
        val wallpaper = _uiState.value.items.getOrNull(index) ?: return
        if (wallpaper.isPremium && !wallpaper.isUnlocked) {
            _uiState.update { it.copy(wallpaperToUnlock = wallpaper) }
        }
    }

    fun dismissUnlockDialog() {
        _uiState.update { it.copy(wallpaperToUnlock = null) }
    }

    fun unlockWallpaper(wallpaper: Wallpaper) {
        viewModelScope.launch {
            wallpaperPreferences.unlockWallpaper(wallpaper.id)
            _uiState.update { it.copy(wallpaperToUnlock = null) }
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
        val transformation = _uiState.value.transformations[index] ?: ImageTransformation()
        viewModelScope.launch {
            setWorking(label = "Downloading…")
            wallpaperDownloader.download(
                url = wallpaper.imageUrl,
                title = wallpaper.title,
                scale = transformation.scale,
                offsetX = transformation.offset.x,
                offsetY = transformation.offset.y,
                rotation = transformation.rotation,
                applier = wallpaperApplier
            )
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

    fun updateTransformation(index: Int, scale: Float, offset: Offset, rotation: Float) {
        _uiState.update { current ->
            current.copy(
                transformations = current.transformations + (index to ImageTransformation(scale, offset, rotation))
            )
        }
    }

    fun resetTransformation(index: Int) {
        _uiState.update { current ->
            current.copy(
                transformations = current.transformations - index
            )
        }
    }

    fun handleDoubleTap(index: Int, tapOffset: Offset = Offset.Zero, center: Offset = Offset.Zero) {
        _uiState.update { current ->
            val currentTransformation = current.transformations[index] ?: ImageTransformation()
            val newTransformation = if (currentTransformation.scale > 1f) {
                ImageTransformation() // Reset to 1x
            } else {
                val targetScale = 2.5f
                // Zoom at tap location relative to center pivot
                val newOffset = (tapOffset - center) * (1f - targetScale)
                ImageTransformation(scale = targetScale, offset = newOffset)
            }
            current.copy(
                transformations = current.transformations + (index to newTransformation)
            )
        }
    }

    fun applyWallpaper(index: Int, target: WallpaperTarget) {
        val wallpaper = _uiState.value.items.getOrNull(index) ?: return
        val transformation = _uiState.value.transformations[index] ?: ImageTransformation()
        viewModelScope.launch {
            setWorking(label = "Applying wallpaper…")
            wallpaperApplier.apply(
                url = wallpaper.imageUrl,
                target = target,
                scale = transformation.scale,
                offsetX = transformation.offset.x,
                offsetY = transformation.offset.y,
                rotation = transformation.rotation
            )
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
        val transformation = _uiState.value.transformations[index] ?: ImageTransformation()
        viewModelScope.launch {
            setWorking(label = "Preparing share…")
            shareManager.share(
                url = wallpaper.imageUrl,
                title = wallpaper.title,
                scale = transformation.scale,
                offsetX = transformation.offset.x,
                offsetY = transformation.offset.y,
                rotation = transformation.rotation,
                applier = wallpaperApplier
            )
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
