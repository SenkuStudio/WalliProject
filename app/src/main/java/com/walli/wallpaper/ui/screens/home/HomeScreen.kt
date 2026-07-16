package com.walli.wallpaper.ui.screens.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walli.wallpaper.R
import com.walli.wallpaper.ads.AdsViewModel
import androidx.compose.ui.platform.LocalConfiguration
import com.walli.wallpaper.domain.model.WallpaperSort
import com.walli.wallpaper.ui.common.LoadState
import com.walli.wallpaper.ui.components.BannerAd
import com.walli.wallpaper.ui.components.EmptyState
import com.walli.wallpaper.ui.components.FeaturedHeroCard
import com.walli.wallpaper.ui.components.NoInternetState
import com.walli.wallpaper.ui.components.WallpaperCard
import com.walli.wallpaper.ui.components.WallpaperCardShimmer
import com.walli.wallpaper.ui.components.UnlockPremiumDialog
import com.walli.wallpaper.util.findActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun HomeRoute(
    onOpenPreview: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    initialCategoryId: Int? = null,
    viewModel: HomeViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()

    LaunchedEffect(initialCategoryId) {
        if (initialCategoryId != null) {
            viewModel.selectCategory(com.walli.wallpaper.domain.model.WallpaperCategory(id = initialCategoryId))
        }
    }

    val onWallpaperClick: (Int) -> Unit = { index ->
        val wallpaper = state.wallpapers.getOrNull(index)
        if (wallpaper != null && wallpaper.isPremium && !wallpaper.isUnlocked) {
            viewModel.onWallpaperClick(index)
        } else {
            adsViewModel.maybeShowOpenInterstitial(activity) {
                viewModel.openPreview(index)
                onOpenPreview()
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onItemClick = { item: DrawerItem ->
                    scope.launch { drawerState.close() }
                    handleDrawerAction(context, item)
                }
            )
        }
    ) {
        HomeScreen(
            state = state,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onSortSelected = viewModel::changeSort,
            onCategorySelected = viewModel::selectCategory,
            onWallpaperClick = onWallpaperClick,
            onMenuClick = { scope.launch { drawerState.open() } }
        )
    }

    state.wallpaperToUnlock?.let { wallpaper ->
        UnlockPremiumDialog(
            wallpaper = wallpaper,
            onDismiss = viewModel::dismissUnlockDialog,
            onUnlock = {
                adsViewModel.showRewarded(
                    activity = activity,
                    onReward = {
                        viewModel.unlockWallpaper(wallpaper)
                        onOpenPreview()
                    }
                )
            }
        )
    }
}

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun HomeScreen(
    state: HomeUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSortSelected: (WallpaperSort) -> Unit,
    onCategorySelected: (com.walli.wallpaper.domain.model.WallpaperCategory) -> Unit,
    onWallpaperClick: (Int) -> Unit,
    onMenuClick: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val pullState = rememberPullToRefreshState()
    val configuration = LocalConfiguration.current
    val columns = when {
        configuration.screenWidthDp >= 900 -> 4
        configuration.screenWidthDp >= 600 -> 3
        else -> 2
    }

    ObserveGridPagination(
        gridState = gridState,
        totalCount = state.wallpapers.size,
        hasNext = state.hasNext,
        onLoadMore = onLoadMore,
    )

    Scaffold(
        topBar = {
            HomeTopBar(onMenuClick = onMenuClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!state.isOnline) {
            NoInternetState(
                modifier = Modifier.padding(padding),
                onRetry = onRefresh
            )
        } else {
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
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 100.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (state.wallpapers.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    FeaturedHeroCard(
                                        wallpaper = state.wallpapers.first(),
                                        onClick = { onWallpaperClick(0) },
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            } else if (state.loadState is LoadState.Loading) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    com.walli.wallpaper.ui.components.FeaturedHeroShimmer()
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
                                                            val index =
                                                                state.wallpapers.indexOfFirst { it.id == wallpaper.id }
                                                            onWallpaperClick(index.coerceAtLeast(0))
                                                        },
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        animatedVisibilityScope = animatedVisibilityScope
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
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        items(state.categories) { category ->
                                            FilterChip(
                                                selected = state.selectedCategoryId == category.id,
                                                onClick = { onCategorySelected(category) },
                                                label = { 
                                                    Text(
                                                        category.name.orEmpty(),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    ) 
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        items(WallpaperSort.entries) { sort ->
                                            FilterChip(
                                                selected = state.sort == sort,
                                                onClick = { onSortSelected(sort) },
                                                label = { 
                                                    Text(
                                                        sort.label,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    ) 
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            when {
                                state.loadState is LoadState.Loading && state.wallpapers.isEmpty() -> {
                                    items(6) {
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
                                    itemsIndexed(
                                        state.wallpapers,
                                        key = { _, item -> item.id },
                                        contentType = { _, _ -> "wallpaper" }
                                    ) { index, wallpaper ->
                                        WallpaperCard(
                                            wallpaper = wallpaper,
                                            onClick = { onWallpaperClick(index) },
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }

                                    // Add shimmers at the bottom when loading more (Appending)
                                    if (state.loadState is LoadState.Appending) {
                                        items(2) {
                                            WallpaperCardShimmer()
                                        }
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
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onMenuClick: () -> Unit) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Rounded.Menu, contentDescription = "Menu")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logoapp),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Walli",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

private enum class DrawerItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HowToUse("How to use", Icons.Rounded.Info),
    ShareApp("Share app", Icons.Rounded.Share),
    RateUs("Rate us", Icons.Rounded.Star),
    MoreApps("More apps", Icons.Rounded.Apps),
    ContactUs("Contact us", Icons.Rounded.Email),
    PrivacyPolicy("Privacy policy", Icons.Rounded.Description)
}

@Composable
private fun DrawerContent(onItemClick: (DrawerItem) -> Unit) {
    ModalDrawerSheet {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logoapp),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Walli",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        DrawerItem.entries.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                selected = false,
                onClick = { onItemClick(item) },
                icon = { Icon(item.icon, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun handleDrawerAction(context: android.content.Context, item: DrawerItem) {
    when (item) {
        DrawerItem.HowToUse -> {
            Toast.makeText(context, "Welcome to Walli! Swipe to explore and tap to preview wallpapers.", Toast.LENGTH_LONG).show()
        }
        DrawerItem.ShareApp -> {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out Walli App")
                putExtra(Intent.EXTRA_TEXT, "Download Walli for amazing wallpapers: https://play.google.com/store/apps/details?id=${context.packageName}")
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        }
        DrawerItem.RateUs -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
            }
        }
        DrawerItem.MoreApps -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=Walli+Team"))
            context.startActivity(intent)
        }
        DrawerItem.ContactUs -> {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@walliapp.com")
                putExtra(Intent.EXTRA_SUBJECT, "Walli App Feedback")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
        DrawerItem.PrivacyPolicy -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://walliapp.com/privacy-policy"))
            context.startActivity(intent)
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
