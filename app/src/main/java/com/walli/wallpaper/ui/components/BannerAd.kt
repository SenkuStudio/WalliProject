package com.walli.wallpaper.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    
    // Calculate the adaptive ad size
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }

    // Convert height from pixels to Dp using the current density
    val heightDp = remember(adSize) {
        (adSize.getHeightInPixels(context) / context.resources.displayMetrics.density).dp
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp),
        factory = { ctx ->
            AdView(ctx).apply {
                this.adUnitId = adUnitId
                setAdSize(adSize)
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { _ ->
            // Size is set in factory and modifier, no dynamic updates needed
        }
    )
}
