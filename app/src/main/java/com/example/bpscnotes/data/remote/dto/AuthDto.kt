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

    @SerializedName("avatar_url")
    val avatarUrl: String? = null,

    @SerializedName("primary_exam")
    val primaryExam: String? = null,

    val streak: Int = 0,
    val coins: Int = 0,
    val accuracy: Double = 0.0,

    @SerializedName("quizzes_attempted")
    val quizzesAttempted: Int = 0,

    val rank: Int? = null,

    @SerializedName("is_verified")
    val isVerified: Boolean = false,

    @SerializedName("is_subscribed")
    val isSubscribed: Boolean = false,

    @SerializedName("current_plan")
    val currentPlan: String? = null,

    @SerializedName("prep_level")
    val prepLevel: String? = null,

    @SerializedName("total_study_minutes")
    val totalStudyMinutes: Int = 0,

    @SerializedName("created_at")
    val createdAt: String? = null
)
