package com.example.bpscnotes.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// ════════════════════════════════════════════════════════════
// AdManager — Central ad controller for BPSCNotes
//
// Placements:
//  1. Rewarded ads    → CoinWalletScreen (Watch Ad → earn 10 coins)
//                       StudyFocusScreen (after session ends, optional earn)
//  2. Interstitial    → QuizPlayScreen result screen (1 per quiz, not more)
//  3. Native/Banner   → JobVacanciesScreen, DashboardScreen
//
// Frequency rules:
//  - Interstitial: max 1 per 20 minutes
//  - Rewarded:     max 3 per day (don't devalue coins)
//  - Banner:       always shown on non-study screens
//  - NO ads in:    Study Rooms, Flashcards, Quiz play (only result screen)
//
// Test IDs are used in DEBUG builds automatically.
// Replace with real IDs from AdMob dashboard for release.
// ════════════════════════════════════════════════════════════

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AdManager"

        // ── AdMob Unit IDs ─────────────────────────────────────
        // REPLACE these with your real AdMob IDs before release.
        // Test IDs from Google — safe to use during development.
        private const val REWARDED_AD_UNIT_ID     = "ca-app-pub-3940256099942544/5224354917"  // test
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"  // test
        private const val BANNER_AD_UNIT_ID       = "ca-app-pub-3940256099942544/6300978111"  // test
        private const val NATIVE_AD_UNIT_ID       = "ca-app-pub-3940256099942544/2247696110"  // test

        // ── Frequency limits ───────────────────────────────────
        private const val INTERSTITIAL_COOLDOWN_MS = 20 * 60 * 1000L  // 20 minutes
        private const val MAX_REWARDED_PER_DAY     = 3
        const val REWARDED_COINS                   = 10  // coins per ad watch
    }

    // Rewarded ad state
    private var rewardedAd:          RewardedAd?     = null
    private var isLoadingRewarded                    = false
    private val _rewardedReady                       = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean>            = _rewardedReady.asStateFlow()

    // Interstitial ad state
    private var interstitialAd:      InterstitialAd? = null
    private var isLoadingInterstitial                = false
    private var lastInterstitialShownMs: Long        = 0L

    // Rewarded daily cap tracking (simple in-memory, resets on app restart)
    private var rewardedWatchedToday                 = 0
    private var lastRewardedDate                     = ""

    // ── Initialise SDK ─────────────────────────────────────────
    fun initialize() {
        MobileAds.initialize(context) { initStatus ->
            Log.d(TAG, "AdMob init: ${initStatus.adapterStatusMap}")
        }
        // Pre-load both ad types on startup
        loadRewardedAd()
        loadInterstitialAd()
    }

    // ════════════════════════════════════════════════════════════
    // REWARDED ADS — user voluntarily watches to earn coins
    // ════════════════════════════════════════════════════════════

    fun loadRewardedAd() {
        if (isLoadingRewarded || rewardedAd != null) return
        isLoadingRewarded = true
        _rewardedReady.value = false

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd        = ad
                isLoadingRewarded = false
                _rewardedReady.value = true
                Log.d(TAG, "✅ Rewarded ad loaded")

                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd           = null
                        _rewardedReady.value = false
                        loadRewardedAd()     // pre-load next one immediately
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.e(TAG, "Rewarded show failed: ${error.message}")
                        rewardedAd           = null
                        _rewardedReady.value = false
                        loadRewardedAd()
                    }
                }
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Rewarded load failed: ${error.message}")
                isLoadingRewarded = false
                _rewardedReady.value = false
                // Retry after 60 seconds
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(::loadRewardedAd, 60_000)
            }
        })
    }

    /**
     * Show a rewarded ad. Calls [onRewarded] with the coins amount if user
     * completes the ad. Calls [onFailed] if ad isn't available or daily cap hit.
     */
    fun showRewardedAd(activity: Activity, onRewarded: (coins: Int) -> Unit, onFailed: (reason: String) -> Unit) {
        // Check daily cap
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        if (lastRewardedDate != today) { rewardedWatchedToday = 0; lastRewardedDate = today }

        if (rewardedWatchedToday >= MAX_REWARDED_PER_DAY) {
            onFailed("Daily limit reached. Come back tomorrow for more coins! 🌙")
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            loadRewardedAd()
            onFailed("Ad not ready yet. Please try again in a few seconds.")
            return
        }

        var didEarnReward = false
        ad.show(activity) { reward ->
            // This fires only if the user completes the ad
            didEarnReward = true
            rewardedWatchedToday++
            onRewarded(REWARDED_COINS)
            Log.d(TAG, "💰 Reward earned: ${reward.amount} ${reward.type} → +$REWARDED_COINS coins")
        }
    }

    fun canWatchRewardedAd(): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        if (lastRewardedDate != today) { rewardedWatchedToday = 0; lastRewardedDate = today }
        return rewardedWatchedToday < MAX_REWARDED_PER_DAY
    }

    fun rewardedAdsRemainingToday(): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        if (lastRewardedDate != today) return MAX_REWARDED_PER_DAY
        return maxOf(0, MAX_REWARDED_PER_DAY - rewardedWatchedToday)
    }

    // ════════════════════════════════════════════════════════════
    // INTERSTITIAL ADS — shown after quiz completion (1 per 20min)
    // ════════════════════════════════════════════════════════════

    fun loadInterstitialAd() {
        if (isLoadingInterstitial || interstitialAd != null) return
        isLoadingInterstitial = true

        InterstitialAd.load(
            context, INTERSTITIAL_AD_UNIT_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd        = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "✅ Interstitial loaded")

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitialAd() // pre-load next
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.e(TAG, "Interstitial show failed: ${error.message}")
                            interstitialAd = null
                            loadInterstitialAd()
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial load failed: ${error.message}")
                    isLoadingInterstitial = false
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(::loadInterstitialAd, 60_000)
                }
            }
        )
    }

    /**
     * Show interstitial after quiz/session ends. Enforces 20-minute cooldown.
     * [onComplete] is called whether or not the ad was shown (so caller can proceed).
     */
    fun showInterstitialIfReady(activity: Activity, onComplete: () -> Unit) {
        val now = System.currentTimeMillis()
        val cooldownOk = (now - lastInterstitialShownMs) >= INTERSTITIAL_COOLDOWN_MS

        val ad = interstitialAd
        if (ad != null && cooldownOk) {
            lastInterstitialShownMs = now
            // Hook onComplete into dismissal
            val original = ad.fullScreenContentCallback
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    original?.onAdDismissedFullScreenContent()
                    onComplete()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    original?.onAdFailedToShowFullScreenContent(error)
                    onComplete()
                }
                override fun onAdShowedFullScreenContent() { original?.onAdShowedFullScreenContent() }
                override fun onAdClicked()                 { original?.onAdClicked() }
                override fun onAdImpression()              { original?.onAdImpression() }
            }
            ad.show(activity)
            Log.d(TAG, "📺 Interstitial shown after quiz")
        } else {
            if (!cooldownOk) Log.d(TAG, "Interstitial skipped: cooldown (${(now - lastInterstitialShownMs) / 1000}s < 1200s)")
            onComplete()
        }
    }

    // ── Banner ad unit ID (used directly in BannerAdView composable) ──
    fun getBannerAdUnitId()  = BANNER_AD_UNIT_ID
    fun getNativeAdUnitId()  = NATIVE_AD_UNIT_ID
}