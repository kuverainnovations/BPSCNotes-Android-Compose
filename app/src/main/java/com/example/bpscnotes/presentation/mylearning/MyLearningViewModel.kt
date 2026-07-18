package com.example.bpscnotes.presentation.mylearning

import com.example.bpscnotes.core.network.toUserMessage

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpscnotes.app.BuildConfig
import com.example.bpscnotes.core.network.CacheInvalidator
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.GPlayCourseVerifyRequest
import com.example.bpscnotes.presentation.payment.GPlayCoursePurchaseManager
import com.example.bpscnotes.presentation.payment.GPlayPurchaseOutcome
import com.example.bpscnotes.presentation.payment.PendingCashfreeSession
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
    val gplayPurchaseCourseId:   String? = null,  // non-null triggers Play Billing launch (release builds only)
    val isGPlayPurchasing:       Boolean = false,
)

@HiltViewModel
class MyLearningViewModel @Inject constructor(
    private val coursesApi: CoursesApiService,
    private val authApi: AuthApiService,
    private val statsApi:   com.example.bpscnotes.data.remote.api.UserStatsApiService,
    private val certificatesApi: com.example.bpscnotes.data.remote.api.CertificatesApiService,
    private val bus:        RefreshEventBus,
    val coinsConfig: com.example.bpscnotes.core.config.CoinsConfigRepository,
    private val cacheInvalidator: CacheInvalidator,
    private val gplay: GPlayCoursePurchaseManager,
    private val tokenStore: com.example.bpscnotes.data.local.TokenStore
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
                    // Course Details screen holds its own CourseDetailViewModel —
                    // it can't reach this list's state directly. Patch the id set
                    // in place instead of a full load() (same pattern as
                    // CaBookmarkChanged for Current Affairs).
                    is RefreshEvent.CourseSaveChanged -> {
                        _uiState.update { state ->
                            val newIds = if (event.isSaved) state.savedCourseIds + event.courseId
                                         else               state.savedCourseIds - event.courseId
                            state.copy(savedCourseIds = newIds)
                        }
                    }
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
                        val price = data.optDouble("price", 0.0)
                        if (BuildConfig.DEBUG) {
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
                        } else {
                            // Release build — Google Play Billing. The Cashfree
                            // session is kept aside (not in uiState) in case the
                            // user picks Cashfree on Google's billing-choice
                            // screen; the Play price still comes from
                            // ProductDetails (see startGPlayCoursePurchase).
                            pendingCashfreeSession = PendingCashfreeSession.fromJson(data)
                            _uiState.update { it.copy(
                                isEnrolling           = false,
                                purchaseRequired      = true,
                                purchasePrice         = price,
                                purchaseCourseId      = courseId,
                                purchaseCourseTitle   = courseTitle,
                                gplayPurchaseCourseId = courseId,
                            )}
                        }
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

    // User choice billing (release builds): the 402's Cashfree session is
    // held here — NOT in uiState, where a non-null purchaseSessionId
    // auto-launches the Cashfree SDK via the screen's LaunchedEffect. Only
    // promoted to uiState if the user picks Cashfree on Google's
    // billing-choice screen. Shared shape with CourseDetailViewModel via
    // presentation/payment/PendingCashfreeSession.
    private var pendingCashfreeSession: PendingCashfreeSession? = null

    // Token from UserChoiceDetails, keyed by courseId: this ViewModel is a
    // single shared instance handling purchases for MANY courses, so an
    // unkeyed token could leak from a failed course-A attempt onto a later
    // course-B confirm and mis-report that payment to Play. Cleared on
    // payment failure/cancel.
    private var pendingExternalTx: Pair<String, String>? = null   // courseId → token

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
                        cfPaymentId              = cfPaymentId,
                        paymentMethod            = "upi",
                        // Only attach the token if it belongs to THIS course —
                        // see pendingExternalTx declaration.
                        externalTransactionToken = pendingExternalTx?.takeIf { it.first == courseId }?.second
                    )
                )
                if (pendingExternalTx?.first == courseId) pendingExternalTx = null
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
        // A failed/cancelled Cashfree attempt invalidates the user-choice
        // token and the held session — stale ones must never ride a later
        // confirm call (see pendingExternalTx declaration).
        pendingExternalTx = null
        pendingCashfreeSession = null
        if (code == 0) return
        _uiState.update { it.copy(error = "Payment failed: $message") }
    }

    fun clearGPlayPurchase() {
        _uiState.update { it.copy(gplayPurchaseCourseId = null) }
    }

    // Release-build counterpart to the Cashfree purchaseSessionId flow above.
    // Same GPlayCoursePurchaseManager instance CourseDetailViewModel uses —
    // this is the only place course-purchase GPlay logic is duplicated
    // (once, per ViewModel, calling into the shared manager).
    fun startGPlayCoursePurchase(activity: Activity, courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGPlayPurchasing = true, error = null) }
            try {
                val details = gplay.queryProductDetails(courseId)
                if (details == null) {
                    _uiState.update { it.copy(isGPlayPurchasing = false, error = "This course isn't available on Google Play yet. Please try again shortly.") }
                    return@launch
                }
                // Subscribe BEFORE launching the flow: purchasesFlow and
                // userChoiceFlow have replay=0, so an emission that lands
                // before a collector attaches is dropped and the await
                // would hang forever. yield() lets the async collector
                // attach before the billing callbacks can fire.
                val outcomeDeferred = async { gplay.awaitPurchaseOrUserChoice(courseId) }
                kotlinx.coroutines.yield()
                val result = gplay.launchPurchase(activity, details, obfuscatedAccountId = tokenStore.getUserId() ?: "")
                if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                    outcomeDeferred.cancel()
                    _uiState.update { it.copy(isGPlayPurchasing = false, error = "Could not open Google Play checkout (${result.debugMessage})") }
                    return@launch
                }
                val clientPriceInr = gplay.priceInrOf(details)
                when (val outcome = outcomeDeferred.await()) {
                    is GPlayPurchaseOutcome.Cancelled -> {
                        // User closed the Play sheet (or it failed) — reset
                        // the spinner; silent on plain cancel.
                        _uiState.update { it.copy(
                            isGPlayPurchasing = false,
                            error = if (outcome.responseCode == com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED) null
                                    else "Payment could not be completed. Please try again."
                        )}
                    }

                    is GPlayPurchaseOutcome.PlayPurchase ->
                        verifyGPlayCoursePurchase(courseId, outcome.purchase.purchaseToken, clientPriceInr)

                    is GPlayPurchaseOutcome.AlternativeBillingChosen -> {
                        // User picked Cashfree on Google's billing-choice
                        // screen — promote the held Cashfree session into
                        // uiState so the screen launches the Cashfree SDK,
                        // and keep the token (keyed to this course) for the
                        // confirm call.
                        val session = pendingCashfreeSession
                        if (session == null) {
                            _uiState.update { it.copy(
                                isGPlayPurchasing = false,
                                error = "Payment option unavailable right now. Please try again."
                            )}
                        } else {
                            pendingExternalTx = courseId to outcome.externalTransactionToken
                            _uiState.update { it.copy(
                                isGPlayPurchasing          = false,
                                purchaseSessionId          = session.sessionId,
                                purchaseProviderOrderId    = session.providerOrderId,
                                purchasePaymentEnvironment = session.environment,
                            )}
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGPlayPurchasing = false, error = e.message ?: "Purchase failed") }
            }
        }
    }

    private fun verifyGPlayCoursePurchase(courseId: String, purchaseToken: String, clientPriceInr: Double) {
        viewModelScope.launch {
            try {
                coursesApi.verifyGPlayCoursePurchase(courseId, GPlayCourseVerifyRequest(
                    purchaseToken  = purchaseToken,
                    clientPriceInr = clientPriceInr
                ))
                bus.emit(RefreshEvent.CourseEnrolled)
                cacheInvalidator.evict()
                load()
                _uiState.update { it.copy(isGPlayPurchasing = false, justEnrolledId = courseId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isGPlayPurchasing = false,
                    error = "Payment received but enrollment failed. Contact support with purchase token: ${purchaseToken.take(16)}…"
                )}
            }
        }
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
                // /courses/saved is served with Cache-Control: public, max-age=300 —
                // without evicting it here first, this re-fetch reliably returns the
                // OkHttp disk-cached response from BEFORE the save/unsave above, which
                // is exactly what made the toggle look like it "auto-deselected"
                // immediately after tapping Save.
                cacheInvalidator.evict()

                // Re-fetch saved list silently so savedCourses list (full course
                // objects, for the Saved tab) is accurate. A failure here must NOT
                // touch savedCourseIds — it has nothing to do with whether the
                // save/unsave above succeeded, and previously overwrote it to an
                // empty set on any transient error, making the toggle look like it
                // "auto-deselected" even though the save had gone through.
                try {
                    val updated = coursesApi.getSavedCourses().data?.courses ?: emptyList()
                    _uiState.update { it.copy(savedCourses = updated, savedCourseIds = updated.map { c -> c.id }.toSet()) }
                } catch (_: Exception) { /* keep optimistic state; next load() will resync */ }
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