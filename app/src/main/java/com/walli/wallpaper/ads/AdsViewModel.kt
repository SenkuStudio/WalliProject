package com.walli.wallpaper.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdsViewModel @Inject constructor(
    private val adMobManager: AdMobManager,
    private val unityAdsManager: UnityAdsManager,
) : ViewModel() {

    private val _isSdkInitialized = MutableStateFlow(false)
    val isSdkInitialized = _isSdkInitialized.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady = _isRewardedReady.asStateFlow()

    init {
        adMobManager.warmUp()
        unityAdsManager.initialize()
        observeAdStatus()
    }

    private fun observeAdStatus() {
        viewModelScope.launch {
            unityAdsManager.isInitialized.collect { initialized ->
                _isSdkInitialized.value = initialized
            }
        }
        viewModelScope.launch {
            unityAdsManager.isAdLoaded.collect { isLoaded ->
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
        if (!isRewardedReady.value) {
            // Optional: Show a toast or log that the ad is still loading
            // But for now, we follow the bypass policy if it's not ready
        }
        unityAdsManager.showRewarded(activity, onReward, onDismiss)
    }
}
