package com.walli.wallpaper.ads

import android.app.Activity
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.walli.wallpaper.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : DefaultLifecycleObserver {
    private var homeInterstitialAd: InterstitialAd? = null
    private var categoryInterstitialAd: InterstitialAd? = null
    private var downloadInterstitialAd: InterstitialAd? = null

    private var homeRewardedAd: RewardedAd? = null
    private var categoryRewardedAd: RewardedAd? = null
    private var previewRewardedAd: RewardedAd? = null
    
    private val _isRewardedLoaded = MutableStateFlow(false)
    val isRewardedLoaded = _isRewardedLoaded.asStateFlow()

    private var openCounter = 0
    private var isAppInForeground = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
    }

    fun warmUp() {
        if (isAppInForeground) {
            loadAllAds()
        }
    }

    private fun loadAllAds() {
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_HOME) { homeInterstitialAd = it }
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) { categoryInterstitialAd = it }
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_DOWNLOAD) { downloadInterstitialAd = it }

        loadRewarded(BuildConfig.ADMOB_REWARDED_HOME) { homeRewardedAd = it }
        loadRewarded(BuildConfig.ADMOB_REWARDED_CATEGORY) { categoryRewardedAd = it }
        loadRewarded(BuildConfig.ADMOB_REWARDED_PREVIEW) { previewRewardedAd = it }
    }

    fun maybeShowHomeInterstitial(activity: Activity?, onContinue: () -> Unit) {
        openCounter += 1
        if (openCounter % 2 != 0) {
            onContinue()
            return
        }
        showInterstitial(activity, homeInterstitialAd, { homeInterstitialAd = null }, BuildConfig.ADMOB_INTERSTITIAL_HOME, onContinue)
    }

    fun maybeShowCategoryInterstitial(activity: Activity?, onContinue: () -> Unit) {
        openCounter += 1
        if (openCounter % 2 != 0) {
            onContinue()
            return
        }
        showInterstitial(activity, categoryInterstitialAd, { categoryInterstitialAd = null }, BuildConfig.ADMOB_INTERSTITIAL_CATEGORY, onContinue)
    }

    fun maybeShowDownloadInterstitial(activity: Activity?, onContinue: () -> Unit) {
        showInterstitial(activity, downloadInterstitialAd, { downloadInterstitialAd = null }, BuildConfig.ADMOB_INTERSTITIAL_DOWNLOAD, onContinue)
    }

    private fun showInterstitial(
        activity: Activity?,
        ad: InterstitialAd?,
        clearAd: () -> Unit,
        adUnitId: String,
        onContinue: () -> Unit
    ) {
        if (activity == null || ad == null) {
            onContinue()
            if (isAppInForeground) loadInterstitial(adUnitId) { 
                if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_HOME) homeInterstitialAd = it
                else if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) categoryInterstitialAd = it
                else if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_DOWNLOAD) downloadInterstitialAd = it
            }
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                clearAd()
                if (isAppInForeground) loadInterstitial(adUnitId) {
                    if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_HOME) homeInterstitialAd = it
                    else if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) categoryInterstitialAd = it
                    else if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_DOWNLOAD) downloadInterstitialAd = it
                }
                onContinue()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                clearAd()
                if (isAppInForeground) loadInterstitial(adUnitId) {
                    if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_HOME) homeInterstitialAd = it
                    else if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) categoryInterstitialAd = it
                    else if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_DOWNLOAD) downloadInterstitialAd = it
                }
                onContinue()
            }
        }
        ad.show(activity)
    }

    fun showHomeRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}) {
        showRewarded(activity, homeRewardedAd, { homeRewardedAd = null }, BuildConfig.ADMOB_REWARDED_HOME, onReward, onDismiss)
    }

    fun showCategoryRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}) {
        showRewarded(activity, categoryRewardedAd, { categoryRewardedAd = null }, BuildConfig.ADMOB_REWARDED_CATEGORY, onReward, onDismiss)
    }

    fun showPreviewRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}) {
        showRewarded(activity, previewRewardedAd, { previewRewardedAd = null }, BuildConfig.ADMOB_REWARDED_PREVIEW, onReward, onDismiss)
    }

    private fun showRewarded(
        activity: Activity?,
        ad: RewardedAd?,
        clearAd: () -> Unit,
        adUnitId: String,
        onReward: () -> Unit,
        onDismiss: () -> Unit
    ) {
        if (activity == null || ad == null) {
            onReward()
            onDismiss()
            if (isAppInForeground) loadRewarded(adUnitId) {
                if (adUnitId == BuildConfig.ADMOB_REWARDED_HOME) homeRewardedAd = it
                else if (adUnitId == BuildConfig.ADMOB_REWARDED_CATEGORY) categoryRewardedAd = it
                else if (adUnitId == BuildConfig.ADMOB_REWARDED_PREVIEW) previewRewardedAd = it
            }
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                clearAd()
                _isRewardedLoaded.value = false
                if (isAppInForeground) loadRewarded(adUnitId) {
                    if (adUnitId == BuildConfig.ADMOB_REWARDED_HOME) homeRewardedAd = it
                    else if (adUnitId == BuildConfig.ADMOB_REWARDED_CATEGORY) categoryRewardedAd = it
                    else if (adUnitId == BuildConfig.ADMOB_REWARDED_PREVIEW) previewRewardedAd = it
                }
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                clearAd()
                _isRewardedLoaded.value = false
                if (isAppInForeground) loadRewarded(adUnitId) {
                    if (adUnitId == BuildConfig.ADMOB_REWARDED_HOME) homeRewardedAd = it
                    else if (adUnitId == BuildConfig.ADMOB_REWARDED_CATEGORY) categoryRewardedAd = it
                    else if (adUnitId == BuildConfig.ADMOB_REWARDED_PREVIEW) previewRewardedAd = it
                }
                onReward()
                onDismiss()
            }
        }

        ad.show(activity, OnUserEarnedRewardListener {
            onReward()
        })
    }

    private fun loadInterstitial(adUnitId: String, onLoaded: (InterstitialAd) -> Unit) {
        if (!isAppInForeground || adUnitId.isBlank()) return
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    onLoaded(ad)
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {}
            },
        )
    }

    private fun loadRewarded(adUnitId: String, onLoaded: (RewardedAd) -> Unit) {
        if (!isAppInForeground || adUnitId.isBlank()) return
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    onLoaded(ad)
                    _isRewardedLoaded.value = true
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {}
            }
        )
    }
}
