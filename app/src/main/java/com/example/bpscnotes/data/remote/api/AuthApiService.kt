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
    @SerializedName("target_year") val targetYear: Int?    = null,
    @SerializedName("prep_level")  val prepLevel:  String? = null
)

data class SendOtpRequest(val mobile: String)

data class SendOtpResponse(val success: Boolean, val message: String, val data: SendOtpData? = null)

data class SendOtpData(
    val isNewUser: Boolean = false,
    // TEMP: backend returns the real OTP while MSG91/DLT template approval is
    // pending, so the app isn't blocked on SMS delivery. Remove once DLT is sorted.
    val otp: String? = null,
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