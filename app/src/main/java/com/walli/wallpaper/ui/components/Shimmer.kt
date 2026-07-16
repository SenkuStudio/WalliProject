package com.walli.wallpaper.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A reusable shimmer brush that can be applied to any background.
 */
@Composable
fun rememberShimmerBrush(
    targetValue: Float = 1000f,
    durationMillis: Int = 1200,
    shimmerColors: List<Color> = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.2f),
        Color.White.copy(alpha = 0.05f),
    )
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

/**
 * Skeleton shimmer for basic shapes.
 */
fun Modifier.shimmerEffect(
    shape: RoundedCornerShape = RoundedCornerShape(0.dp)
): Modifier = composed {
    val brush = rememberShimmerBrush(
        shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.5f),
            Color.LightGray.copy(alpha = 0.3f),
        )
    )
    this.clip(shape).background(brush)
}

@Composable
fun WallpaperCardShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.68f) // Match WallpaperCard aspect ratio
            .shimmerEffect(RoundedCornerShape(24.dp))
    )
}

@Composable
fun FeaturedHeroShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp) // Match FeaturedHeroCard height
            .shimmerEffect(RoundedCornerShape(32.dp))
    )
}

@Composable
fun CategoryCardShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(21f / 9f)
            .shimmerEffect(RoundedCornerShape(24.dp))
    )
}
