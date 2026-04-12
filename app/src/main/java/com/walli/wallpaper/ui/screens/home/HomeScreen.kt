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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    viewModel: HomeViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    HomeScreen(
        state = state,
        onQueryChange = viewModel::updateQuery,
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
    onQueryChange: (String) -> Unit,
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
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        HomeHeader(query = state.query, onQueryChange = onQueryChange)
                    }

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
                            Column {
                                Text(
                                    text = "Recently viewed",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(state.recentWallpapers.take(8)) { wallpaper ->
                                        Box(modifier = Modifier.width(156.dp)) {
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
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.categories) { category ->
                                FilterChip(
                                    selected = state.selectedCategory == category.name,
                                    onClick = { onCategorySelected(category) },
                                    label = { Text(category.name) },
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(WallpaperSort.entries) { sort ->
                                FilterChip(
                                    selected = state.sort == sort,
                                    onClick = { onSortSelected(sort) },
                                    label = { Text(sort.label) },
                                )
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
                            BannerAd(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
                    .padding(bottom = 8.dp),
    ) {
        Text(
            text = "Discover premium wallpapers",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search mountains, AMOLED, abstract…") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )
        }
    }
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
