package com.walli.wallpaper.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.walli.wallpaper.BuildConfig

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val width = LocalConfiguration.current.screenWidthDp

    AndroidView(
        modifier = modifier,
        factory = {
            AdView(it).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(it, width))
                loadAd(AdRequest.Builder().build())
            }
        },
        update = {
            // Ad size can only be set once on AdView.
            // If you need to change the ad size, you should recreate the AdView.
        },
    )
}
