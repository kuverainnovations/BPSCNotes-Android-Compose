package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// ════════════════════════════════════════════════════════════
// FILE: data/remote/api/TierRoomsApiService.kt
//
// All DTOs + Retrofit interfaces for the Tier-Based Study Room
// system. Mirrors the backend TierRoomsModule APIs exactly.
// ════════════════════════════════════════════════════════════

// ── Tier DTOs ─────────────────────────────────────────────────

data class RoomTierDto(
    val id: String,
    @SerializedName("tierKey")       val tierKey: String,        // "silver"|"gold"|"premium"|"diamond"
    val name: String,
    val description: String?,
    @SerializedName("color_hex")      val colorHex: String,       // "#C0C0C0"
    @SerializedName("icon_emoji")     val iconEmoji: String,      // "🥈"
    @SerializedName("sort_order")     val sortOrder: Int,
    @SerializedName("max_members")    val maxMembers: Int,
    @SerializedName("coin_multiplier") val coinMultiplier: Double, // 1.0 | 1.5 | 2.0 | 3.0
    @SerializedName("xp_multiplier")  val xpMultiplier: Double,
    val perks: List<String> = emptyList(),
    @SerializedName("total_members")  val totalMembers: Int = 0,
    @SerializedName("active_sessions") val activeSessions: Int = 0,
    @SerializedName("is_active")      val isActive: Boolean = true
)

data class TierProgressItemDto(
    val label: String,      // "Study Hours" | "Streak" | "Quizzes" | "Accuracy"
    val current: Double,
    val required: Double,
    val unit: String,       // "h" | "days" | "quizzes" | "%"
    val done: Boolean       // true = this condition is met
)

data class NextTierDto(
    val id: String,
    @SerializedName("tierKey")   val tierKey: String,
    val name: String,
    @SerializedName("icon_emoji") val iconEmoji: String
)

data class UserTierStatsDto(
    @SerializedName("total_study_hours")   val totalStudyHours: Double,
    val streak: Int,
    @SerializedName("quizzes_attempted")   val quizzesAttempted: Int,
    val accuracy: Double,
    val xp: Int,
    @SerializedName("xp_level")            val xpLevel: Int,
    val coins: Int
)

/**
 * Response for GET /rooms/tiers/my
 * Contains: current tier, next tier, per-condition progress, user stats
 */
data class MyTierResponseData(
    @SerializedName("currentTier")      val currentTier: RoomTierDto,
    @SerializedName("nextTier")         val nextTier: NextTierDto?,
    @SerializedName("promotedAt")       val promotedAt: String?,
    @SerializedName("nextTierProgress") val nextTierProgress: Double, // 0.0-1.0
    @SerializedName("progressItems")    val progressItems: List<TierProgressItemDto>,
    @SerializedName("demotionGraceUntil") val demotionGraceUntil: String?,
    @SerializedName("userStats")        val userStats: UserTierStatsDto
)

data class TiersListResponseData(
    val tiers: List<RoomTierDto> = emptyList()
)

// ── Leaderboard DTOs ──────────────────────────────────────────

data class LeaderboardEntryDto(
    @SerializedName("rank_position")   val rankPosition: Int,
    @SerializedName("study_minutes")   val studyMinutes: Int,
    @SerializedName("coins_earned")    val coinsEarned: Int,
    @SerializedName("xp_earned")       val xpEarned: Int,
    @SerializedName("goals_completed") val goalsCompleted: Int,
    @SerializedName("streak_days")     val streakDays: Int,
    @SerializedName("user_id")         val userId: String,
    @SerializedName("user_name")       val userName: String,
    @SerializedName("xp_level")        val xpLevel: Int
)

data class LeaderboardResponseData(
    val leaderboard: List<LeaderboardEntryDto> = emptyList(),
    val period: String,         // "weekly"|"monthly"|"alltime"
    @SerializedName("periodKey") val periodKey: String  // "2026-W19"
)

// ── Tier Members ──────────────────────────────────────────────

data class TierMemberDto(
    val id: String,
    val name: String,
    val streak: Int,
    @SerializedName("quizzes_attempted") val quizzesAttempted: Int,
    val accuracy: Double,
    val coins: Int,
    val xp: Int,
    @SerializedName("xp_level")          val xpLevel: Int,
    @SerializedName("total_study_minutes") val totalStudyMinutes: Int,
    @SerializedName("promoted_at")        val promotedAt: String?,
    @SerializedName("next_tier_progress") val nextTierProgress: Double,
    @SerializedName("is_studying_now")    val isStudyingNow: Boolean = false
)

data class TierMembersResponseData(
    val members: List<TierMemberDto> = emptyList()
)

// ── Study Session DTOs ────────────────────────────────────────

/**
 * Request body for POST /rooms/sessions/start
 */
data class StartSessionRequest(
    @SerializedName("roomId") val roomId: String? = null,
    val mode: String = "study"   // "study" | "pomodoro" | "silent"
)

/**
 * Response from POST /rooms/sessions/start
 */
data class StartSessionResponseData(
    val sessionId: String,
    val startedAt: String,
    val mode: String,
    val tierId: String?,
    @SerializedName("heartbeatIntervalSeconds") val heartbeatIntervalSeconds: Int = 300
)

/**
 * Request body for POST /rooms/sessions/heartbeat
 */
data class HeartbeatRequest(
    val sessionId: String
)

/**
 * Response from POST /rooms/sessions/heartbeat
 * The ViewModel reads coinsEarnedThisBeat to update the live coin counter.
 */
data class HeartbeatResponseData(
    val isAfk: Boolean,
    @SerializedName("activeMinsThisBeat")    val activeMinsThisBeat: Int,
    @SerializedName("coinsEarnedThisBeat")   val coinsEarnedThisBeat: Int,
    @SerializedName("xpEarnedThisBeat")      val xpEarnedThisBeat: Int,
    @SerializedName("totalCoinsThisSession") val totalCoinsThisSession: Int,
    @SerializedName("totalXpThisSession")    val totalXpThisSession: Int,
    @SerializedName("totalActiveMinutes")    val totalActiveMinutes: Int,
    val message: String
)

/**
 * Request body for POST /rooms/sessions/end
 */
data class EndSessionRequest(
    val sessionId: String
)

/**
 * Response from POST /rooms/sessions/end — shown in the session summary sheet
 */
data class EndSessionResponseData(
    val sessionId: String,
    @SerializedName("durationMinutes")  val durationMinutes: Int,
    @SerializedName("activeMinutes")    val activeMinutes: Int,
    @SerializedName("totalCoins")       val totalCoins: Int,
    @SerializedName("totalXp")          val totalXp: Int,
    @SerializedName("bonusCoins")       val bonusCoins: Int,
    val message: String
)

/**
 * Active session data returned by GET /rooms/sessions/active
 * Null session means no active session exists.
 */
data class ActiveSessionTierDto(
    val name: String,
    @SerializedName("iconEmoji") val iconEmoji: String,
    @SerializedName("colorHex")  val colorHex: String
)

data class ActiveSessionDto(
    val id: String,
    val startedAt: String,
    val mode: String,
    @SerializedName("activeMinutes")  val activeMinutes: Int,
    @SerializedName("coinsEarned")    val coinsEarned: Int,
    @SerializedName("xpEarned")       val xpEarned: Int,
    @SerializedName("afkCount")       val afkCount: Int,
    @SerializedName("isAfkNow")       val isAfkNow: Boolean,
    @SerializedName("lastHeartbeat")  val lastHeartbeat: String,
    val tier: ActiveSessionTierDto?
)

data class ActiveSessionResponseData(
    val session: ActiveSessionDto?   // null = no active session
)

// ════════════════════════════════════════════════════════════
// RETROFIT INTERFACE
// ════════════════════════════════════════════════════════════

interface TierRoomsApiService {

    // ── Tier Rooms ───────────────────────────────────────────

    /**
     * GET /rooms/tiers
     * Returns all 4 tier rooms with member counts and active session counts.
     * Used in: RoomsHubScreen lobby
     */
    @GET("rooms/tiers")
    suspend fun getAllTiers(): ApiResponse<TiersListResponseData>

    /**
     * GET /rooms/tiers/my
     * Returns the user's current tier + progress toward next tier.
     * Used in: RoomsHubScreen hero card + TierRoomScreen header
     */
    @GET("rooms/tiers/my")
    suspend fun getMyTier(): ApiResponse<MyTierResponseData>

    /**
     * GET /rooms/tiers/{tierKey}/members?page=1&limit=20
     * Returns paginated list of users currently in this tier.
     * Used in: TierRoomScreen → "Members" tab
     */
    @GET("rooms/tiers/{tierKey}/members")
    suspend fun getTierMembers(
        @Path("tierKey") tierKey: String,
        @Query("page")   page: Int = 1,
        @Query("limit")  limit: Int = 20
    ): ApiResponse<TierMembersResponseData>

    /**
     * GET /rooms/tiers/{tierKey}/leaderboard?period=weekly
     * Returns cron-computed leaderboard for this tier.
     * period: "weekly" | "monthly" | "alltime"
     * Used in: TierRoomScreen → "Leaderboard" tab
     */
    @GET("rooms/tiers/{tierKey}/leaderboard")
    suspend fun getTierLeaderboard(
        @Path("tierKey")   tierKey: String,
        @Query("period")   period: String = "weekly"
    ): ApiResponse<LeaderboardResponseData>

    // ── Study Sessions ───────────────────────────────────────

    /**
     * POST /rooms/sessions/start
     * Creates a new study session. Returns sessionId used in subsequent calls.
     * Error 409 if user already has an active session.
     * Used in: StudySessionViewModel.startSession()
     */
    @POST("rooms/sessions/start")
    suspend fun startSession(
        @Body body: StartSessionRequest
    ): ApiResponse<StartSessionResponseData>

    /**
     * POST /rooms/sessions/heartbeat
     * Must be called every 5 minutes to:
     *   - Prove user is active (AFK detection)
     *   - Receive coin + XP award for the verified interval
     * If gap since last heartbeat > 7 min: isAfk=true, no coins awarded.
     * Used in: StudySessionViewModel heartbeat coroutine loop
     */
    @POST("rooms/sessions/heartbeat")
    suspend fun heartbeat(
        @Body body: HeartbeatRequest
    ): ApiResponse<HeartbeatResponseData>

    /**
     * POST /rooms/sessions/end
     * Ends the active session. Awards bonus coins if >= 30 active minutes.
     * Returns session summary shown in the EndSessionSheet composable.
     * Used in: StudySessionViewModel.endSession()
     */
    @POST("rooms/sessions/end")
    suspend fun endSession(
        @Body body: EndSessionRequest
    ): ApiResponse<EndSessionResponseData>

    /**
     * GET /rooms/sessions/active
     * Returns the user's currently active session (or null).
     * Called on app resume to restore session state.
     * Used in: StudySessionViewModel.checkForExistingSession()
     */
    @GET("rooms/sessions/active")
    suspend fun getActiveSession(): ApiResponse<ActiveSessionResponseData>
}
