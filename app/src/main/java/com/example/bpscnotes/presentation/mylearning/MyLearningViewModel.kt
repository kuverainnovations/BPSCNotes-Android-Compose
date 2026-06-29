package com.example.bpscnotes.presentation.mylearning

import com.example.bpscnotes.core.network.toUserMessage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.network.CacheInvalidator
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.CoursesApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyLearningUiState(
    val storeCourses:    List<CourseDto> = emptyList(),  // all courses NOT enrolled (for Store tab)
    val enrolledCourses: List<CourseDto> = emptyList(),  // courses user has enrolled in
    val savedCourses:    List<CourseDto> = emptyList(),  // courses user has saved/wishlisted
    val savedCourseIds:  Set<String>     = emptySet(),   // for quick O(1) lookup in UI
    val userCoins:       Int             = 0,
    val subjects:        List<String>    = emptyList(),
    val certificateUrls: Map<String, String> = emptyMap(), // courseId -> certificate_url (only present once generated)
    val isLoading:       Boolean         = true,
    val isEnrolling:     Boolean         = false,
    val enrollSuccess:   String?         = null,
    val justEnrolledId:  String?         = null,   // triggers tab switch in Screen
    val saveToast:       String?         = null,
    val error:           String?         = null,
    // Set when enroll() hits a 402 for a paid course — launch Cashfree SDK directly.
    val purchaseRequired:        Boolean = false,
    val purchasePrice:           Double  = 0.0,
    val purchaseSessionId:       String? = null,
    val purchaseProviderOrderId: String? = null,
    val purchasePaymentEnvironment: String = "sandbox",
    val purchaseCourseId:        String? = null,
    val purchaseCourseTitle:     String  = "",
)

@HiltViewModel
class MyLearningViewModel @Inject constructor(
    private val coursesApi: CoursesApiService,
    private val authApi: AuthApiService,
    private val statsApi:   com.example.bpscnotes.data.remote.api.UserStatsApiService,
    private val certificatesApi: com.example.bpscnotes.data.remote.api.CertificatesApiService,
    private val bus:        RefreshEventBus,
    val coinsConfig: com.example.bpscnotes.core.config.CoinsConfigRepository,
    private val cacheInvalidator: CacheInvalidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyLearningUiState())
    val uiState: StateFlow<MyLearningUiState> = _uiState.asStateFlow()

    init {
        load()
        // Auto-refresh when lesson is completed or course enrolled from any screen
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.LessonCompleted,
                    is RefreshEvent.CourseProgressChanged,
                    is RefreshEvent.CourseEnrolled -> load()
                    else -> {}
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // CRITICAL FIX: Use supervisorScope so one child failure doesn't crash others
                // Without supervisorScope, async{}.await() propagates exceptions to parent even
                // when inner try/catch is present, causing FATAL EXCEPTION in main thread
                val allCourses = kotlinx.coroutines.supervisorScope {
                    try { coursesApi.getCourses(limit = 100).data?.courses ?: emptyList() }
                    catch (e: Exception) {
                        Log.w("MyLearningVM", "getCourses failed: \${e.message}")
                        emptyList<com.example.bpscnotes.data.remote.api.CourseDto>()
                    }
                }
                val savedCourses = kotlinx.coroutines.supervisorScope {
                    try { coursesApi.getSavedCourses().data?.courses ?: emptyList() }
                    catch (e: Exception) {
                        Log.w("MyLearningVM", "getSavedCourses failed: \${e.message}")
                        emptyList<com.example.bpscnotes.data.remote.api.CourseDto>()
                    }
                }
                val userCoinsVal = kotlinx.coroutines.supervisorScope {
                    try { authApi.getMe().data?.user?.coins ?: 0 } catch (_: Exception) { 0 }
                }
                val savedIds        = savedCourses.map { it.id }.toSet()
                val enrolledCourses = allCourses
                    .filter { it.enrollment?.status in listOf("active", "completed") }
                    .sortedByDescending { it.enrollment?.enrolled_at ?: it.created_at }
                val enrolledIds     = enrolledCourses.map { it.id }.toSet()
                val storeCourses    = allCourses.filter { it.id !in enrolledIds }

                val subjectNames = kotlinx.coroutines.supervisorScope {
                    try {
                        val res = statsApi.getSubjects().data?.subjects ?: emptyList()
                        listOf("All") + res.map { it.name }
                    } catch (_: Exception) {
                        listOf("All", "Polity", "History", "Geography", "Economy", "Bihar GK", "Science")
                    }
                }

                // Fetch issued certificates — only courses with a generated
                // certificate_url will show the Download button (others
                // show "Completed" but no download until ready)
                val certUrls = kotlinx.coroutines.supervisorScope {
                    try {
                        certificatesApi.getCertificates().data?.certificates
                            ?.filter { !it.certificateUrl.isNullOrBlank() }
                            ?.associate { it.courseId to it.certificateUrl!! }
                            ?: emptyMap()
                    } catch (e: Exception) {
                        Log.w("MyLearningVM", "getCertificates failed: \${e.message}")
                        emptyMap()
                    }
                }
                _uiState.update {
                    it.copy(
                        storeCourses    = storeCourses,
                        enrolledCourses = enrolledCourses,
                        savedCourses    = savedCourses,
                        savedCourseIds  = savedIds,
                        userCoins       = userCoinsVal,
                        subjects        = subjectNames,
                        certificateUrls = certUrls,
                        isLoading       = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MyLearningVM", e.toUserMessage(""), e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load")) }
            }
        }
    }

    fun retry() = load()

    // ── Enroll ─────────────────────────────────────────────────
    // FIX: coinsToApply removed — real-money Cashfree payment only
    fun enroll(courseId: String, courseTitle: String = "") {
        enrollWithCoins(courseId, courseTitle, 0)
    }

    fun enrollWithCoins(courseId: String, courseTitle: String = "", coinsToApply: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnrolling = true, purchaseRequired = false) }
            try {
                coursesApi.enrollCourse(courseId, com.example.bpscnotes.data.remote.api.EnrollCourseRequest(coinsToApply = coinsToApply))
                cacheInvalidator.evict()
                load()
                _uiState.update { s ->
                    s.copy(
                        isEnrolling    = false,
                        enrollSuccess  = "Enrolled successfully!",
                        justEnrolledId = courseId
                    )
                }
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 402) {
                    try {
                        val body = e.response()?.errorBody()?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val data = json.optJSONObject("data") ?: json
                        val price              = data.optDouble("price", 0.0)
                        val sessionId          = data.optString("paymentSessionId").takeIf { it.isNotBlank() }
                        val providerOrderId    = data.optString("providerOrderId").takeIf { it.isNotBlank() }
                        val paymentEnvironment = data.optString("paymentEnvironment").takeIf { it.isNotBlank() } ?: "sandbox"
                        _uiState.update { it.copy(
                            isEnrolling                = false,
                            purchaseRequired           = true,
                            purchasePrice              = price,
                            purchaseSessionId          = sessionId,
                            purchaseProviderOrderId    = providerOrderId,
                            purchasePaymentEnvironment = paymentEnvironment,
                            purchaseCourseId           = courseId,
                            purchaseCourseTitle        = courseTitle
                        )}
                    } catch (_: Exception) {
                        _uiState.update { it.copy(isEnrolling = false, error = "Purchase required") }
                    }
                } else {
                    Log.e("MyLearningVM", "enroll: ${e.message}", e)
                    _uiState.update { it.copy(isEnrolling = false, error = e.toUserMessage("Enrollment failed")) }
                }
            } catch (e: Exception) {
                Log.e("MyLearningVM", "enroll: ${e.message}", e)
                _uiState.update { it.copy(isEnrolling = false, error = e.toUserMessage("Enrollment failed")) }
            }
        }
    }

    fun clearPurchaseRequired() {
        _uiState.update { it.copy(
            purchaseRequired        = false,
            purchaseSessionId       = null,
            purchaseProviderOrderId = null,
        )}
    }

    fun confirmCoursePurchase(courseId: String, cfPaymentId: String) {
        viewModelScope.launch {
            try {
                coursesApi.confirmCoursePurchase(
                    courseId,
                    com.example.bpscnotes.data.remote.api.ConfirmCoursePurchaseRequest(
                        cfPaymentId   = cfPaymentId,
                        paymentMethod = "upi"
                    )
                )
                bus.emit(RefreshEvent.CourseEnrolled)
                cacheInvalidator.evict()
                load()
                _uiState.update { it.copy(justEnrolledId = courseId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Payment received but enrollment failed. Contact support. Ref: $cfPaymentId") }
            }
        }
    }

    fun handleCoursePaymentFailure(code: Int, message: String) {
        if (code == 0) return
        _uiState.update { it.copy(error = "Payment failed: $message") }
    }

    // ── Save / Unsave (Wishlist) ────────────────────────────────
    fun toggleSave(courseId: String) {
        val isSaved = _uiState.value.savedCourseIds.contains(courseId)

        // Optimistic update
        _uiState.update { state ->
            val newIds = if (isSaved) state.savedCourseIds - courseId
            else         state.savedCourseIds + courseId
            state.copy(savedCourseIds = newIds)
        }

        viewModelScope.launch {
            try {
                if (isSaved) {
                    coursesApi.unsaveCourse(courseId)
                    _uiState.update { it.copy(saveToast = "Removed from saved") }
                } else {
                    coursesApi.saveCourse(courseId)
                    _uiState.update { it.copy(saveToast = "Course saved! View in My Courses → Saved tab") }
                }
                // Re-fetch saved list silently so savedCourses list is accurate
                val updated = try { coursesApi.getSavedCourses().data?.courses ?: emptyList() } catch (_: Exception) { emptyList() }
                _uiState.update { it.copy(savedCourses = updated, savedCourseIds = updated.map { c -> c.id }.toSet()) }
            } catch (e: Exception) {
                // Revert optimistic update on failure
                _uiState.update { state ->
                    val revertIds = if (isSaved) state.savedCourseIds + courseId
                    else          state.savedCourseIds - courseId
                    state.copy(savedCourseIds = revertIds, error = e.toUserMessage("Save failed"))
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(enrollSuccess = null, saveToast = null, error = null) }
    }

    fun clearJustEnrolled() {
        _uiState.update { it.copy(justEnrolledId = null) }
    }
}