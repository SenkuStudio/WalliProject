package com.walli.wallpaper.ui.common

sealed interface LoadState {
    data object Idle : LoadState
    data object Loading : LoadState
    data object Refreshing : LoadState
    data object Appending : LoadState
    data object Empty : LoadState
    data class Error(val message: String) : LoadState
}
