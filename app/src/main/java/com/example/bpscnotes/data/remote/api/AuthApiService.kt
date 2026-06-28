package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.example.bpscnotes.data.remote.dto.GetMeData
import com.example.bpscnotes.data.remote.dto.RegisterRequest
import com.example.bpscnotes.data.remote.dto.UserDto
import com.google.gson.annotations.SerializedName
import retrofit2.http.*
// ════════════════════════════════════════════════════════════
// AuthApiService — auth + profile management
// ════════════════════════════════════════════════════════════
interface AuthApiService {

    /** Step 1 — request OTP */
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body body: SendOtpRequest): SendOtpResponse

    /** Step 2 — verify OTP */
    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): VerifyOtpResponse

    /** Check OTP is valid without consuming it — used by forgot-mpin flow */
    @POST("auth/validate-otp")
    suspend fun validateForgotMpinOtp(@Body body: VerifyOtpRequest): ApiResponse<Any>

    /** Step 3 — register with name */
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    /** GET /auth/me — current user profile */
    @GET("auth/me")
    suspend fun getMe(): GetMeResponse

    /** GET /exams */
    @GET("exams")
    suspend fun getExams(): ApiResponse<ExamsResponseData>

    /** GET /districts — district list for the profile-creation dropdown */
    @GET("districts")
    suspend fun getDistricts(): ApiResponse<DistrictsResponseData>

    /** PUT /users/exam-target */
    @PUT("users/exam-target")
    suspend fun saveExamTarget(@Body body: ExamTargetRequest): ApiResponse<Any>

    /** PATCH /users/profile — update name, email, bio, district, etc. */
    @PATCH("auth/users/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ApiResponse<GetMeData>

    /** POST /users/upload-avatar — upload profile picture */
    @Multipart
    @POST("users/upload-avatar")
    suspend fun uploadAvatar(
        @Part avatar: okhttp3.MultipartBody.Part
    ): ApiResponse<AvatarUploadData>

    /** POST /auth/logout — invalidate server-side token */
    @POST("auth/logout")
    suspend fun logOut(): ApiResponse<Any>

    /** DELETE /users/account — permanent account deletion */
    @DELETE("users/account")
    suspend fun deleteAccount(): ApiResponse<Any>

    @POST("auth/fcm-token")
    suspend fun updateFcmToken(@Body dto: Map<String, String>): ApiResponse<Any>

    // ── MPIN endpoints ─────────────────────────────────────────

    /** GET /auth/check-mpin?mobile=+91XXXXXXXXXX */
    @GET("auth/check-mpin")
    suspend fun checkMpin(@Query("mobile") mobile: String): ApiResponse<CheckMpinData>

    /** POST /auth/login-mpin — primary login for returning users */
    @POST("auth/login-mpin")
    suspend fun loginMpin(@Body dto: LoginMpinRequest): VerifyOtpResponse

    /** POST /auth/create-mpin — after registration (JWT required) */
    @POST("auth/create-mpin")
    suspend fun createMpin(@Body dto: CreateMpinRequest): ApiResponse<CreateMpinData>

    /** POST /auth/forgot-mpin — triggers OTP (public) */
    @POST("auth/forgot-mpin")
    suspend fun forgotMpin(@Body dto: ForgotMpinRequest): ApiResponse<Any>

    /** POST /auth/reset-mpin — OTP + new MPIN, returns JWT (public) */
    @POST("auth/reset-mpin")
    suspend fun resetMpin(@Body dto: ResetMpinRequest): VerifyOtpResponse

    /** POST /auth/change-mpin — current + new MPIN (JWT required) */
    @POST("auth/change-mpin")
    suspend fun changeMpin(@Body dto: ChangeMpinRequest): ApiResponse<Any>

    @GET("users/me/learning-progress")
    suspend fun getLearningProgress(): ApiResponse<LearningProgressDto>
}

// ── MPIN DTOs ──────────────────────────────────────────────────

data class CheckMpinData(
    val hasMpin: Boolean = false,
    val isLocked: Boolean = false,
    val lockedUntilSeconds: Int = 0
)

data class LoginMpinRequest(val mobile: String, val mpin: String)

data class CreateMpinRequest(val mpin: String)

data class CreateMpinData(val mpinCreated: Boolean = false)

data class ForgotMpinRequest(val mobile: String)

data class ResetMpinRequest(
    val mobile: String,
    val otp: String,
    val newMpin: String
)

data class ChangeMpinRequest(
    val currentMpin: String,
    val newMpin: String
)

// ── DTOs ──────────────────────────────────────────────────────
data class UpdateProfileRequest(
    val name:        String?  = null,
    val email:       String?  = null,
    val bio:         String?  = null,
    val district:    String?  = null,
    val state:       String?  = null,
    @SerializedName("target_year")          val targetYear:          Int?     = null,
    @SerializedName("prep_level")           val prepLevel:           String?  = null,
    @SerializedName("onboarding_completed") val onboardingCompleted: Boolean? = null,
    @SerializedName("daily_goal_mins")      val dailyGoalMins:       Int?     = null
)

data class SendOtpRequest(val mobile: String)

data class SendOtpResponse(val success: Boolean, val message: String, val data: SendOtpData? = null)

data class SendOtpData(
    val isNewUser: Boolean = false,
    // WhatsApp delivers the OTP — no otp field in response
)

data class VerifyOtpRequest(val mobile: String, val otp: String)

data class VerifyOtpResponse(
    val success: Boolean, val message: String,
    val data: VerifyOtpData? = null, val timestamp: String? = null
)

data class VerifyOtpData(
    val isNewUser: Boolean    = false,
    val tempToken: String?    = null,
    val accessToken: String?  = null,
    val refreshToken: String? = null,
    val hasMpin: Boolean      = false,   // true = user already has MPIN set
)

data class RegisterResponse(
    val success: Boolean, val message: String,
    val data: RegisterData? = null
)

data class RegisterData(val accessToken: String, val refreshToken: String)

data class GetMeResponse(
    val success: Boolean, val message: String,
    val data: GetMeData? = null
)


data class AvatarUploadData(
    val url: String? = null
)

data class InProgressSessionDto(
    @com.google.gson.annotations.SerializedName("session_id")  val sessionId:          String = "",
    @com.google.gson.annotations.SerializedName("quiz_id")     val quizId:             String = "",
    @com.google.gson.annotations.SerializedName("quiz_title")  val quizTitle:          String = "",
    @com.google.gson.annotations.SerializedName("quiz_type")   val quizType:           String = "",
    @com.google.gson.annotations.SerializedName("quiz_subject") val quizSubject:       String = "",
    @com.google.gson.annotations.SerializedName("total_questions") val totalQuestions: Int    = 0,
    @com.google.gson.annotations.SerializedName("answers_so_far_count") val answeredCount: Int = 0,
)

data class LearningProgressDto(
    val inProgressSession: InProgressSessionDto? = null,
    val recentAttempts: List<com.example.bpscnotes.data.remote.api.RecentQuizAttemptDto> = emptyList(),
)