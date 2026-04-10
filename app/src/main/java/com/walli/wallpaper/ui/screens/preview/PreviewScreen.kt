@file:OptIn(ExperimentalMaterial3Api::class)

package com.walli.wallpaper.ui.screens.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.walli.wallpaper.ads.AdsViewModel
import com.walli.wallpaper.domain.model.WallpaperTarget
import com.walli.wallpaper.ui.components.EmptyState
import com.walli.wallpaper.util.findActivity
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PreviewRoute(
    onBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context.findActivity()
    var showSetSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    if (state.items.isEmpty()) {
        EmptyState(
            title = "Nothing to preview",
            subtitle = "Open a wallpaper from Home or Favorites first.",
            actionText = "Go back",
            onAction = onBack,
        )
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.initialIndex.coerceIn(0, state.items.lastIndex),
        pageCount = { 
            if (state.items.isEmpty()) 0 else Int.MAX_VALUE 
        },
    )

    LaunchedEffect(pagerState, state.items) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            if (state.items.isEmpty()) return@collectLatest
            val actualPage = page % state.items.size
            viewModel.onPageSettled(actualPage)
            preloadAdjacentImages(
                context = context,
                urls = listOfNotNull(
                    state.items.getOrNull((actualPage - 1 + state.items.size) % state.items.size)?.imageUrl,
                    state.items.getOrNull((actualPage + 1) % state.items.size)?.imageUrl,
                ),
            )
        }
    }

    if (showSetSheet) {
        ModalBottomSheet(onDismissRequest = { showSetSheet = false }) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WallpaperTarget.entries.forEach { target ->
                    FilledTonalButton(
                        onClick = {
                            showSetSheet = false
                            val actualPage = pagerState.currentPage % state.items.size
                            val action = { viewModel.applyWallpaper(actualPage, target) }
                            val current = state.items[actualPage]
                            if (current.isPremium) {
                                adsViewModel.showRewarded(activity, onReward = action)
                            } else {
                                action()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        Text(target.label)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Black,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                if (state.items.isEmpty()) return@HorizontalPager
                val actualPage = page % state.items.size
                val wallpaper = state.items[actualPage]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = viewModel::toggleControls,
                        ),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(wallpaper.imageUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = wallpaper.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.72f),
                                    ),
                                ),
                            ),
                    )
                }
            }

            AnimatedVisibility(
                visible = state.controlsVisible,
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                if (state.items.isEmpty()) return@AnimatedVisibility
                val actualPage = pagerState.currentPage % state.items.size
                val current = state.items[actualPage]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledIconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = current.category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                    ) {
                        Text(
                            text = "${actualPage + 1}/${state.items.size}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color.White,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = state.controlsVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                if (state.items.isEmpty()) return@AnimatedVisibility
                val actualPage = pagerState.currentPage % state.items.size
                val current = state.items[actualPage]
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = Color.Black.copy(alpha = 0.38f),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (current.isPremium) {
                            Text(
                                text = "Premium wallpaper · unlock with rewarded ad",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                        
                        val screenWidth = LocalConfiguration.current.screenWidthDp
                        val showLabels = screenWidth > 360

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val action = { viewModel.download(actualPage) }
                                    if (current.isPremium) {
                                        adsViewModel.showRewarded(activity, onReward = action)
                                    } else {
                                        adsViewModel.maybeShowDownloadInterstitial(activity, action)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = if (showLabels) 12.dp else 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "Download",
                                    modifier = Modifier.size(20.dp)
                                )
                                if (showLabels) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Download",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { showSetSheet = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = if (showLabels) 12.dp else 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Wallpaper,
                                    contentDescription = "Set",
                                    modifier = Modifier.size(20.dp)
                                )
                                if (showLabels) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Set",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }
                            }
                            FilledIconButton(
                                onClick = { viewModel.toggleFavorite(actualPage) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (current.isFavorite) {
                                        Icons.Rounded.Favorite
                                    } else {
                                        Icons.Rounded.FavoriteBorder
                                    },
                                    contentDescription = "Favorite",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            FilledIconButton(
                                onClick = { viewModel.share(actualPage) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${current.downloads} downloads",
                                color = Color.White.copy(alpha = 0.92f),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Tap image to hide controls",
                                    color = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (state.isWorking) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(state.workingLabel ?: "Working…", color = Color.White)
                    }
                }
            }
        }
    }
}

private fun preloadAdjacentImages(context: android.content.Context, urls: List<String>) {
    val loader = SingletonImageLoader.get(context)
    urls.forEach { url ->
        loader.enqueue(
            ImageRequest.Builder(context)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
        )
    }
}
