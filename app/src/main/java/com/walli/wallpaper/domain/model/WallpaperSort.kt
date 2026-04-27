package com.walli.wallpaper.domain.model

enum class WallpaperSort(val label: String, val apiValue: String) {
    LATEST(label = "Latest", apiValue = "latest"),
    POPULAR(label = "Popular", apiValue = "popular"),
    TRENDING(label = "Trending", apiValue = "trending"),
    RANDOM(label = "Random", apiValue = "random"),
}
