package com.walli.wallpaper.ui.screens.categories

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.walli.wallpaper.BuildConfig
import com.walli.wallpaper.domain.model.WallpaperCategory
import com.walli.wallpaper.ui.common.LoadState
import com.walli.wallpaper.ui.components.BannerAd
import com.walli.wallpaper.ui.components.EmptyState
import com.walli.wallpaper.ui.components.NoInternetState
import com.walli.wallpaper.ui.components.rememberShimmerBrush

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoriesRoute(
    onCategoryClick: (WallpaperCategory) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Krishna Wallpaper 4k",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!state.isOnline && state.categories.isEmpty()) {
            NoInternetState(
                modifier = Modifier.padding(padding),
                onRetry = { /* Managed by NetworkMonitor */ }
            )
        } else {
            when (state.loadState) {
                is LoadState.Loading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(6) {
                            com.walli.wallpaper.ui.components.CategoryCardShimmer()
                        }
                    }
                }
                is LoadState.Error -> EmptyState(
                    title = "Categories unavailable",
                    subtitle = (state.loadState as LoadState.Error).message,
                    modifier = Modifier.padding(padding)
                )
                LoadState.Empty -> EmptyState(
                    title = "No categories",
                    subtitle = "Add categories in your Cloudflare dataset and they’ll appear here.",
                    modifier = Modifier.padding(padding)
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.categories) { category ->
                        val fallbackCover = state.categories.firstOrNull { it.coverUrl != null }?.coverUrl
                        CategoryCard(
                            name = category.name ?: "",
                            coverUrl = category.coverUrl ?: fallbackCover,
                            onClick = { onCategoryClick(category) }
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        if (state.categories.isNotEmpty()) {
                            BannerAd(
                                adUnitId = BuildConfig.ADMOB_BANNER_CATEGORIES,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    name: String,
    coverUrl: String?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(21f / 9f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverUrl != null) {
                val shimmerBrush = rememberShimmerBrush()
                Box(modifier = Modifier.fillMaxSize()) {
                    // Shimmer Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(shimmerBrush)
                    )
                    // Image Layer
                    coil3.compose.AsyncImage(
                        model = coverUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 100f
                        )
                    )
            )
            
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}
