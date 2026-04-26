package com.example.bpscnotes.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request body for POST /auth/register (new user only).
 * All other auth DTOs live in AuthApiService.kt next to the interface.
 */
data class RegisterRequest(
    val tempToken: String,
    val name: String,
    val email: String? = null,
    val district: String? = null,
    val referralCode: String? = null
)

/**
 * User profile returned by GET /auth/me and embedded in auth responses.
 */

data class GetMeData(
    val user: UserDto
)
data class UserDto(
    val id: String,
    val name: String,

    val mobile: String? = null,
    val email: String? = null,

    @SerializedName("mobile_verified")
    val mobileVerified: Boolean = false,

    @SerializedName("email_verified")
    val emailVerified: Boolean = false,

    @SerializedName("avatar_url")
    val avatarUrl: String? = null,

    val bio: String? = null,
    val district: String? = null,
    val state: String? = null,
    val role: String? = null,
    val status: String? = null,

    @SerializedName("primary_exam")
    val primaryExam: String? = null,

    @SerializedName("secondary_exam")
    val secondaryExam: String? = null,

    @SerializedName("prep_level")
    val prepLevel: String? = null,

    @SerializedName("target_year")
    val targetYear: Int? = null,

    val streak: Int = 0,

    @SerializedName("longest_streak")
    val longestStreak: Int = 0,

    @SerializedName("last_study_date")
    val lastStudyDate: String? = null,

    val coins: Int = 0,

    @SerializedName("total_coins_earned")
    val totalCoinsEarned: Int = 0,

    val rank: Int? = null,

    @SerializedName("total_study_minutes")
    val totalStudyMinutes: Int = 0,

    // ⚠️ FIX: API sends String
    val accuracy: String? = null,

    @SerializedName("quizzes_attempted")
    val quizzesAttempted: Int = 0,

    @SerializedName("referral_code")
    val referralCode: String? = null,

    @SerializedName("referred_by")
    val referredBy: String? = null,

    @SerializedName("is_verified")
    val isVerified: Boolean = false,

    @SerializedName("notification_enabled")
    val notificationEnabled: Boolean = true,

    @SerializedName("is_subscribed")
    val isSubscribed: Boolean = false,

    @SerializedName("current_plan")
    val currentPlan: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("last_active_at")
    val lastActiveAt: String? = null,

    @SerializedName("deleted_at")
    val deletedAt: String? = null
)
