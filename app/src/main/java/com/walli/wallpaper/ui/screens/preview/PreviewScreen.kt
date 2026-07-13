@file:OptIn(ExperimentalMaterial3Api::class)

package com.walli.wallpaper.ui.screens.preview

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.asDrawable
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.walli.wallpaper.ads.AdsViewModel
import com.walli.wallpaper.domain.model.WallpaperTarget
import com.walli.wallpaper.ui.components.NoInternetState
import com.walli.wallpaper.ui.components.EmptyState
import com.walli.wallpaper.ui.components.PremiumLoader
import com.walli.wallpaper.util.findActivity
import kotlinx.coroutines.flow.collectLatest

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun PreviewRoute(
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: PreviewViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context.findActivity()
    var showSetSheet by remember { mutableStateOf(false) }

    // Color Palette state
    var dominantColor by remember { mutableStateOf(Color.Black) }
    var onDominantColor by remember { mutableStateOf(Color.White) }

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
            val imageUrl = state.items[actualPage].imageUrl
            
            viewModel.onPageSettled(actualPage)
            
            // Extract Palette from the current image
            val loader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // Required to convert to bitmap
                .build()
            
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.image.asDrawable(context.resources).toBitmap()
                Palette.from(bitmap).generate { palette ->
                    palette?.let {
                        val swatch = it.vibrantSwatch ?: it.dominantSwatch
                        swatch?.let { s ->
                            dominantColor = Color(s.rgb)
                            onDominantColor = Color(s.titleTextColor)
                        }
                    }
                }
            }

            preloadAdjacentImages(
                context = context,
                urls = listOfNotNull(
                    state.items.getOrNull((actualPage - 1 + state.items.size) % state.items.size)?.imageUrl,
                    state.items.getOrNull((actualPage + 1) % state.items.size)?.imageUrl,
                ),
            )
        }
    }

    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 600),
        label = "dominantColor"
    )

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (!state.isOnline) {
            NoInternetState(
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                onRetry = null // Network monitor handles it
            )
        } else {
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
                    val imageModifier = Modifier.fillMaxSize()
                    val sharedImageModifier = if (page % state.items.size == state.initialIndex) {
                        with(sharedTransitionScope) {
                            imageModifier.sharedElement(
                                rememberSharedContentState(key = "image-${wallpaper.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else {
                        imageModifier
                    }

                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(wallpaper.imageUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = wallpaper.title,
                        modifier = sharedImageModifier,
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PremiumLoader(
                                    isPremium = wallpaper.isPremium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f),
                                    ),
                                ),
                            ),
                    )
                }
            }

            // Top Controls
            AnimatedVisibility(
                visible = state.controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                if (state.items.isNotEmpty()) {
                    val actualPage = pagerState.currentPage % state.items.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        FilledIconButton(
                            onClick = onBack,
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = animatedDominantColor.copy(alpha = 0.95f),
                                contentColor = onDominantColor
                            )
                        ) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = animatedDominantColor.copy(alpha = 0.95f),
                            tonalElevation = 4.dp
                        ) {
                            Text(
                                text = "${actualPage + 1}/${state.items.size}",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = onDominantColor,
                            )
                        }
                    }
                }
            }

            // Bottom Controls
            AnimatedVisibility(
                visible = state.controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                if (state.items.isNotEmpty()) {
                    val actualPage = pagerState.currentPage % state.items.size
                    val current = state.items[actualPage]
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = animatedDominantColor.copy(alpha = 0.95f),
                        tonalElevation = 12.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (current.isPremium) {
                                Text(
                                    text = "Premium wallpaper · unlock with rewarded ad",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = onDominantColor,
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
                                    contentPadding = PaddingValues(horizontal = if (showLabels) 12.dp else 8.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                        containerColor = onDominantColor.copy(alpha = 0.15f),
                                        contentColor = onDominantColor
                                    )
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
                                            overflow = TextOverflow.Ellipsis, softWrap = false
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { showSetSheet = true },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = if (showLabels) 12.dp else 8.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                        containerColor = onDominantColor.copy(alpha = 0.15f),
                                        contentColor = onDominantColor
                                    )
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
                                            overflow = TextOverflow.Ellipsis, softWrap = false
                                        )
                                    }
                                }
                                FilledIconButton(
                                    onClick = { viewModel.toggleFavorite(actualPage) },
                                    modifier = Modifier.size(44.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = onDominantColor.copy(alpha = 0.15f),
                                        contentColor = onDominantColor
                                    )
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
                                    modifier = Modifier.size(44.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = onDominantColor.copy(alpha = 0.15f),
                                        contentColor = onDominantColor
                                    )
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
                                    color = onDominantColor.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = onDominantColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "Tap image to hide controls",
                                        color = onDominantColor.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(start = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.isWorking) {
                val currentWallpaper = state.items.getOrNull(pagerState.currentPage % state.items.size)
                val isPremiumAction = currentWallpaper?.isPremium == true
                val accentColor = if (isPremiumAction) Color(0xFFFFD700) else onDominantColor

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = animatedDominantColor.copy(alpha = 0.95f),
                        tonalElevation = 16.dp,
                        shadowElevation = 24.dp,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 48.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            PremiumLoader(
                                isPremium = isPremiumAction,
                                color = onDominantColor,
                                label = null // Just the spinner here
                            )
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.workingLabel ?: "Processing…",
                                    color = onDominantColor,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                if (isPremiumAction) {
                                    Text(
                                        text = "Unlocking Premium",
                                        color = accentColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            
                            // "Progressing" feel with a sleek linear bar
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(4.dp),
                                color = accentColor,
                                trackColor = accentColor.copy(alpha = 0.2f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        )
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
