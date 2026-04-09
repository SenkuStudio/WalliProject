package com.walli.wallpaper.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walli.wallpaper.domain.model.WallpaperCategory
import com.walli.wallpaper.domain.usecase.GetCategoriesUseCase
import com.walli.wallpaper.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<WallpaperCategory> = emptyList(),
    val loadState: LoadState = LoadState.Loading,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCategoriesUseCase()
                .onSuccess { categories ->
                    _uiState.update {
                        it.copy(
                            categories = categories,
                            loadState = if (categories.isEmpty()) LoadState.Empty else LoadState.Idle,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(loadState = LoadState.Error(throwable.message ?: "Failed"))
                    }
                }
        }
    }
}
