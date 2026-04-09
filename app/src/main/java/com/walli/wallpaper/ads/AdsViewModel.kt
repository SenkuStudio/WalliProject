package com.walli.wallpaper.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AdsViewModel @Inject constructor(
    private val adMobManager: AdMobManager,
) : ViewModel() {

    init {
        adMobManager.warmUp()
    }

    fun maybeShowOpenInterstitial(activity: Activity?, onContinue: () -> Unit) {
        adMobManager.maybeShowOpenInterstitial(activity, onContinue)
    }

    fun maybeShowDownloadInterstitial(activity: Activity?, onContinue: () -> Unit) {
        adMobManager.maybeShowDownloadInterstitial(activity, onContinue)
    }

    fun showRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onDismiss: () -> Unit = {},
    ) {
        adMobManager.showRewarded(activity, onReward, onDismiss)
    }
}
