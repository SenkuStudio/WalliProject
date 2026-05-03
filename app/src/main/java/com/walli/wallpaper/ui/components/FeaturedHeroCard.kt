package com.walli.wallpaper.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.walli.wallpaper.domain.model.Wallpaper

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun FeaturedHeroCard(
    wallpaper: Wallpaper,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(32.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageModifier = Modifier.fillMaxSize()
            val sharedImageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    imageModifier.sharedElement(
                        rememberSharedContentState(key = "image-${wallpaper.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            } else {
                imageModifier
            }

            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = sharedImageModifier,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.8f),
                            ),
                            startY = 100f
                        ),
                    ),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp
            ) {
                Text(
                    text = "Featured Selection",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp),
            ) {
                Text(
                    text = wallpaper.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = wallpaper.category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
