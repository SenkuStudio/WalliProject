package com.walli.wallpaper.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdsViewModel @Inject constructor(
    private val adMobManager: AdMobManager,
) : ViewModel() {

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady = _isRewardedReady.asStateFlow()

    init {
        adMobManager.warmUp()
        observeAdStatus()
    }

    private fun observeAdStatus() {
        viewModelScope.launch {
            adMobManager.isRewardedLoaded.collect { isLoaded ->
                _isRewardedReady.value = isLoaded
            }
        }
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
