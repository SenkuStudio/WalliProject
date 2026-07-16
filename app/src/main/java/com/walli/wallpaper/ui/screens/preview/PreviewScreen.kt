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
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.palette.graphics.Palette
import com.walli.wallpaper.util.blurhash.BlurhashDecoder
import coil3.SingletonImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.asDrawable
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import com.walli.wallpaper.ads.AdsViewModel
import com.walli.wallpaper.domain.model.WallpaperTarget
import com.walli.wallpaper.ui.components.NoInternetState
import com.walli.wallpaper.ui.components.EmptyState
import com.walli.wallpaper.ui.components.PremiumLoader
import com.walli.wallpaper.util.findActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom

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
    val scope = androidx.compose.runtime.rememberCoroutineScope()
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
            state.items.size 
        },
    )

    LaunchedEffect(pagerState, state.items) {
        snapshotFlow { pagerState.settledPage }.collectLatest { actualPage ->
            if (state.items.isEmpty()) return@collectLatest
            val imageUrl = state.items[actualPage].imageUrl
            
            viewModel.onPageSettled(actualPage)
            
            // Extract Palette from the current image
            val loader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(128, 128) // Smaller size is sufficient for Palette extraction
                .allowHardware(false) // Required to convert to bitmap
                .build()
            
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.image.asDrawable(context.resources).toBitmap()
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap).maximumColorCount(16).generate()
                }
                val swatch = palette.vibrantSwatch ?: palette.dominantSwatch
                swatch?.let { s ->
                    dominantColor = Color(s.rgb)
                    onDominantColor = Color(s.titleTextColor)
                }
            }

            preloadAdjacentImages(
                context = context,
                urls = listOfNotNull(
                    state.items.getOrNull(actualPage - 1)?.imageUrl,
                    state.items.getOrNull(actualPage + 1)?.imageUrl,
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
                userScrollEnabled = (state.transformations[pagerState.currentPage % state.items.size]?.scale ?: 1f) <= 1f,
                key = { page -> state.items.getOrNull(page % state.items.size)?.id ?: page }
            ) { page ->
                if (state.items.isEmpty()) return@HorizontalPager
                val actualPage = page % state.items.size
                val wallpaper = state.items[actualPage]

                var isLoaded by remember(wallpaper.id) { mutableStateOf(false) }

                val transformation = state.transformations[actualPage] ?: ImageTransformation()
                val currentTransformationState = rememberUpdatedState(transformation)

                val placeholder = remember(wallpaper.blurHash) {
                    wallpaper.blurHash?.let {
                        BlurhashDecoder.decode(it, 4, 6)?.asImageBitmap()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(actualPage) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    viewModel.handleDoubleTap(
                                        index = actualPage,
                                        tapOffset = tapOffset,
                                        center = Offset(size.width / 2f, size.height / 2f)
                                    )
                                },
                                onTap = { viewModel.toggleControls() }
                            )
                        }
                        .pointerInput(actualPage, isLoaded) {
                            if (!isLoaded) return@pointerInput
                            detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, rotation ->
                                val current = currentTransformationState.value
                                val newScale = (current.scale * zoom).coerceIn(0.5f, 5f)
                                
                                val newOffset = if (newScale > 1f) {
                                    // Calculate center-zoom offset adjustment
                                    val pivot = centroid - Offset(size.width / 2f, size.height / 2f)
                                    val scaleChange = newScale / current.scale
                                    val zoomOffset = (current.offset - pivot) * scaleChange + pivot - current.offset
                                    current.offset + pan + zoomOffset
                                } else {
                                    Offset.Zero
                                }
                                
                                viewModel.updateTransformation(
                                    actualPage,
                                    newScale,
                                    newOffset,
                                    current.rotation + rotation
                                )
                            }
                        },
                ) {
                    val imageModifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val currentScale = if (isLoaded) transformation.scale else 1f
                            val currentOffset = if (isLoaded) transformation.offset else Offset.Zero
                            val currentRotation = if (isLoaded) transformation.rotation else 0f
                            
                            scaleX = currentScale
                            scaleY = currentScale
                            translationX = currentOffset.x
                            translationY = currentOffset.y
                            rotationZ = currentRotation
                        }
                    val sharedImageModifier = if (page == state.initialIndex) {
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
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = wallpaper.title,
                        modifier = sharedImageModifier,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                        onSuccess = { isLoaded = true },
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // 1. BlurHash (Static Layer)
                                placeholder?.let { bitmap ->
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        alignment = Alignment.Center
                                    )
                                }
                                
                                // 2. Thumbnail (Instant Layer - Matches Grid Cache)
                                coil3.compose.AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(wallpaper.thumbnailUrl)
                                        .size(320, 480) // Exact same size as WallpaperCard for cache hit
                                        .precision(coil3.size.Precision.INEXACT)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center
                                )

                                PremiumLoader(
                                    isPremium = wallpaper.isPremium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        error = {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = "Error",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Failed to load image",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
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
                        
                        // Reset Button
                        val actualTransformation = state.transformations[actualPage]
                        if (actualTransformation != null && (actualTransformation.scale != 1f || actualTransformation.offset != Offset.Zero || actualTransformation.rotation != 0f)) {
                            FilledTonalButton(
                                onClick = { viewModel.resetTransformation(actualPage) },
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = animatedDominantColor.copy(alpha = 0.95f),
                                    contentColor = onDominantColor
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Reset", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }

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
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    },
                                    enabled = pagerState.currentPage > 0,
                                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = onDominantColor,
                                        disabledContentColor = onDominantColor.copy(alpha = 0.38f)
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                                    Text("Previous", style = MaterialTheme.typography.labelLarge)
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = onDominantColor.copy(alpha = 0.1f),
                                ) {
                                    Text(
                                        text = "${actualPage + 1} / ${state.items.size}",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = onDominantColor,
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    },
                                    enabled = pagerState.currentPage < state.items.size - 1,
                                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = onDominantColor,
                                        disabledContentColor = onDominantColor.copy(alpha = 0.38f)
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Next", style = MaterialTheme.typography.labelLarge)
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val screenWidth = LocalConfiguration.current.screenWidthDp
                            val showLabels = screenWidth > 400

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
                                    modifier = Modifier.weight(1f, fill = showLabels),
                                    contentPadding = PaddingValues(horizontal = if (showLabels) 12.dp else 4.dp),
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
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { showSetSheet = true },
                                    modifier = Modifier.weight(1f, fill = showLabels),
                                    contentPadding = PaddingValues(horizontal = if (showLabels) 12.dp else 4.dp),
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
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false
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
