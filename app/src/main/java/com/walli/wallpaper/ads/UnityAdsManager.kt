package com.walli.wallpaper.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import com.walli.wallpaper.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnityAdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val tag = "UnityAdsManager"
    private val gameId = "1486551"
    private val rewardedPlacementId = "rewardedVideo"
    
    private val testMode = BuildConfig.DEBUG

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    private var isInitializing = false
    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded = _isAdLoaded.asStateFlow()
    
    private var retryCount = 0

    fun initialize() {
        if (UnityAds.isInitialized) {
            _isInitialized.value = true
            if (!_isAdLoaded.value) loadRewarded()
            return
        }

        if (isInitializing) {
            Log.d(tag, "Unity Ads initialization already in progress...")
            return
        }
        
        isInitializing = true
        Log.d(tag, "Initializing Unity Ads with Game ID: $gameId, Package: ${context.packageName}")
        UnityAds.initialize(context, gameId, testMode, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d(tag, "Unity Ads Initialization Complete")
                _isInitialized.value = true
                isInitializing = false
                loadRewarded()
            }

            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                Log.e(tag, "Unity Ads Initialization Failed: [$error] $message")
                _isInitialized.value = false
                isInitializing = false
            }
        })
    }

    fun showRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onDismiss: () -> Unit = {},
    ) {
        if (activity == null) {
            onReward()
            onDismiss()
            return
        }

        if (!UnityAds.isInitialized) {
            if (isInitializing) {
                Log.w(tag, "Unity Ads initialization in progress, bypassing to grant reward")
            } else {
                Log.w(tag, "Unity Ads not initialized, attempting init and bypassing")
                initialize()
            }
            onReward()
            onDismiss()
            return
        }

        if (!_isAdLoaded.value) {
            Log.w(tag, "Ad not loaded for $rewardedPlacementId, fetching now and bypassing")
            loadRewarded()
            onReward()
            onDismiss()
            return
        }

        Log.d(tag, "Showing rewarded ad: $rewardedPlacementId")
        UnityAds.show(activity, rewardedPlacementId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                Log.e(tag, "Unity Ads Show Failure: [$error] $message")
                _isAdLoaded.value = false
                onReward() 
                onDismiss()
                loadRewarded()
            }

            override fun onUnityAdsShowStart(placementId: String?) {}
            override fun onUnityAdsShowClick(placementId: String?) {}

            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                Log.d(tag, "Unity Ads Show Complete: $state")
                _isAdLoaded.value = false
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    onReward()
                }
                onDismiss()
                loadRewarded()
            }
        })
    }

    private fun loadRewarded() {
        if (!UnityAds.isInitialized) {
            Log.w(tag, "Cannot load ad: Unity Ads not initialized")
            return
        }
        
        Log.d(tag, "Loading ad for placement: $rewardedPlacementId")
        UnityAds.load(rewardedPlacementId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                Log.d(tag, "Unity Ad Loaded Successfully: $placementId")
                _isAdLoaded.value = true
                retryCount = 0
            }

            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                Log.e(tag, "Unity Ad Failed to Load: [$error] $message")
                _isAdLoaded.value = false
                
                // Retry logic for network errors
                if (retryCount < 5) {
                    retryCount++
                    val delayMillis = (5000 * retryCount).toLong()
                    Log.d(tag, "Retrying load ($retryCount/5) in ${delayMillis/1000}s...")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadRewarded()
                    }, delayMillis)
                }
            }
        })
    }
}
