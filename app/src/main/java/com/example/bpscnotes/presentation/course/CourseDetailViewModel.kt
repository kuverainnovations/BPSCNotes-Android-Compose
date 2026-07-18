package com.example.bpscnotes.presentation.course

import com.example.bpscnotes.core.network.toUserMessage

import android.app.Activity
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpscnotes.app.BuildConfig
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.Chapter
import com.example.bpscnotes.data.remote.api.CompleteLessonRequest
import com.example.bpscnotes.data.remote.api.CourseDetailResponse
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.core.network.CacheInvalidator
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.ConfirmCoursePurchaseRequest
import com.example.bpscnotes.data.remote.api.GPlayCourseVerifyRequest
import com.example.bpscnotes.data.remote.api.SubmitReviewRequest
import com.example.bpscnotes.presentation.payment.GPlayCoursePurchaseManager
import com.example.bpscnotes.presentation.payment.GPlayPurchaseOutcome
import com.example.bpscnotes.presentation.payment.PendingCashfreeSession
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
    val purchasePrice: Double = 0.0,
    val purchaseSessionId: String? = null,          // paymentSessionId → Cashfree SDK (debug builds only)
    val purchaseProviderOrderId: String? = null,    // Cashfree order ID for LaunchedEffect
    val purchasePaymentEnvironment: String = "sandbox", // 'sandbox' | 'production' → CFSession.Environment
    val gplayPurchaseCourseId: String? = null,      // non-null triggers Play Billing launch (release builds only)
    val isGPlayPurchasing: Boolean = false,
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
    // Save / bookmark
    val isSaved: Boolean = false,
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
    private val gplay: GPlayCoursePurchaseManager,
    private val tokenStore: com.example.bpscnotes.data.local.TokenStore,
    @ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    private var activeCourseId: String = ""

    // User choice billing (release builds): the 402 response's Cashfree
    // session is held here — NOT in uiState, where a non-null
    // purchaseSessionId immediately auto-launches the Cashfree SDK via the
    // screen's LaunchedEffect. It's only promoted to uiState if the user
    // picks Cashfree on Google's billing-choice screen.
    private var pendingCashfreeSession: PendingCashfreeSession? = null

    // Token from UserChoiceDetails — rides along on the Cashfree confirm
    // call so the backend can report the payment to the Play Developer API.
    // Cleared on payment failure/cancel so a stale token can never be
    // attached to a later, unrelated payment.
    private var pendingExternalTxToken: String? = null

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

                val savedVal = kotlinx.coroutines.supervisorScope {
                    try {
                        api.getSavedCourses().data?.courses?.any { it.id == courseId } ?: false
                    } catch (e: Exception) {
                        Log.w("CourseDetailVM", "getSavedCourses: ${e.message}")
                        false
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
                        isRatingSubmitted = reviewedCourseIds.contains(courseId),
                        isSaved           = savedVal
                    )
                }
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "load: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load course")) }
            }
        }
    }

    // ── Enroll ────────────────────────────────────────────────

    // FIX: accepts coinsToApply from the purchase dialog coin slider
    fun enroll(courseId: String, coinsToApply: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnrolling = true, purchaseRequired = false) }
            try {
                api.enrollCourse(courseId, com.example.bpscnotes.data.remote.api.EnrollCourseRequest(coinsToApply = coinsToApply))
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
                        val price = data.optDouble("price", 0.0)
                        if (BuildConfig.DEBUG) {
                            val sessionId           = data.optString("paymentSessionId").takeIf { it.isNotBlank() }
                            val providerOrderId     = data.optString("providerOrderId").takeIf { it.isNotBlank() }
                            val paymentEnvironment  = data.optString("paymentEnvironment").takeIf { it.isNotBlank() } ?: "sandbox"
                            _uiState.update { it.copy(
                                isEnrolling                 = false,
                                purchaseRequired            = true,
                                purchasePrice               = price,
                                purchaseSessionId           = sessionId,
                                purchaseProviderOrderId     = providerOrderId,
                                purchasePaymentEnvironment  = paymentEnvironment,
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
                                purchasePrice          = price,
                                gplayPurchaseCourseId = courseId,
                            )}
                        }
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

    // ── Save / Unsave (Wishlist) ────────────────────────────────

    fun toggleSave() {
        val courseId = _uiState.value.course?.id ?: return
        val isSaved  = _uiState.value.isSaved

        // Optimistic update
        _uiState.update { it.copy(isSaved = !isSaved) }

        viewModelScope.launch {
            try {
                if (isSaved) api.unsaveCourse(courseId) else api.saveCourse(courseId)
                // /courses/saved is cached for 5 min (Cache-Control: public,
                // max-age=300) — without evicting it, navigating back to the
                // course list would show this course's old saved-state until
                // that cache expires.
                cacheInvalidator.evict()
                // MyLearningViewModel (the list) is a separate ViewModel instance
                // scoped to a different nav destination — it never sees this
                // toggle unless told. Without this, "back to list" kept showing
                // the pre-toggle saved state until the list's own load() ran.
                bus.emit(RefreshEvent.CourseSaveChanged(courseId, !isSaved))
            } catch (e: Exception) {
                Log.w("CourseDetailVM", "toggleSave: ${e.message}")
                _uiState.update { it.copy(isSaved = isSaved) } // revert on failure
            }
        }
    }

    // ── Clear messages ─────────────────────────────────────────

    fun clearMessages() {
        _uiState.update { it.copy(enrollSuccess = false, error = null) }
    }

    fun clearPurchaseRequired() {
        _uiState.update { it.copy(
            purchaseRequired        = false,
            purchasePrice           = 0.0,
            purchaseSessionId       = null,
            purchaseProviderOrderId = null,
        )}
    }

    fun clearGPlayPurchase() {
        _uiState.update { it.copy(gplayPurchaseCourseId = null) }
    }

    // Release-build counterpart to the Cashfree purchaseSessionId flow above.
    // GPlayCoursePurchaseManager is shared with MyLearningViewModel — this
    // method is the only place course-purchase GPlay logic is duplicated
    // (once, per ViewModel, calling into the shared manager) rather than
    // reimplemented independently.
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
                    is GPlayPurchaseOutcome.PlayPurchase ->
                        verifyGPlayCoursePurchase(courseId, outcome.purchase.purchaseToken, clientPriceInr)

                    is GPlayPurchaseOutcome.AlternativeBillingChosen -> {
                        // User picked Cashfree on Google's billing-choice
                        // screen. Promote the 402's Cashfree session into
                        // uiState — the screen's LaunchedEffect launches the
                        // Cashfree SDK from there — and hold the token for
                        // the confirm call.
                        val session = pendingCashfreeSession
                        if (session == null) {
                            _uiState.update { it.copy(
                                isGPlayPurchasing = false,
                                error = "Payment option unavailable right now. Please try again."
                            )}
                        } else {
                            pendingExternalTxToken = outcome.externalTransactionToken
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
                api.verifyGPlayCoursePurchase(courseId, GPlayCourseVerifyRequest(
                    purchaseToken   = purchaseToken,
                    clientPriceInr  = clientPriceInr
                ))
                bus.emit(RefreshEvent.CourseEnrolled)
                cacheInvalidator.evict()
                _uiState.update { it.copy(isGPlayPurchasing = false, enrollSuccess = true) }
                load(courseId)
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isGPlayPurchasing = false,
                    error = "Payment received but enrollment failed. Contact support with purchase token: ${purchaseToken.take(16)}…"
                )}
            }
        }
    }

    fun confirmCoursePurchase(cfPaymentId: String) {
        val courseId = _uiState.value.course?.id ?: return
        viewModelScope.launch {
            try {
                api.confirmCoursePurchase(courseId, ConfirmCoursePurchaseRequest(
                    cfPaymentId              = cfPaymentId,
                    paymentMethod            = "upi",
                    externalTransactionToken = pendingExternalTxToken
                ))
                pendingExternalTxToken = null
                bus.emit(RefreshEvent.CourseEnrolled)
                cacheInvalidator.evict()
                _uiState.update { it.copy(enrollSuccess = true) }
                load(courseId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Payment received but enrollment failed. Contact support. Ref: $cfPaymentId") }
            }
        }
    }

    fun handleCoursePaymentFailure(code: Int, message: String) {
        // A failed/cancelled Cashfree attempt invalidates the user-choice
        // token and the promoted session — a retry mints fresh ones via a
        // new enroll + choice-screen round; stale ones must never ride a
        // later confirm call.
        pendingExternalTxToken = null
        pendingCashfreeSession = null
        if (code == 0) return  // user cancelled — silent
        _uiState.update { it.copy(error = "Payment failed: $message") }
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