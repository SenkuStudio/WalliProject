package com.walli.wallpaper.ui.screens.category_wallpapers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walli.wallpaper.ads.AdsViewModel
import com.walli.wallpaper.domain.model.WallpaperCategory
import com.walli.wallpaper.ui.common.LoadState
import com.walli.wallpaper.ui.components.EmptyState
import com.walli.wallpaper.ui.components.WallpaperCard
import com.walli.wallpaper.ui.components.WallpaperCardShimmer
import com.walli.wallpaper.ui.screens.home.HomeUiState
import com.walli.wallpaper.ui.screens.home.HomeViewModel
import com.walli.wallpaper.util.findActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryWallpapersRoute(
    categoryName: String,
    onBack: () -> Unit,
    onOpenPreview: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    LaunchedEffect(categoryName) {
        viewModel.selectCategory(WallpaperCategory(categoryName))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        CategoryWallpapersScreen(
            modifier = Modifier.padding(padding),
            state = state,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onWallpaperClick = { index ->
                adsViewModel.maybeShowOpenInterstitial(activity) {
                    viewModel.openPreview(index)
                    onOpenPreview()
                }
            },
        )
    }
}

@Composable
private fun CategoryWallpapersScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onWallpaperClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(gridState, state.wallpapers.size, state.hasNext) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { lastVisible -> state.hasNext && state.wallpapers.isNotEmpty() && lastVisible >= state.wallpapers.size - 4 }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    PullToRefreshBox(
        isRefreshing = state.loadState is LoadState.Refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.loadState is LoadState.Loading && state.wallpapers.isEmpty()) {
                items(8) {
                    WallpaperCardShimmer()
                }
            } else if (state.wallpapers.isEmpty() && state.loadState is LoadState.Idle) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "No wallpapers found",
                        subtitle = "This category seems to be empty.",
                    )
                }
            } else {
                itemsIndexed(state.wallpapers, key = { _, item -> item.id }) { index, wallpaper ->
                    WallpaperCard(
                        wallpaper = wallpaper,
                        onClick = { onWallpaperClick(index) },
                    )
                }

                if (state.loadState is LoadState.Appending) {
                    items(2) {
                        WallpaperCardShimmer()
                    }
                }
            }
        }
    }
}
