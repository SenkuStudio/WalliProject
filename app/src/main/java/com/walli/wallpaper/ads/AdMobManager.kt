package com.walli.wallpaper.ads

import android.app.Activity
import android.content.Context
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
) {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    
    private val _isRewardedLoaded = MutableStateFlow(false)
    val isRewardedLoaded = _isRewardedLoaded.asStateFlow()

    private var openCounter = 0
    private var downloadCounter = 0

    fun warmUp() {
        loadInterstitial()
        loadRewarded()
    }

    fun maybeShowOpenInterstitial(activity: Activity?, onContinue: () -> Unit) {
        openCounter += 1
        if (openCounter % 3 != 0) {
            onContinue()
            return
        }
        showInterstitial(activity = activity, onContinue = onContinue)
    }

    fun maybeShowDownloadInterstitial(activity: Activity?, onContinue: () -> Unit) {
        downloadCounter += 1
        if (downloadCounter % 2 != 0) {
            onContinue()
            return
        }
        showInterstitial(activity = activity, onContinue = onContinue)
    }

    private fun showInterstitial(activity: Activity?, onContinue: () -> Unit) {
        val interstitial = interstitialAd
        if (activity == null || interstitial == null) {
            onContinue()
            loadInterstitial()
            return
        }

        interstitial.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial()
                onContinue()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadInterstitial()
                onContinue()
            }
        }
        interstitial.show(activity)
    }

    fun showRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onDismiss: () -> Unit = {},
    ) {
        val rewarded = rewardedAd
        if (activity == null || rewarded == null) {
            // Bypass if ad is not ready, as per existing logic in ViewModel
            onReward()
            onDismiss()
            loadRewarded()
            return
        }

        rewarded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isRewardedLoaded.value = false
                loadRewarded()
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _isRewardedLoaded.value = false
                loadRewarded()
                // Bypass failure to not block user
                onReward()
                onDismiss()
            }
        }

        rewarded.show(activity, OnUserEarnedRewardListener {
            onReward()
        })
    }

    private fun loadInterstitial() {
        if (BuildConfig.ADMOB_INTERSTITIAL_ID.isBlank()) return
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                }
            },
        )
    }

    private fun loadRewarded() {
        if (BuildConfig.ADMOB_REWARDED_ID.isBlank()) return
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _isRewardedLoaded.value = true
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    _isRewardedLoaded.value = false
                }
            }
        )
    }
}
