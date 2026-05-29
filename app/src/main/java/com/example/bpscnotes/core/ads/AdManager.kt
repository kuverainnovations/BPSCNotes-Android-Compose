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
        // No daily cap on rewarded ads — every view earns revenue for client
        const val REWARDED_COINS                   = 10  // coins per ad watch
    }

    // Rewarded ad state
    private var rewardedAd:          RewardedAd?     = null
    private var isLoadingRewarded                    = false
    private val _rewardedReady                       = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean>            = _rewardedReady.asStateFlow()

    private val _adLoopActive                        = MutableStateFlow(false)
    val adLoopActive: StateFlow<Boolean>             = _adLoopActive.asStateFlow()

    // Interstitial ad state
    private var interstitialAd:      InterstitialAd? = null
    private var isLoadingInterstitial                = false
    private var lastInterstitialShownMs: Long        = 0L

    // Rewarded daily cap tracking (simple in-memory, resets on app restart)
    // Persist ad count in SharedPreferences so daily limit survives app restarts
    private val adPrefs by lazy {
        context.getSharedPreferences("bpsc_ad_prefs", android.content.Context.MODE_PRIVATE)
    }

    private var rewardedWatchedToday: Int
        get() {
            val saved = adPrefs.getString("rewarded_date", "") ?: ""
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            return if (saved == today) adPrefs.getInt("rewarded_count", 0) else 0
        }
        set(value) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            adPrefs.edit().putInt("rewarded_count", value).putString("rewarded_date", today).apply()
        }

    private var lastRewardedDate: String
        get() = adPrefs.getString("rewarded_date", "") ?: ""
        set(value) { adPrefs.edit().putString("rewarded_date", value).apply() }

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
        // No daily cap — every ad watched earns revenue, let users watch freely
        val ad = rewardedAd
        if (ad == null) {
            loadRewardedAd()
            onFailed("Ad not ready yet. Please try again in a few seconds.")
            return
        }

        ad.show(activity) { reward ->
            rewardedWatchedToday++  // track for analytics only
            onRewarded(REWARDED_COINS)
            Log.d(TAG, "💰 Reward earned: ${reward.amount} ${reward.type} → +$REWARDED_COINS coins (total today: $rewardedWatchedToday)")
        }
    }

    /**
     * Shows [count] rewarded ads back-to-back (default 2).
     * Each completed ad calls [onEachRewarded] with its coins.
     * [onAllComplete] fires after all ads finish or on first failure.
     */
    /**
     * Shows [count] rewarded ads back-to-back (default 2).
     * Waits for each ad to be FULLY DISMISSED before showing the next.
     * Each completed ad calls [onEachRewarded] with its coins.
     */
    /**
     * Shows [count] rewarded ads back-to-back.
     * Coins are accumulated across all ads and credited ONCE after the
     * final ad is dismissed — not per-ad. Back button is blocked externally
     * via [isAdLoopActive] StateFlow.
     */
    fun showRewardedAdLoop(
        activity:      Activity,
        count:         Int = 2,
        coinsPerAd:    Int = REWARDED_COINS,
        onAllComplete: (totalCoins: Int) -> Unit,  // called ONCE after ALL ads done
        onFailed:      (reason: String) -> Unit,
    ) {
        var shownCount    = 0
        var totalEarned   = 0  // accumulate across all ads, credit at the end
        var earnedThisAd  = false  // did the user earn reward for the current ad?

        // Signal that the loop is active — UI uses this to block back button
        _adLoopActive.value = true

        fun finish() {
            _adLoopActive.value = false
            loadRewardedAd()  // pre-load for next session
            onAllComplete(totalEarned)
        }

        fun showNext() {
            if (shownCount >= count) { finish(); return }

            val ad = rewardedAd
            if (ad == null) {
                loadRewardedAd()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val retry = rewardedAd
                    if (retry != null) showNext()
                    else {
                        _adLoopActive.value = false
                        // If at least 1 ad completed, still credit those coins
                        if (totalEarned > 0) onAllComplete(totalEarned)
                        else onFailed("Ad not ready. Please try again in a few seconds.")
                    }
                }, 5_000)
                return
            }

            earnedThisAd = false
            val adNumber = shownCount + 1
            Log.d(TAG, "📺 Showing loop ad $adNumber/$count")

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    _rewardedReady.value = false
                    Log.d(TAG, "✅ Ad $adNumber dismissed (earned=$earnedThisAd)")

                    if (shownCount < count) {
                        // More ads to show — load next and chain
                        loadRewardedAd()
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            showNext()
                        }, 600)
                    } else {
                        // All ads done — credit total now
                        finish()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Ad $adNumber failed to show: ${error.message}")
                    rewardedAd = null
                    _rewardedReady.value = false
                    _adLoopActive.value = false
                    loadRewardedAd()
                    if (totalEarned > 0) onAllComplete(totalEarned)
                    else onFailed("Ad failed to show. Please try again.")
                }
            }

            ad.show(activity) { _ ->
                // Reward granted — accumulate, don't credit yet
                rewardedWatchedToday++
                shownCount++
                earnedThisAd = true
                totalEarned += coinsPerAd
                Log.d(TAG, "💰 Ad $adNumber reward earned. Total so far: $totalEarned coins")
            }
        }

        showNext()
    }

    fun canWatchRewardedAd(): Boolean = true  // no daily limit

    /** Returns how many ads watched today (for analytics). -1 means unlimited. */
    fun rewardedAdsRemainingToday(): Int = -1  // unlimited — no cap

    fun rewardedAdsWatchedToday(): Int = rewardedWatchedToday

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