package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// ════════════════════════════════════════════════════════════
// FILE: data/remote/api/AchievementsApiService.kt
// DTOs + Retrofit interfaces for Achievements + Weekly Challenges
// ════════════════════════════════════════════════════════════

// ── Achievement DTOs ──────────────────────────────────────────

data class AchievementDto(
    val id: String,
    val key: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: String,           // "study"|"streak"|"quiz"|"social"|"tier"|"challenge"
    @SerializedName("coins_reward") val coinsReward: Int = 0,
    @SerializedName("xp_reward")    val xpReward: Int    = 0,
    @SerializedName("sort_order")   val sortOrder: Int   = 0,
    @SerializedName("is_earned")    val isEarned: Boolean = false,
    @SerializedName("earned_at")    val earnedAt: String? = null,
    @SerializedName("is_active")    val isActive: Boolean = true,
    // Progress fields for in-progress achievements
    @SerializedName("goal_target")  val goalTarget: Int? = null,
    @SerializedName("current_value")val currentValue: Int? = null
)

data class AchievementsResponseData(
    val achievements: List<AchievementDto> = emptyList(),
    val grouped: Map<String, List<AchievementDto>> = emptyMap(),
    @SerializedName("earnedCount") val earnedCount: Int = 0,
    @SerializedName("totalCount")  val totalCount: Int  = 0
)

data class RecentAchievementsData(
    val achievements: List<AchievementDto> = emptyList()
)

// ── Challenge DTOs ────────────────────────────────────────────

data class ChallengeGoalDto(
    val type: String,       // "study_hours"|"quizzes"|"goals"|"sessions"|"streak_days"
    val target: Double
)

data class ChallengeDto(
    val id: String,
    val title: String,
    val description: String?,
    val emoji: String,
    @SerializedName("period_key")         val periodKey: String,
    @SerializedName("target_tier_key")    val targetTierKey: String?,
    @SerializedName("target_tier_emoji")  val targetTierEmoji: String?,
    val goal: ChallengeGoalDto,
    @SerializedName("coins_reward")       val coinsReward: Int = 0,
    @SerializedName("xp_reward")          val xpReward: Int    = 0,
    // User-specific progress fields (null for users not in target tier)
    @SerializedName("user_progress")      val userProgress: Double = 0.0,
    @SerializedName("progress_pct")       val progressPct: Int     = 0,
    @SerializedName("is_completed")       val isCompleted: Boolean = false,
    @SerializedName("reward_claimed")     val rewardClaimed: Boolean = false,
    @SerializedName("completed_at")       val completedAt: String? = null,
    @SerializedName("is_active")          val isActive: Boolean    = true
)

data class ChallengesResponseData(
    val challenges: List<ChallengeDto> = emptyList(),
    @SerializedName("periodKey")  val periodKey: String  = "",
    @SerializedName("weekLabel")  val weekLabel: String  = ""
)

data class ClaimRewardData(
    @SerializedName("coinsRewarded") val coinsRewarded: Int = 0,
    @SerializedName("xpRewarded")    val xpRewarded: Int    = 0
)

// ════════════════════════════════════════════════════════════
// RETROFIT INTERFACES
// ════════════════════════════════════════════════════════════

interface AchievementsApiService {

    /**
     * GET /achievements
     * Returns all achievements with user's earned status.
     * Used in: AchievementsScreen
     */
    @GET("achievements")
    suspend fun getAll(): ApiResponse<AchievementsResponseData>

    /**
     * GET /achievements/recent?limit=5
     * Returns the 5 most recently earned achievements.
     * Used in: Dashboard achievements section (future)
     */
    @GET("achievements/recent")
    suspend fun getRecent(
        @Query("limit") limit: Int = 5
    ): ApiResponse<RecentAchievementsData>
}

interface ChallengesApiService {

    /**
     * GET /challenges/current
     * Returns this week's challenges + user's progress on each.
     * Used in: ChallengesScreen, RoomsHubScreen challenges tab
     */
    @GET("challenges/current")
    suspend fun getCurrent(): ApiResponse<ChallengesResponseData>

    /**
     * POST /challenges/{id}/claim
     * Claims the reward for a completed challenge.
     * Returns coins + XP awarded.
     */
    @POST("challenges/{id}/claim")
    suspend fun claimReward(
        @Path("id") id: String
    ): ApiResponse<ClaimRewardData>
}