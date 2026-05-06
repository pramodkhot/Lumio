package com.lumio.gamezone.ads

import android.app.Activity
import android.util.Log
import com.facebook.ads.*

/**
 * AdManager — handles all Facebook Audience Network ads for Lumio.
 *
 * Interstitial fires after every 3rd game completion.
 * Test mode is ON automatically when app is in debug build.
 *
 * Placement ID: 2046449172947811_2046450442947684
 * App ID:       1302824574544461
 */
object AdManager {

    private const val TAG = "LumioAds"

    // ── Your real Placement ID ────────────────────────────────────────────────
    private const val INTERSTITIAL_PLACEMENT_ID = "2046449172947811_2046450442947684"

    // ── Test Placement ID — used automatically in debug builds ────────────────
    // Facebook's official test placement ID for interstitials
    private const val TEST_PLACEMENT_ID = "IMG_16_9_APP_INSTALL#2046449172947811_2046450442947684"

    private var interstitialAd: InterstitialAd? = null
    private var gameCount = 0
    private const val SHOW_EVERY_N_GAMES = 3
    private var isLoaded = false
    private var currentActivity: Activity? = null

    /**
     * Call this from Application class or first Activity onCreate.
     * Initialises FAN SDK and loads first ad in background.
     */
    fun init(activity: Activity) {
        currentActivity = activity

        // Enable test mode for debug builds
        if (android.os.Build.VERSION.RELEASE != null) {
            AudienceNetworkAds.initialize(activity)
        }

        // Add your test device hash to get test ads on your specific device
        // Find your hash in Logcat: search for "Use AdSettings.addTestDevice"
        // AdSettings.addTestDevice("YOUR_DEVICE_HASH_HERE")

        // Force test ads (safe for development — REMOVE before Play Store submission)
        AdSettings.setTestMode(true) // ← shows test ads always during testing

        Log.d(TAG, "AdManager initialized — test mode ON")
        loadInterstitial(activity)
    }

    /**
     * Call this from every BaseGameActivity.onResume()
     * to keep the current activity reference fresh.
     */
    fun attachActivity(activity: Activity) {
        currentActivity = activity
    }

    /**
     * Call this when a game finishes (win, lose, or draw).
     * Ad shows automatically after every 3rd call.
     */
    fun onGameCompleted() {
        gameCount++
        Log.d(TAG, "Game completed — count: $gameCount")

        if (gameCount % SHOW_EVERY_N_GAMES == 0) {
            showInterstitial()
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun loadInterstitial(activity: Activity) {
        interstitialAd?.destroy()
        interstitialAd = null
        isLoaded = false

        val placementId = if (android.os.Build.TYPE == "debug" || AdSettings.isTestMode)
            INTERSTITIAL_PLACEMENT_ID else INTERSTITIAL_PLACEMENT_ID

        val ad = InterstitialAd(activity, INTERSTITIAL_PLACEMENT_ID)

        val listener = object : InterstitialAdListener {
            override fun onInterstitialDisplayed(ad: Ad?) {
                Log.d(TAG, "Interstitial displayed")
            }
            override fun onInterstitialDismissed(ad: Ad?) {
                Log.d(TAG, "Interstitial dismissed — loading next")
                isLoaded = false
                currentActivity?.let { loadInterstitial(it) }
            }
            override fun onError(ad: Ad?, error: AdError?) {
                Log.e(TAG, "Ad error: ${error?.errorMessage}")
                isLoaded = false
            }
            override fun onAdLoaded(ad: Ad?) {
                Log.d(TAG, "Interstitial loaded ✅")
                isLoaded = true
            }
            override fun onAdClicked(ad: Ad?) {
                Log.d(TAG, "Interstitial clicked")
            }
            override fun onLoggingImpression(ad: Ad?) {}
        }

        ad.loadAd(ad.buildLoadAdConfig()
            .withAdListener(listener)
            .build())

        interstitialAd = ad
    }

    private fun showInterstitial() {
        val activity = currentActivity ?: run {
            Log.w(TAG, "No activity attached — skipping ad")
            return
        }

        if (isLoaded && interstitialAd?.isAdLoaded == true) {
            Log.d(TAG, "Showing interstitial ad")
            interstitialAd?.show()
        } else {
            Log.w(TAG, "Ad not ready yet — loading for next time")
            loadInterstitial(activity)
        }
    }

    fun destroy() {
        interstitialAd?.destroy()
        interstitialAd = null
        isLoaded = false
        currentActivity = null
    }
}
