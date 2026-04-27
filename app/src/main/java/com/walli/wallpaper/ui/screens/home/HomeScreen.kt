package com.walli.wallpaper.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walli.wallpaper.ads.AdsViewModel
import com.walli.wallpaper.domain.model.WallpaperSort
import com.walli.wallpaper.ui.common.LoadState
import com.walli.wallpaper.ui.components.BannerAd
import com.walli.wallpaper.ui.components.EmptyState
import com.walli.wallpaper.ui.components.FeaturedHeroCard
import com.walli.wallpaper.ui.components.WallpaperCard
import com.walli.wallpaper.ui.components.WallpaperCardShimmer
import com.walli.wallpaper.util.findActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.snapshotFlow

@Composable
fun HomeRoute(
    onOpenPreview: () -> Unit,
    initialCategory: String? = null,
    viewModel: HomeViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    LaunchedEffect(initialCategory) {
        if (initialCategory != null) {
            viewModel.selectCategory(com.walli.wallpaper.domain.model.WallpaperCategory(initialCategory))
        }
    }

    HomeScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onSortSelected = viewModel::changeSort,
        onCategorySelected = viewModel::selectCategory,
        onWallpaperClick = { index ->
            adsViewModel.maybeShowOpenInterstitial(activity) {
                viewModel.openPreview(index)
                onOpenPreview()
            }
        },
    )
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSortSelected: (WallpaperSort) -> Unit,
    onCategorySelected: (com.walli.wallpaper.domain.model.WallpaperCategory) -> Unit,
    onWallpaperClick: (Int) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val pullState = rememberPullToRefreshState()

    ObserveGridPagination(
        gridState = gridState,
        totalCount = state.wallpapers.size,
        hasNext = state.hasNext,
        onLoadMore = onLoadMore,
    )

    Scaffold(
        topBar = {
            HomeTopBar()
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            state.loadState is LoadState.Error && state.wallpapers.isEmpty() -> {
                val message = (state.loadState as LoadState.Error).message
                EmptyState(
                    title = "Couldn’t load wallpapers",
                    subtitle = message,
                    actionText = "Retry",
                    onAction = onRefresh,
                )
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = state.loadState is LoadState.Refreshing,
                    onRefresh = onRefresh,
                    state = pullState,
                    modifier = Modifier.padding(padding)
                ) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (state.wallpapers.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                FeaturedHeroCard(
                                    wallpaper = state.wallpapers.first(),
                                    onClick = { onWallpaperClick(0) },
                                )
                            }
                        }

                        if (state.recentWallpapers.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        text = "Continue Exploring",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(state.recentWallpapers.take(8)) { wallpaper ->
                                            Box(modifier = Modifier.width(140.dp)) {
                                                WallpaperCard(
                                                    wallpaper = wallpaper,
                                                    onClick = {
                                                        val index = state.wallpapers.indexOfFirst { it.id == wallpaper.id }
                                                        onWallpaperClick(index.coerceAtLeast(0))
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Explore",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 4.dp)
                                ) {
                                    items(state.categories) { category ->
                                        FilterChip(
                                            selected = state.selectedCategory == category.name,
                                            onClick = { onCategorySelected(category) },
                                            label = { Text(category.name) },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(WallpaperSort.entries) { sort ->
                                        FilterChip(
                                            selected = state.sort == sort,
                                            onClick = { onSortSelected(sort) },
                                            label = { Text(sort.label) },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        when {
                            state.loadState is LoadState.Loading && state.wallpapers.isEmpty() -> {
                                items(8) {
                                    WallpaperCardShimmer()
                                }
                            }

                            state.loadState is LoadState.Empty -> {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyState(
                                        title = "No results",
                                        subtitle = "Try a different keyword, sort, or category.",
                                        modifier = Modifier.height(280.dp),
                                    )
                                }
                            }

                            else -> {
                                itemsIndexed(state.wallpapers, key = { _, item -> item.id }) { index, wallpaper ->
                                    WallpaperCard(
                                        wallpaper = wallpaper,
                                        onClick = { onWallpaperClick(index) },
                                    )
                                }
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            if (state.loadState is LoadState.Appending || state.wallpapers.isNotEmpty()) {
                                BannerAd(modifier = Modifier.padding(top = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Walli",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "App",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun ObserveGridPagination(
    gridState: LazyGridState,
    totalCount: Int,
    hasNext: Boolean,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(gridState, totalCount, hasNext) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { lastVisible -> hasNext && totalCount > 0 && lastVisible >= totalCount - 4 }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}
