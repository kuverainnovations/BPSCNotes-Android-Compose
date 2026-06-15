package com.example.bpscnotes.core.config

import com.example.bpscnotes.data.remote.api.CoinRuleConfigDto
import com.example.bpscnotes.data.remote.api.CoinsApiService
import com.example.bpscnotes.data.remote.api.CoinsConfigDto
import com.example.bpscnotes.data.remote.api.CoinsEconomyDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Mirrors the canonical coin_rules seed in
// backend/src/database/migrations/1781700000000-UnifyCoinEconomy.ts —
// used ONLY until the first successful /coins/config fetch (or if it
// ever fails), so numbers shown before that point match the backend
// defaults instead of being 0/blank.
private val DEFAULT_RULES: Map<String, CoinRuleConfigDto> = mapOf(
    "daily_quiz"          to CoinRuleConfigDto(coins = 10,  maxPerDay = 1),
    "mock_quiz"           to CoinRuleConfigDto(coins = 10,  maxPerDay = 3),
    "topic_quiz"          to CoinRuleConfigDto(coins = 15,  maxPerDay = 5),
    "study_room"          to CoinRuleConfigDto(coins = 5,   maxPerDay = 2),
    "study_session"       to CoinRuleConfigDto(coins = 15,  maxPerDay = 1),
    "active_recall"       to CoinRuleConfigDto(coins = 5,   maxPerDay = 3),
    "material_upload"     to CoinRuleConfigDto(coins = 25,  maxPerDay = 1),
    "daily_login"         to CoinRuleConfigDto(coins = 5,   maxPerDay = 1),
    "referral_signup"     to CoinRuleConfigDto(coins = 50,  maxPerDay = 5),
    "referral_joined"     to CoinRuleConfigDto(coins = 25,  maxPerDay = 1),
    "referral_engagement" to CoinRuleConfigDto(coins = 50,  maxPerDay = 1),
    "referral_active"     to CoinRuleConfigDto(coins = 50,  maxPerDay = 1),
    "profile_complete"    to CoinRuleConfigDto(coins = 20,  maxPerDay = 1),
    "ad_watch"            to CoinRuleConfigDto(coins = 5,   maxPerDay = 3),
    "achievement"         to CoinRuleConfigDto(coins = 10,  maxPerDay = 10),
    "leaderboard_reward"  to CoinRuleConfigDto(coins = 50,  maxPerDay = 1),
    "tier_promotion"      to CoinRuleConfigDto(coins = 20,  maxPerDay = 1),
    "weekly_challenge"    to CoinRuleConfigDto(coins = 30,  maxPerDay = 1),
    "streak_7"            to CoinRuleConfigDto(coins = 15,  maxPerDay = 1),
    "streak_30"           to CoinRuleConfigDto(coins = 100, maxPerDay = 1),
    "mock_top10"          to CoinRuleConfigDto(coins = 100, maxPerDay = 1),
    "target_complete"     to CoinRuleConfigDto(coins = 1,   maxPerDay = 20),
    "subscription_bonus"  to CoinRuleConfigDto(coins = 0,   maxPerDay = 1),
)

private val DEFAULT_CONFIG = CoinsConfigDto(
    enabled = true,
    rules = DEFAULT_RULES,
    economy = CoinsEconomyDto(),
    checkInRewards = listOf(5, 5, 10, 10, 15, 15, 25),
)

/**
 * Single source for every coin amount, daily cap, and economy setting
 * shown anywhere in the app (wallet, quiz screens, referral screen,
 * payment screen, course/material redemption, etc). Backed by
 * GET /coins/config, which mirrors the admin "Coins" page exactly —
 * an admin edit there is reflected here on the next fetch, with no
 * app update required.
 *
 * Call [fetch] once on app start (see MainActivity) and again whenever
 * the wallet screen is opened, so admin changes show up promptly.
 */
@Singleton
class CoinsConfigRepository @Inject constructor(
    private val api: CoinsApiService
) {
    private val _config = MutableStateFlow(DEFAULT_CONFIG)
    val config: StateFlow<CoinsConfigDto> = _config

    suspend fun fetch() {
        try {
            val res = api.getConfig()
            res.data?.let { fetched ->
                // Fill in any action the server hasn't seeded yet with our
                // bundled defaults, so a partial response never zeroes out
                // a number the rest of the app expects.
                _config.value = fetched.copy(rules = DEFAULT_RULES + fetched.rules)
            }
        } catch (_: Exception) { /* keep previous/default values on network failure */ }
    }

    /** Coins awarded for [action], or [fallback] if the rule is missing/inactive. */
    fun coins(action: String, fallback: Int = 0): Int {
        val rule = _config.value.rules[action] ?: return fallback
        return if (rule.active) rule.coins else fallback
    }

    /** Max times [action] can be rewarded per day (0 = unlimited). */
    fun maxPerDay(action: String, fallback: Int = 1): Int =
        _config.value.rules[action]?.maxPerDay ?: fallback

    val isEnabled: Boolean get() = _config.value.enabled
    val economy: CoinsEconomyDto get() = _config.value.economy
    val checkInRewards: List<Int> get() = _config.value.checkInRewards
}
