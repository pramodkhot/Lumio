package com.lumio.gamezone.ads

import android.app.Activity
import android.util.Log
import com.facebook.ads.*

/**
 * AdManager — Facebook Audience Network interstitial ads.
 * Shows a full-screen ad after every 3rd game completion.
 *
 * App ID:        1302824574544461
 * Placement ID:  2046449172947811_2046450442947684
 * Test mode ON:  shows Facebook test ads immediately (no review needed)
 */
object AdManager {

    private const val TAG = "LumioAds"
    private const val PLACEMENT_ID = "2046449172947811_2046450442947684"
    private const val SHOW_EVERY_N = 3

    private var interstitialAd: InterstitialAd? = null
    private var isLoaded = false
    private var gameCount = 0
    private var currentActivity: Activity? = null

    fun init(activity: Activity) {
        currentActivity = activity
        AudienceNetworkAds.initialize(activity)
        AdSettings.setTestMode(true) // ← Remove this line before publishing to Play Store
        Log.d(TAG, "AdManager ready — test mode ON")
        loadAd(activity)
    }

    fun attachActivity(activity: Activity) {
        currentActivity = activity
    }

    fun onGameCompleted() {
        gameCount++
        Log.d(TAG, "Games played: $gameCount")
        if (gameCount % SHOW_EVERY_N == 0) showAd()
    }

    private fun loadAd(activity: Activity) {
        interstitialAd?.destroy()
        interstitialAd = null
        isLoaded = false

        val ad = InterstitialAd(activity, PLACEMENT_ID)
        val listener = object : InterstitialAdListener {
            override fun onInterstitialDisplayed(ad: Ad?) {
                Log.d(TAG, "Ad shown")
            }
            override fun onInterstitialDismissed(ad: Ad?) {
                Log.d(TAG, "Ad dismissed — reloading")
                isLoaded = false
                currentActivity?.let { loadAd(it) }
            }
            override fun onError(ad: Ad?, error: AdError?) {
                Log.e(TAG, "Ad error: ${error?.errorMessage}")
                isLoaded = false
            }
            override fun onAdLoaded(ad: Ad?) {
                Log.d(TAG, "Ad loaded ✅")
                isLoaded = true
            }
            override fun onAdClicked(ad: Ad?) {}
            override fun onLoggingImpression(ad: Ad?) {}
        }

        ad.loadAd(ad.buildLoadAdConfig().withAdListener(listener).build())
        interstitialAd = ad
    }

    private fun showAd() {
        val activity = currentActivity ?: return
        if (isLoaded && interstitialAd?.isAdLoaded == true) {
            interstitialAd?.show()
        } else {
            Log.w(TAG, "Ad not ready — will show next time")
            loadAd(activity)
        }
    }

    fun destroy() {
        interstitialAd?.destroy()
        interstitialAd = null
        currentActivity = null
    }
}
