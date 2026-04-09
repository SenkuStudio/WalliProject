package com.walli.wallpaper.preview

import com.walli.wallpaper.domain.model.Wallpaper
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PreviewSession(
    val items: List<Wallpaper> = emptyList(),
    val initialIndex: Int = 0,
    val source: String = "home",
)

@Singleton
class PreviewSessionManager @Inject constructor() {
    private val _session = MutableStateFlow(PreviewSession())
    val session: StateFlow<PreviewSession> = _session.asStateFlow()

    fun open(items: List<Wallpaper>, initialIndex: Int, source: String) {
        _session.value = PreviewSession(items = items, initialIndex = initialIndex, source = source)
    }
}
