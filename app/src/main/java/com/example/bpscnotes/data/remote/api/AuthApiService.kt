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

    /** Step 3 — register with name */
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    /** GET /auth/me — current user profile */
    @GET("auth/me")
    suspend fun getMe(): GetMeResponse

    /** GET /exams */
    @GET("exams")
    suspend fun getExams(): ApiResponse<ExamsResponseData>

    /** PUT /users/exam-target */
    @PUT("users/exam-target")
    suspend fun saveExamTarget(@Body body: ExamTargetRequest): ApiResponse<Any>

    /** PATCH /users/profile — update name, email, bio, district, etc. */
    @PATCH("auth/users/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ApiResponse<GetMeData>

    /** POST /auth/logout — invalidate server-side token */
    @POST("auth/logout")
    suspend fun logOut(): ApiResponse<Any>

    /** DELETE /users/account — permanent account deletion */
    @DELETE("users/account")
    suspend fun deleteAccount(): ApiResponse<Any>

    @retrofit2.http.POST("auth/fcm-token")
    suspend fun updateFcmToken(@retrofit2.http.Body dto: Map<String, String>): ApiResponse<Any>
}

// ── DTOs ──────────────────────────────────────────────────────
data class UpdateProfileRequest(
    val name:        String?  = null,
    val email:       String?  = null,
    val bio:         String?  = null,
    val district:    String?  = null,
    val state:       String?  = null,
    @SerializedName("target_year") val targetYear: Int?    = null,
    @SerializedName("prep_level")  val prepLevel:  String? = null
)

data class SendOtpRequest(val mobile: String)

data class SendOtpResponse(val success: Boolean, val message: String)

data class VerifyOtpRequest(val mobile: String, val otp: String)

data class VerifyOtpResponse(
    val success: Boolean, val message: String,
    val data: VerifyOtpData? = null, val timestamp: String? = null
)

data class VerifyOtpData(
    val isNewUser: Boolean    = false,
    val tempToken: String?    = null,
    val accessToken: String?  = null,
    val refreshToken: String? = null
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