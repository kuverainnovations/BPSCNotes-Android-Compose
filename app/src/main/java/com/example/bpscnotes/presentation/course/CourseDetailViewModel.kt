package com.example.bpscnotes.presentation.course

import com.example.bpscnotes.core.network.toUserMessage

import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.Chapter
import com.example.bpscnotes.data.remote.api.CompleteLessonRequest
import com.example.bpscnotes.data.remote.api.CourseDetailResponse
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.core.network.CacheInvalidator
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.SubmitReviewRequest
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

data class CourseDetailUiState(
    val course: CourseDto? = null,
    val chapters: List<Chapter> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val enrollSuccess: Boolean = false,
    val isEnrolling: Boolean = false,
    // Payment required
    val purchaseRequired: Boolean = false,
    val purchasePrice: Int = 0,
    val purchaseSessionId: String? = null,    // paymentSessionId → Cashfree SDK
    val purchaseProviderOrderId: String? = null, // Cashfree order ID for LaunchedEffect
    // Rating
    val showRatingSheet: Boolean = false,
    val isSubmittingRating: Boolean = false,
    val isRatingSubmitted: Boolean = false,
    val ratingError: String? = null,
    val userCoins: Int = 0,
    // Certificate sharing — actual generated PDF, not just text
    val certificateUrl: String? = null,
    val certificateId: String? = null,
    val isDownloadingCert: Boolean = false,
    val certError: String? = null,
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val api: CoursesApiService,
    private val authApi: AuthApiService,
    private val certificatesApi: com.example.bpscnotes.data.remote.api.CertificatesApiService,
    private val bus: RefreshEventBus,
    private val cacheInvalidator: CacheInvalidator,
    private val okHttpClient: OkHttpClient,
    val coinsConfig: com.example.bpscnotes.core.config.CoinsConfigRepository,
    @ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    private var activeCourseId: String = ""

    init {
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.CourseProgressChanged ->
                        if (event.courseId == activeCourseId) load(activeCourseId)
                    is RefreshEvent.LessonCompleted ->
                        if (activeCourseId.isNotEmpty()) load(activeCourseId)
                    else -> {}
                }
            }
        }
    }

    companion object {
        /**
         * Survives ViewModel recreation (back-stack) within the same process.
         * Prevents the "Rate this Course" banner from reappearing after
         * the user navigates away and returns.
         */
        private val reviewedCourseIds = mutableSetOf<String>()
    }

    // ── Load ──────────────────────────────────────────────────

    fun load(courseId: String) {
        activeCourseId = courseId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getCourseDetail(courseId)
                val dataObj  = response.data ?: run {
                    _uiState.update { it.copy(isLoading = false, error = "No data returned") }
                    return@launch
                }
                val detail = Gson().fromJson(dataObj, CourseDetailResponse::class.java)
                if (detail?.course == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Course data missing") }
                    return@launch
                }
                val sortedChapters = (detail.course.chapters ?: emptyList())
                    .sortedBy { it.sortOrder }
                    .map { ch -> ch.copy(lessons = ch.lessons?.sortedBy { it.sort_order } ?: emptyList()) }

                val userCoinsVal = kotlinx.coroutines.supervisorScope {
                    try { authApi.getMe().data?.user?.coins ?: 0 } catch (_: Exception) { 0 }
                }

                // FIX: "Share Certificate" was always sharing plain text
                // because the screen never had the actual certificate
                // PDF URL. Look it up from GET /users/certificates.
                val certUrl = kotlinx.coroutines.supervisorScope {
                    try {
                        certificatesApi.getCertificates().data?.certificates
                            ?.firstOrNull { it.courseId == courseId }
                    } catch (e: Exception) {
                        Log.w("CourseDetailVM", "getCertificates: ${e.message}")
                        null
                    }
                }

                _uiState.update {
                    it.copy(
                        course            = detail.course,
                        chapters          = sortedChapters,
                        isLoading         = false,
                        userCoins         = userCoinsVal,
                        certificateUrl    = certUrl?.certificateUrl,
                        certificateId     = certUrl?.id,
                        // Restore submitted state if user already reviewed this course
                        isRatingSubmitted = reviewedCourseIds.contains(courseId)
                    )
                }
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "load: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load course")) }
            }
        }
    }

    // ── Enroll ────────────────────────────────────────────────

    // FIX Issue 2/5: coins cannot be used for purchases — always pass 0
    fun enroll(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnrolling = true, purchaseRequired = false) }
            try {
                api.enrollCourse(courseId, com.example.bpscnotes.data.remote.api.EnrollCourseRequest(coinsToApply = 0))
                _uiState.update { it.copy(isEnrolling = false, enrollSuccess = true) }
                bus.emit(RefreshEvent.CourseEnrolled)
                cacheInvalidator.evict()           // stale enrollment data must not be served
                load(courseId)
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 402) {
                    // Parse payment required response
                    try {
                        val body = e.response()?.errorBody()?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val data = json.optJSONObject("data") ?: json
                        val price           = data.optInt("price", 0)
                        val sessionId       = data.optString("paymentSessionId").takeIf { it.isNotBlank() }
                        val providerOrderId = data.optString("providerOrderId").takeIf { it.isNotBlank() }
                        _uiState.update { it.copy(
                            isEnrolling             = false,
                            purchaseRequired        = true,
                            purchasePrice           = price,
                            purchaseSessionId       = sessionId,
                            purchaseProviderOrderId = providerOrderId,
                        )}
                    } catch (_: Exception) {
                        _uiState.update { it.copy(isEnrolling = false, error = "Purchase required") }
                    }
                } else {
                    Log.e("CourseDetailVM", "enroll: ${e.message}", e)
                    _uiState.update { it.copy(isEnrolling = false, error = e.message) }
                }
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "enroll: ${e.message}", e)
                _uiState.update { it.copy(isEnrolling = false, error = e.message) }
            }
        }
    }

    // ── Complete lesson ───────────────────────────────────────

    fun completeLesson(courseId: String, lessonId: String, watchSecs: Int = 0) {
        viewModelScope.launch {
            try {
                api.completeCourseLesson(courseId, lessonId, CompleteLessonRequest(watchSecs))
                load(courseId)
            } catch (e: Exception) {
                Log.w("CourseDetailVM", "completeLesson failed: ${e.message}")
            }
        }
    }

    // ── Rating sheet ──────────────────────────────────────────

    fun showRatingSheet() {
        _uiState.update { it.copy(showRatingSheet = true, ratingError = null) }
    }

    fun dismissRatingSheet() {
        _uiState.update { it.copy(showRatingSheet = false, ratingError = null) }
    }

    fun submitRating(courseId: String, rating: Int, comment: String) {
        if (rating < 1 || rating > 5) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingRating = true, ratingError = null) }
            try {
                api.submitCourseReview(courseId, SubmitReviewRequest(rating, comment.trim()))

                // Mark as reviewed in the companion set so it persists on back-navigation
                reviewedCourseIds.add(courseId)

                _uiState.update {
                    it.copy(
                        isSubmittingRating = false,
                        showRatingSheet    = false,
                        isRatingSubmitted  = true
                    )
                }
                // Reload so the new review appears in the list
                load(courseId)
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "submitRating: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isSubmittingRating = false,
                        ratingError        = "Could not submit review. Please try again."
                    )
                }
            }
        }
    }

    // ── Clear messages ─────────────────────────────────────────

    fun clearMessages() {
        _uiState.update { it.copy(enrollSuccess = false, error = null) }
    }

    fun clearPurchaseRequired() {
        _uiState.update { it.copy(purchaseRequired = false) }
    }

    // ── Certificate sharing ─────────────────────────────────────
    // FIX: previously "Share Certificate" only ever shared a plain text
    // message ("I just completed '...' on BPSCNotes!") regardless of
    // whether a certificate existed. Now download the actual generated
    // PDF and hand back a content:// URI (via FileProvider) so the
    // share sheet attaches the real certificate file.
    //
    // Returns null if there's no certificate URL yet, or the download
    // fails — caller should fall back to text-only sharing in that case.
    suspend fun downloadCertificateForShare(certUrl: String, certificateId: String): android.net.Uri? {
        return withContext(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isDownloadingCert = true, certError = null) }
                val dir = File(appContext.cacheDir, "certificates").apply { mkdirs() }
                val file = File(dir, "certificate_$certificateId.pdf")

                if (!file.exists() || file.length() == 0L) {
                    val request = Request.Builder().url(certUrl).build()
                    val response = okHttpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception("Download failed: HTTP ${response.code}")
                    }
                    response.body?.byteStream()?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw Exception("Empty certificate response")
                }

                FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "downloadCertificateForShare: ${e.message}", e)
                _uiState.update { it.copy(certError = "Could not download certificate") }
                null
            } finally {
                _uiState.update { it.copy(isDownloadingCert = false) }
            }
        }
    }
}