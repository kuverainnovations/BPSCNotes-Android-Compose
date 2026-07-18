package com.example.bpscnotes.presentation.studymaterials

import com.example.bpscnotes.core.network.toUserMessage
import com.example.bpscnotes.presentation.payment.GPlayPurchaseOutcome

import com.example.bpscnotes.data.remote.api.StudyMaterialsApiService

import android.app.Application
import android.app.DownloadManager
import android.os.Build
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.*
import com.google.gson.Gson
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// StudyMaterialsViewModel
//
// FIXES:
// 1. NetworkOnMainThreadException — S3 upload was done on the main
//    thread via OkHttp's synchronous .execute().
//    Fix: replaced with Retrofit multipart upload on Dispatchers.IO.
//
// 2. No AWS required — upload goes directly to our own server at
//    POST /study-materials/upload (multipart/form-data).
//    The server stores files on its local disk and returns a public URL.
// ════════════════════════════════════════════════════════════

// Permission state for download on Android 9 and below
data class StudyMaterialsUiState(
    val needsStoragePermission: Boolean  = false,
    val pendingDownloadMaterial: com.example.bpscnotes.data.remote.api.StudyMaterialDto? = null,
    // Downloads history tab
    val downloadHistory:   List<com.example.bpscnotes.data.remote.api.DownloadHistoryItem> = emptyList(),
    val isLoadingHistory:  Boolean = false,
    // Purchased IDs — for PDF unlocking
    val purchasedIds:      Set<String> = emptySet(),
    // Purchase flow
    val purchasingId:      String? = null,
    val purchaseSuccess:   String? = null,
    val purchaseError:     String? = null,
    // Cashfree payment for marketplace purchase (Phase 3)
    val pendingPurchase:          InitPurchaseData? = null,  // set when requiresPayment=true — triggers Cashfree SDK
    val pendingPurchaseMaterialId: String? = null,
    val pendingPurchaseTitle:      String? = null,
    // Retained separately from pendingPurchase so confirmMaterialPurchase()
    // still has the order ID after consumePendingPurchase() clears pendingPurchase.
    val pendingPurchaseOrderId:    String? = null,
    val isConfirmingPurchase: Boolean = false,
    val userCoins: Int = 0,
    // ── Rating ──────────────────────────────────────────────
    val myRatingStars:    Int     = 0,      // 0 = not yet rated
    val myRatingReview:   String? = null,
    val isSubmittingRating: Boolean = false,
    val ratingSuccess:    String? = null,
    val ratingError:      String? = null,
    // List
    val materials:          List<StudyMaterialDto> = emptyList(),
    val isLoadingList:      Boolean                = true,
    val isRefreshing:       Boolean                = false,
    val listError:          String?                = null,
    val hasNextPage:        Boolean                = false,
    val currentPage:        Int                    = 1,
    val isLoadingMore:      Boolean                = false,

    // Pinned materials — fetched from a dedicated unpaginated endpoint
    // so pinned items always appear regardless of their downloads rank.
    val pinnedMaterials:    List<StudyMaterialDto> = emptyList(),
    val isLoadingPinned:    Boolean                = false,

    // Filters
    val selectedType:       MaterialType?          = null,
    val selectedSubject:    String                 = "All",
    val selectedLanguage:   String                 = "All",
    val searchQuery:        String                 = "",
    val sortBy:             String                 = "downloads",
    val showBookmarksOnly:  Boolean                = false,
    val subjects:           List<String>           = listOf("All"),

    // Stats
    val stats:              StatsData?             = null,

    // Detail sheet
    val selectedMaterial:   MaterialDetailData?    = null,
    val isLoadingDetail:    Boolean                = false,

    // Upload
    val showUploadSheet:    Boolean                = false,
    val showUploadCancelDialog: Boolean            = false,  // shown when user tries to close mid-upload
    val isUploading:        Boolean                = false,
    val uploadProgress:     Float                  = 0f,
    val uploadSuccess:      String?                = null,
    val uploadError:        String?                = null,
    // Upload form fields — kept in VM so they survive sheet close/reopen
    val uploadTitle:        String                 = "",
    val uploadDescription:  String                 = "",
    val uploadSubject:      String                 = "",
    val uploadAuthor:       String                 = "",
    val uploadTags:         String                 = "",
    val uploadType:         MaterialType           = MaterialType.PDF,
    val uploadIsPremium:    Boolean                = false,
    val uploadFreePages:    String                 = "3",
    val uploadPrice:        String                 = "0",
    val uploadLanguage:     String                 = "English",

    // My uploads tab
    val myUploads:          List<StudyMaterialDto> = emptyList(),
    val currentUserId:      String                 = "",
    val isLoadingMyUploads: Boolean                = false,

    // Download
    val downloadingId:      String?                = null,
    val downloadedIds:      Set<String>            = emptySet(),
    val localPaths:         Map<String, String>    = emptyMap(), // materialId → local file path

    // Bookmark (optimistic)
    val bookmarkedIds:      Set<String>            = emptySet(),

    // Toast
    val toastMessage:       String?                = null,

    // Marketplace rules popup — shown on first visit, or via (i) button
    val showRulesSheet:     Boolean                = false,

    // Negotiation (Phase 2)
    val negotiationSheet:   StudyMaterialDto?       = null,   // material whose negotiation sheet is open
    val negotiationHistory: NegotiationHistoryData? = null,
    val isLoadingNegotiation: Boolean               = false,
    val isRespondingNegotiation: Boolean            = false,

    // Seller wallet (Phase 3)
    val showWalletSheet: Boolean = false,
    val wallet: WalletData? = null,
    val isLoadingWallet: Boolean = false,

    // Social proof (Phase 4)
    val buyers: BuyersData? = null,
    val isLoadingBuyers: Boolean = false,

    // Phase 5: navigation event for "Chat with uploader"
    val pendingChatId: String? = null,
    val isOpeningChat: Boolean = false,
)

@HiltViewModel
class StudyMaterialsViewModel @Inject constructor(
    private val api:        StudyMaterialsApiService,
    private val chatApi:    com.example.bpscnotes.data.remote.api.MaterialChatApiService,
    private val authApi:    com.example.bpscnotes.data.remote.api.AuthApiService,
    private val tokenStore: com.example.bpscnotes.data.local.TokenStore,
    @ApplicationContext private val context: Context,
    val coinsConfig: com.example.bpscnotes.core.config.CoinsConfigRepository,
    private val gplayMaterial: com.example.bpscnotes.presentation.payment.GPlayMaterialPurchaseManager,
    private val bus: RefreshEventBus) : ViewModel() {

    private val _state = MutableStateFlow(StudyMaterialsUiState())
    val state: StateFlow<StudyMaterialsUiState> = _state.asStateFlow()

    companion object {
        private const val TAG = "StudyMaterialsVM"
        private const val SEEN_MARKETPLACE_RULES_KEY = "seen_marketplace_rules"
    }

    private var searchJob:  Job? = null
    private var uploadJob:  Job? = null  // tracked so user can cancel mid-upload

    init {
        // Load persisted downloaded IDs; purge any whose files no longer exist on disk
        val allIds = tokenStore.getDownloadedIds()
        val validPaths = allIds.mapNotNull { id ->
            val path = tokenStore.getLocalPath(id)
            if (path != null && java.io.File(path).exists()) id to path else null
        }.toMap()
        val staleIds = allIds - validPaths.keys
        staleIds.forEach { tokenStore.removeDownloadedId(it) }

        _state.update { it.copy(
            downloadedIds = validPaths.keys,
            purchasedIds  = tokenStore.getPurchasedIds(),
            localPaths    = validPaths,
            showRulesSheet = !tokenStore.getBoolPref(SEEN_MARKETPLACE_RULES_KEY, false),
        )}
        loadSubjects()
        loadStats()
        loadMaterials(reset = true)
        loadPinned()
        loadDownloadHistory()
        loadMyUploads()
        loadUserInfo()

        // ── Refresh on bus events ─────────────────────────────
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.CoinsChanged,
                    is RefreshEvent.LessonCompleted -> refresh()
                    else -> {}
                }
            }
        }
    }

    // ── Subjects ──────────────────────────────────────────────
    private fun loadSubjects() {
        viewModelScope.launch {
            try {
                val res = api.getSubjects()
                _state.update { it.copy(subjects = res.data?.subjects ?: listOf("All")) }
            } catch (e: Exception) { Log.w(TAG, "loadSubjects: ${e.message}") }
        }
    }

    // ── Stats ─────────────────────────────────────────────────
    private fun loadStats() {
        viewModelScope.launch {
            try {
                val res = api.getStats()
                _state.update { it.copy(stats = res.data) }
            } catch (e: Exception) { Log.w(TAG, "loadStats: ${e.message}") }
        }
    }

    // ── Material list ─────────────────────────────────────────
    // Fetch ALL pinned/featured materials from a dedicated endpoint —
    // not subject to the main list's 20-item pagination, so a pinned
    // item never silently disappears just because it ranks low on downloads.
    fun loadPinned() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPinned = true) }
            try {
                val res = api.getPinned()
                _state.update {
                    it.copy(
                        pinnedMaterials = res.data?.materials ?: emptyList(),
                        isLoadingPinned = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadPinned: ${e.message}", e)
                _state.update { it.copy(isLoadingPinned = false) }
            }
        }
    }

    fun loadMaterials(loadMore: Boolean = false, reset: Boolean = false){
        val s = _state.value
        if (loadMore && (!s.hasNextPage || s.isLoadingMore)) return

        viewModelScope.launch {
            val page = if (reset) 1 else s.currentPage + 1
            _state.update {
                it.copy(
                    isLoadingList = reset && it.materials.isEmpty(),
                    isLoadingMore = loadMore,
                    isRefreshing  = reset && it.materials.isNotEmpty(),
                    listError     = null
                )
            }
            try {
                val res = api.list(
                    type           = s.selectedType?.apiKey,
                    subject        = if (s.selectedSubject == "All") null else s.selectedSubject,
                    language       = if (s.selectedLanguage == "All") null else s.selectedLanguage,
                    search         = s.searchQuery.ifEmpty { null },
                    page           = page,
                    limit          = 20,
                    sort           = s.sortBy,
                    bookmarkedOnly = s.showBookmarksOnly
                )
                val data    = res.data ?: throw Exception("Empty response")
                val newList = if (reset) data.materials else s.materials + data.materials
                val bookmarked = newList.filter { it.isBookmarked }.map { it.id }.toSet()
                _state.update {
                    it.copy(
                        materials     = newList,
                        hasNextPage   = data.meta.hasNext,
                        currentPage   = page,
                        isLoadingList = false,
                        isRefreshing  = false,
                        isLoadingMore = false,
                        bookmarkedIds = if (reset) bookmarked else it.bookmarkedIds + bookmarked,
                        listError     = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMaterials: ${e.message}", e)
                _state.update {
                    it.copy(isLoadingList = false, isRefreshing = false, isLoadingMore = false,
                        listError = e.toUserMessage("Failed to load materials"))
                }
            }
        }
    }

    fun refresh()   { loadMaterials(reset = true); loadPinned() }
    fun loadMore()  = loadMaterials(loadMore = true)

    // ── Filters ───────────────────────────────────────────────
    fun selectType(type: MaterialType?)   { _state.update { it.copy(selectedType    = type)    }; loadMaterials(reset = true) }
    fun selectSubject(subject: String)    { _state.update { it.copy(selectedSubject = subject) }; loadMaterials(reset = true) }
    fun selectLanguage(language: String)  { _state.update { it.copy(selectedLanguage = language) }; loadMaterials(reset = true) }
    fun setSortBy(sort: String)           { _state.update { it.copy(sortBy          = sort)    }; loadMaterials(reset = true) }
    fun toggleBookmarksOnly()             { _state.update { it.copy(showBookmarksOnly = !it.showBookmarksOnly) }; loadMaterials(reset = true) }

    // FIX: FilterDialog used to call selectType/selectSubject/.../toggleBookmarksOnly
    // straight from each chip tap, so the list behind the dialog re-filtered on every
    // tap instead of waiting for "Apply". The dialog now holds its own draft selection
    // and commits it all here in one shot when "Apply" is pressed.
    fun applyFilters(
        type:          MaterialType?,
        subject:       String,
        language:      String,
        sort:          String,
        bookmarksOnly: Boolean
    ) {
        _state.update { it.copy(
            selectedType      = type,
            selectedSubject   = subject,
            selectedLanguage  = language,
            sortBy            = sort,
            showBookmarksOnly = bookmarksOnly,
        )}
        loadMaterials(reset = true)
    }

    // FIX Issue 6: Reset all active filters in one tap
    fun resetFilters() {
        _state.update { it.copy(
            selectedType      = null,
            selectedSubject   = "All",
            selectedLanguage  = "All",
            sortBy            = "downloads",
            showBookmarksOnly = false,
            searchQuery       = "",
        )}
        loadMaterials(reset = true)
    }

    fun setSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(400); loadMaterials(reset = true) }
    }

    // ── Detail sheet ──────────────────────────────────────────
    fun openDetail(materialId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingDetail = true, buyers = null) }
            try {
                val res = api.getMaterial(materialId)
                _state.update { it.copy(selectedMaterial = res.data, isLoadingDetail = false) }

                // Fetch anonymized buyer list for social proof — only if
                // someone has actually purchased this material.
                if ((res.data?.buyerCount ?: 0) > 0) {
                    _state.update { it.copy(isLoadingBuyers = true) }
                    try {
                        val buyersRes = api.getBuyers(materialId)
                        _state.update { it.copy(buyers = buyersRes.data, isLoadingBuyers = false) }
                    } catch (_: Exception) {
                        _state.update { it.copy(isLoadingBuyers = false) }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingDetail = false, toastMessage = e.toUserMessage("Failed to load")) }
            }
        }
    }

    fun closeDetail() = _state.update { it.copy(selectedMaterial = null, buyers = null) }

    /**
     * Records the material as accessed when the user opens/reads it —
     * POST /:id/download writes the material_downloads row the backend's
     * rate rule checks, and hasAccessed flips locally so the rating stars
     * appear immediately (QA: tapping stars before access threw a 400).
     */
    fun markOpened(materialId: String) {
        _state.update { st ->
            val sel = st.selectedMaterial
            if (sel != null && sel.id == materialId && !sel.hasAccessed)
                st.copy(selectedMaterial = sel.copy(hasAccessed = true))
            else st
        }
        viewModelScope.launch {
            runCatching { api.recordDownload(materialId) }
                .onFailure { android.util.Log.w("MaterialsVM", "markOpened: ${it.message}") }
        }
    }

    // ── Phase 5: Chat with uploader ─────────────────────────────
// Gets or creates the buyer's chat thread for this material, then
// signals the screen to navigate via pendingChatId.
    fun openChatWithUploader(materialId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isOpeningChat = true) }
            try {
                val res = chatApi.getOrCreateThread(materialId)
                val chatId = res.data?.id
                if (chatId != null) {
                    _state.update { it.copy(isOpeningChat = false, pendingChatId = chatId) }
                } else {
                    _state.update { it.copy(isOpeningChat = false, toastMessage = "Could not open chat") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isOpeningChat = false, toastMessage = e.toUserMessage("Could not open chat")) }
            }
        }
    }

    fun consumePendingChat() = _state.update { it.copy(pendingChatId = null) }

    // ── Bookmark ──────────────────────────────────────────────
    fun toggleBookmark(materialId: String) {
        val current = _state.value.bookmarkedIds.contains(materialId)
        _state.update {
            it.copy(
                bookmarkedIds    = if (current) it.bookmarkedIds - materialId else it.bookmarkedIds + materialId,
                selectedMaterial = if (it.selectedMaterial?.id == materialId)
                    it.selectedMaterial.copy(isBookmarked = !current) else it.selectedMaterial
            )
        }
        viewModelScope.launch {
            try {
                val res      = api.toggleBookmark(materialId)
                val serverVal = res.data?.bookmarked ?: !current
                if (serverVal != !current) {
                    _state.update { it.copy(bookmarkedIds = if (serverVal) it.bookmarkedIds + materialId else it.bookmarkedIds - materialId) }
                }
                _state.update { it.copy(toastMessage = if (serverVal) "🔖 Saved" else "Removed from saved") }
            } catch (e: Exception) {
                _state.update { it.copy(bookmarkedIds = if (current) it.bookmarkedIds + materialId else it.bookmarkedIds - materialId) }
                Log.e(TAG, "toggleBookmark: ${e.message}", e)
            }
        }
    }

    // ── Download to app-private storage ───────────────────────
// Files saved to context.filesDir/materials/ — NOT accessible to
// other apps, file managers, or gallery. Cannot be shared externally.
// User can only open the file through BPSCNotes in-app viewers.
    fun downloadMaterial(material: StudyMaterialDto) {
        if (_state.value.downloadedIds.contains(material.id)) {
            val localPath = tokenStore.getLocalPath(material.id)
            if (localPath != null && java.io.File(localPath).exists()) {
                _state.update { it.copy(toastMessage = "Already downloaded — open from the Downloads tab") }
                return
            }
            // File deleted — remove stale record and re-download
            tokenStore.removeDownloadedId(material.id)
            _state.update { it.copy(downloadedIds = it.downloadedIds - material.id) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(downloadingId = material.id) }
            try {
                val res = api.recordDownload(material.id)
                val url = res.data?.downloadUrl ?: throw Exception("No download URL")

                val ext = when {
                    // material.type == MaterialType.VIDEO -> "mp4"
                    //  url.contains(".mp4")  -> "mp4"
                    url.contains(".png")  -> "png"
                    url.contains(".jpg") || url.contains(".jpeg") -> "jpg"
                    else -> "pdf"
                }

                val dir       = java.io.File(context.filesDir, "materials").also { it.mkdirs() }
                val localFile = java.io.File(dir, "${material.id}.$ext")

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                    .build()
                val body = client.newCall(okhttp3.Request.Builder().url(url).build())
                    .execute().body ?: throw Exception("Empty response")

                body.byteStream().use { input ->
                    java.io.FileOutputStream(localFile).use { output ->
                        input.copyTo(output, bufferSize = 65536)
                    }
                }

                tokenStore.addDownloadedId(material.id)
                tokenStore.saveLocalPath(material.id, localFile.absolutePath)

                _state.update { it.copy(
                    downloadingId = null,
                    downloadedIds = it.downloadedIds + material.id,
                    localPaths    = it.localPaths + (material.id to localFile.absolutePath),
                    // Downloads live in Student Hub → Downloads tab, not My
                    // Learning (QA 09-Jul issue 17)
                    toastMessage  = "✅ Downloaded — open from the Downloads tab"
                )}
                loadDownloadHistory()

            } catch (e: Exception) {
                Log.e(TAG, "download: ${e.message}", e)
                _state.update { it.copy(downloadingId = null, toastMessage = "Download failed: ${e.message}") }
            }
        }
    }

    fun getLocalPath(materialId: String): String? {
        val path = _state.value.localPaths[materialId] ?: tokenStore.getLocalPath(materialId)
        return if (path != null && java.io.File(path).exists()) path else null
    }

    // ── Upload ────────────────────────────────────────────────
    fun showUpload() = _state.update { it.copy(showUploadSheet = true, uploadSuccess = null, uploadError = null) }

    // ── Marketplace rules popup ────────────────────────────────
// Opens the rules sheet on demand — used by the (i) info button
// so users can revisit the rules anytime, even after dismissing
// the first-time popup.
    fun showRules() = _state.update { it.copy(showRulesSheet = true) }

    // Dismiss the rules sheet and persist the "seen" flag so it never
// auto-shows again (the (i) button can still re-open it).
    fun dismissRules() {
        tokenStore.setBoolPref(SEEN_MARKETPLACE_RULES_KEY, true)
        _state.update { it.copy(showRulesSheet = false) }
    }

    fun hideUpload() {
        // If upload is in progress, show confirmation dialog instead of closing
        if (_state.value.isUploading) {
            _state.update { it.copy(showUploadCancelDialog = true) }
        } else {
            _state.update { it.copy(showUploadSheet = false) }
        }
    }

    fun dismissCancelDialog() = _state.update { it.copy(showUploadCancelDialog = false) }

    fun confirmCancelUpload() {
        uploadJob?.cancel()
        uploadJob = null
        _state.update { it.copy(
            showUploadCancelDialog = false,
            showUploadSheet        = false,
            isUploading            = false,
            uploadProgress         = 0f,
            uploadError            = null
        )}
    }

    fun updateUploadForm(
        title:       String?       = null,
        description: String?       = null,
        subject:     String?       = null,
        author:      String?       = null,
        tags:        String?       = null,
        type:        MaterialType? = null,
        isPremium:   Boolean?      = null,
        freePages:   String?       = null,
        price:       String?       = null,
        language:    String?       = null,
    ) {
        _state.update { s -> s.copy(
            uploadTitle       = title       ?: s.uploadTitle,
            uploadDescription = description ?: s.uploadDescription,
            uploadSubject     = subject     ?: s.uploadSubject,
            uploadAuthor      = author      ?: s.uploadAuthor,
            uploadTags        = tags        ?: s.uploadTags,
            uploadType        = type        ?: s.uploadType,
            uploadIsPremium   = isPremium   ?: s.uploadIsPremium,
            uploadFreePages   = freePages   ?: s.uploadFreePages,
            uploadPrice       = price       ?: s.uploadPrice,
            uploadLanguage    = language    ?: s.uploadLanguage,
        )}
    }

    fun resetUploadForm() {
        _state.update { it.copy(
            uploadTitle = "", uploadDescription = "", uploadSubject = "",
            uploadAuthor = "", uploadTags = "", uploadType = MaterialType.PDF,
            uploadIsPremium = false, uploadFreePages = "3", uploadPrice = "0",
            uploadError = null, uploadProgress = 0f
        )}
    }

    /**
     * FIX: Upload via direct multipart POST to our own server.
     *
     * Old flow (broken):
     *   1. GET /study-materials/upload-url  → S3 presigned URL
     *   2. PUT to S3 via OkHttp on main thread → NetworkOnMainThreadException
     *   3. POST /study-materials  → create record
     *
     * New flow (fixed):
     *   1. POST /study-materials/upload  (multipart) → server stores file + creates record
     *
     * No AWS account needed. Files stored on server disk.
     */
    fun uploadMaterial(
        uri: Uri,
        title: String,
        description: String,
        subject: String,
        type: MaterialType,
        author: String,
        tags: List<String>,
        pageCount: Int,
        isPremium: Boolean = false,
        freePages: Int = 3,
        price: Int = 0,
        language: String = "English"
    ) {
        uploadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.update { it.copy(isUploading = true, uploadProgress = 0.02f, uploadError = null) }

                val mimeType     = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val realFileName = getFileName(uri) ?: "upload.${ext(mimeType)}"

                // ── Compress / copy to temp file ──────────────
                _state.update { it.copy(uploadProgress = 0.05f) }
                val (uploadFile, uploadMime) = when {
                    mimeType.startsWith("video/") -> compressVideo(uri, realFileName)
                    else -> copyToTemp(uri, realFileName) to mimeType
                }

                _state.update { it.copy(uploadProgress = 0.2f) }

                // ── Streaming RequestBody with progress ───────
                val fileLength = uploadFile.length()
                val progressBody = object : okhttp3.RequestBody() {
                    override fun contentType() = uploadMime.toMediaTypeOrNull()
                    // CRITICAL: return -1 so OkHttp does NOT buffer the whole body before sending.
                    // Returning the real length causes OkHttp to load everything into RAM first,
                    // which is why the progress bar stays at 0 until the upload finishes.
                    override fun contentLength() = -1L
                    override fun writeTo(sink: okio.BufferedSink) {
                        val buf = ByteArray(65536) // 64KB chunks
                        var uploaded = 0L
                        uploadFile.inputStream().use { fis ->
                            var read: Int
                            while (fis.read(buf).also { read = it } != -1) {
                                sink.write(buf, 0, read)
                                sink.flush() // push each chunk immediately — don't wait for full buffer
                                uploaded += read
                                val p = 0.2f + (uploaded.toFloat() / fileLength) * 0.75f
                                _state.update { it.copy(uploadProgress = p.coerceIn(0.2f, 0.95f)) }
                            }
                        }
                    }
                }

                val filePart = MultipartBody.Part.createFormData("file", uploadFile.name, progressBody)

                val response = api.uploadMaterial(
                    file         = filePart,
                    title        = title.toRequestBody("text/plain".toMediaTypeOrNull()),
                    description  = description.toRequestBody("text/plain".toMediaTypeOrNull()),
                    subject      = subject.toRequestBody("text/plain".toMediaTypeOrNull()),
                    materialType = type.apiKey.toRequestBody("text/plain".toMediaTypeOrNull()),
                    author       = author.toRequestBody("text/plain".toMediaTypeOrNull()),
                    tags         = Gson().toJson(tags).toRequestBody("text/plain".toMediaTypeOrNull()),
                    pageCount    = pageCount.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    isPremium    = isPremium.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    freePages    = freePages.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    price        = price.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    language     = language.toRequestBody("text/plain".toMediaTypeOrNull())
                )

                uploadFile.delete() // cleanup temp

                _state.update {
                    it.copy(
                        isUploading     = false,
                        uploadProgress  = 1f,
                        showUploadSheet = false,
                        uploadSuccess   = response.message ?: "Upload successful",
                        toastMessage    = response.message ?: "Upload successful",
                        // Reset form for next upload
                        uploadTitle = "", uploadDescription = "", uploadSubject = "",
                        uploadAuthor = "", uploadTags = "", uploadType = MaterialType.PDF,
                        uploadIsPremium = false, uploadFreePages = "3", uploadPrice = "0",
                        uploadLanguage = "English",
                    )
                }
                refresh()

            } catch (e: Exception) {
                Log.e(TAG, "uploadMaterial: ${e.message}", e)
                _state.update { it.copy(isUploading = false, uploadError = e.toUserMessage("Upload failed")) }
            }
        }
    }

    // ── Copy URI → temp file (streaming, no full RAM load) ────
    private fun copyToTemp(uri: Uri, fileName: String): File {
        val safe = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_").take(60)
        val tmp  = File(context.cacheDir, "up_${System.currentTimeMillis()}_$safe")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { out -> input.copyTo(out, bufferSize = 65536) }
        }
        return tmp
    }

    // ── Video transcoding using MediaCodec ────────────────────
// Re-encodes video at lower bitrate — typically reduces 21MB → 3-5MB for a 6s clip.
// Falls back to simple remux if transcoding fails.
    private fun compressVideo(uri: Uri, originalName: String): Pair<File, String> {
        val inputFile  = copyToTemp(uri, originalName)
        val outputFile = File(context.cacheDir, "comp_${System.currentTimeMillis()}.mp4")

        // Skip compression for already-small files
        if (inputFile.length() < 2 * 1024 * 1024) {
            outputFile.delete()
            return inputFile to "video/mp4"
        }

        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val srcW = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
            val srcH = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720
            val rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            retriever.release()

            // Target max 720p — keeps quality reasonable, massively reduces size
            val scale = minOf(1f, minOf(1280f / srcW, 720f / srcH))
            // Keep dimensions divisible by 2 (codec requirement)
            val outW = ((srcW * scale).toInt() / 2) * 2
            val outH = ((srcH * scale).toInt() / 2) * 2

            // Target bitrate: 1.5 Mbps for 720p, 800 Kbps for smaller
            val targetBitrate = if (outW >= 1280 || outH >= 720) 1_500_000 else 800_000

            val extractor = android.media.MediaExtractor()
            extractor.setDataSource(context, uri, null)

            // Find video and audio track indices
            var videoTrackIdx = -1
            var audioTrackIdx = -1
            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i).getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && videoTrackIdx == -1) videoTrackIdx = i
                if (mime.startsWith("audio/") && audioTrackIdx == -1) audioTrackIdx = i
            }

            if (videoTrackIdx == -1) {
                extractor.release()
                return inputFile to "video/mp4"
            }

            // ── Set up video encoder ──────────────────────────
            val encFormat = android.media.MediaFormat.createVideoFormat("video/avc", outW, outH).apply {
                setInteger(android.media.MediaFormat.KEY_BIT_RATE, targetBitrate)
                setInteger(android.media.MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                setInteger(android.media.MediaFormat.KEY_COLOR_FORMAT,
                    android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
            val encoder = android.media.MediaCodec.createEncoderByType("video/avc")
            encoder.configure(encFormat, null, null, android.media.MediaCodec.CONFIGURE_FLAG_ENCODE)
            val encSurface = encoder.createInputSurface()
            encoder.start()

            // ── Set up video decoder ──────────────────────────
            val decFormat = extractor.getTrackFormat(videoTrackIdx)
            val decoder = android.media.MediaCodec.createDecoderByType(
                decFormat.getString(android.media.MediaFormat.KEY_MIME) ?: "video/avc"
            )
            decoder.configure(decFormat, encSurface, null, 0)
            decoder.start()

            val muxer = android.media.MediaMuxer(outputFile.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var muxerStarted = false

            // Copy audio track directly (no re-encoding — audio is small)
            val audioSamples = mutableListOf<Triple<java.nio.ByteBuffer, android.media.MediaCodec.BufferInfo, Int>>()
            if (audioTrackIdx != -1) {
                extractor.selectTrack(audioTrackIdx)
                val audioBuf = java.nio.ByteBuffer.allocate(512 * 1024)
                val audioInfo = android.media.MediaCodec.BufferInfo()
                while (true) {
                    audioInfo.size = extractor.readSampleData(audioBuf, 0)
                    if (audioInfo.size < 0) break
                    audioInfo.presentationTimeUs = extractor.sampleTime
                    audioInfo.flags = extractor.sampleFlags
                    val copy = java.nio.ByteBuffer.allocate(audioInfo.size)
                    audioBuf.rewind(); audioBuf.limit(audioInfo.size)
                    copy.put(audioBuf); copy.rewind()
                    audioSamples.add(Triple(copy, android.media.MediaCodec.BufferInfo().also {
                        it.offset = 0; it.size = audioInfo.size
                        it.presentationTimeUs = audioInfo.presentationTimeUs
                        it.flags = audioInfo.flags
                    }, audioTrackIdx))
                    extractor.advance()
                }
                extractor.unselectTrack(audioTrackIdx)
            }

            // Transcode video
            extractor.selectTrack(videoTrackIdx)
            extractor.seekTo(0, android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val decBufInfo = android.media.MediaCodec.BufferInfo()
            val encBufInfo = android.media.MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            val TIMEOUT = 10_000L // 10ms

            while (!outputDone) {
                // Feed decoder
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(TIMEOUT)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, extractor.sampleFlags)
                            extractor.advance()
                        }
                    }
                }

                // Drain decoder → encoder surface
                val decOutIdx = decoder.dequeueOutputBuffer(decBufInfo, TIMEOUT)
                if (decOutIdx >= 0) {
                    val render = decBufInfo.size > 0
                    decoder.releaseOutputBuffer(decOutIdx, render)
                    if (decBufInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        encoder.signalEndOfInputStream()
                    }
                }

                // Drain encoder → muxer
                val encOutIdx = encoder.dequeueOutputBuffer(encBufInfo, TIMEOUT)
                when {
                    encOutIdx == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        muxerVideoTrack = muxer.addTrack(encoder.outputFormat)
                        if (audioTrackIdx != -1 && audioSamples.isNotEmpty()) {
                            val audioFmt = extractor.getTrackFormat(audioTrackIdx)
                            muxerAudioTrack = muxer.addTrack(audioFmt)
                        }
                        muxer.start(); muxerStarted = true

                        // Write audio now that muxer is started
                        audioSamples.forEach { (buf, info, _) ->
                            if (muxerAudioTrack >= 0) muxer.writeSampleData(muxerAudioTrack, buf, info)
                        }
                    }
                    encOutIdx >= 0 -> {
                        if (muxerStarted && encBufInfo.size > 0 &&
                            encBufInfo.flags and android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            muxer.writeSampleData(muxerVideoTrack, encoder.getOutputBuffer(encOutIdx)!!, encBufInfo)
                        }
                        encoder.releaseOutputBuffer(encOutIdx, false)
                        if (encBufInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            decoder.stop(); decoder.release()
            encoder.stop(); encoder.release()
            encSurface.release()
            extractor.release()
            if (muxerStarted) { muxer.stop() }
            muxer.release()

            val origKb = inputFile.length() / 1024
            val compKb = outputFile.length() / 1024
            Log.i(TAG, "Video transcoded: ${origKb}KB → ${compKb}KB (${100 - compKb * 100 / origKb}% smaller) ${outW}x${outH} @ ${targetBitrate/1000}kbps")
            inputFile.delete()
            outputFile to "video/mp4"

        } catch (e: Exception) {
            Log.w(TAG, "Video transcoding failed, falling back to remux: ${e.message}")
            outputFile.delete()
            // Fallback: simple remux without re-encoding
            remuxVideo(uri, inputFile)
        }
    }

    // ── Fallback: remux only (no quality change, but fixes container issues) ─
    private fun remuxVideo(uri: Uri, inputFile: File): Pair<File, String> {
        val outputFile = File(context.cacheDir, "remux_${System.currentTimeMillis()}.mp4")
        return try {
            val extractor = android.media.MediaExtractor()
            extractor.setDataSource(context, uri, null)
            val muxer = android.media.MediaMuxer(outputFile.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = mutableMapOf<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i); trackMap[i] = muxer.addTrack(fmt)
                }
            }
            muxer.start()
            val buf = java.nio.ByteBuffer.allocate(1024 * 1024)
            val info = android.media.MediaCodec.BufferInfo()
            while (true) {
                info.size = extractor.readSampleData(buf, 0)
                if (info.size < 0) break
                info.presentationTimeUs = extractor.sampleTime; info.flags = extractor.sampleFlags
                trackMap[extractor.sampleTrackIndex]?.let { muxer.writeSampleData(it, buf, info) }
                extractor.advance()
            }
            muxer.stop(); muxer.release(); extractor.release()
            inputFile.delete()
            outputFile to "video/mp4"
        } catch (e: Exception) {
            Log.w(TAG, "Remux also failed: ${e.message}")
            outputFile.delete()
            inputFile to "video/mp4"
        }
    }

    private fun ext(mimeType: String) = when (mimeType) {
        "application/pdf"  -> "pdf"
        "video/mp4"        -> "mp4"
        "video/quicktime"  -> "mov"
        "image/jpeg"       -> "jpg"
        "image/png"        -> "png"
        else               -> "bin"
    }



    fun clearToast() = _state.update { it.copy(toastMessage = null) }

    // ── Helpers ───────────────────────────────────────────────
    private fun getFileName(uri: android.net.Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(idx)
            }
        } catch (e: Exception) { null }
    }
    // ── My Uploads tab ────────────────────────────────────────
    fun loadMyUploads() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMyUploads = true) }
            try {
                val res = api.myUploads()
                _state.update { it.copy(
                    myUploads          = res.data?.uploads ?: emptyList(),
                    currentUserId      = tokenStore.getUserId() ?: "",
                    isLoadingMyUploads = false
                )}
            } catch (e: Exception) {
                Log.e(TAG, "loadMyUploads: \${e.message}")
                _state.update { it.copy(isLoadingMyUploads = false) }
            }
        }
    }

    // ── Load user coin balance ───────────────────────────────────
    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val user = authApi.getMe().data?.user
                _state.update { it.copy(
                    userCoins = user?.coins ?: 0,
                )}
            } catch (_: Exception) {}
        }
    }

    // ── Negotiation (Phase 2) ──────────────────────────────────
// Opens the negotiation sheet for an uploaded material and loads
// its full offer history. Called when tapping the "Respond" banner
// on a "negotiating" item in My Uploads.
    fun openNegotiation(material: StudyMaterialDto) {
        _state.update { it.copy(negotiationSheet = material, negotiationHistory = null, isLoadingNegotiation = true) }
        viewModelScope.launch {
            try {
                val res = api.getNegotiationHistory(material.id)
                _state.update { it.copy(negotiationHistory = res.data, isLoadingNegotiation = false) }
            } catch (e: Exception) {
                Log.e(TAG, "openNegotiation: ${e.message}")
                _state.update { it.copy(
                    isLoadingNegotiation = false,
                    toastMessage = "Could not load negotiation details"
                )}
            }
        }
    }

    fun closeNegotiation() = _state.update { it.copy(negotiationSheet = null, negotiationHistory = null) }

    // Accept the admin's current counter-offer — material goes live
// immediately at that price.
    fun acceptNegotiationOffer() {
        val materialId = _state.value.negotiationSheet?.id ?: return
        _state.update { it.copy(isRespondingNegotiation = true) }
        viewModelScope.launch {
            try {
                val res = api.acceptNegotiation(materialId)
                _state.update { it.copy(
                    isRespondingNegotiation = false,
                    negotiationSheet = null,
                    negotiationHistory = null,
                    toastMessage = res.message ?: "Accepted! Your material is now live 🎉"
                )}
                loadMyUploads()
            } catch (e: Exception) {
                Log.e(TAG, "acceptNegotiationOffer: ${e.message}")
                _state.update { it.copy(
                    isRespondingNegotiation = false,
                    toastMessage = e.toUserMessage("Failed to accept offer")
                )}
            }
        }
    }

    // Send a counter-offer back to admin — increments the round.
    fun counterNegotiationOffer(price: Double, message: String?) {
        val materialId = _state.value.negotiationSheet?.id ?: return
        if (price < 0) {
            _state.update { it.copy(toastMessage = "Enter a valid price") }
            return
        }
        _state.update { it.copy(isRespondingNegotiation = true) }
        viewModelScope.launch {
            try {
                val res = api.counterNegotiation(materialId, CounterOfferRequest(price, message))
                _state.update { it.copy(
                    isRespondingNegotiation = false,
                    negotiationSheet = null,
                    negotiationHistory = null,
                    toastMessage = res.message ?: "Counter-offer sent"
                )}
                loadMyUploads()
            } catch (e: Exception) {
                Log.e(TAG, "counterNegotiationOffer: ${e.message}")
                _state.update { it.copy(
                    isRespondingNegotiation = false,
                    toastMessage = e.toUserMessage("Failed to send counter-offer")
                )}
            }
        }
    }

    // ── Seller wallet (Phase 3) ────────────────────────────────
    fun openWallet() {
        _state.update { it.copy(showWalletSheet = true) }
        loadWallet()
    }

    fun closeWallet() = _state.update { it.copy(showWalletSheet = false) }

    fun loadWallet() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingWallet = true) }
            try {
                val res = api.getWallet()
                _state.update { it.copy(wallet = res.data, isLoadingWallet = false) }
            } catch (e: Exception) {
                Log.e(TAG, "loadWallet: ${e.message}")
                _state.update { it.copy(isLoadingWallet = false, toastMessage = "Could not load wallet") }
            }
        }
    }


    // ── Downloads history ─────────────────────────────────────
    fun loadDownloadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingHistory = true) }
            try {
                val res = api.getMyDownloads()
                _state.update { it.copy(
                    downloadHistory  = res.data?.downloads ?: emptyList(),
                    isLoadingHistory = false
                )}
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingHistory = false) }
                Log.e(TAG, "loadDownloadHistory: ${e.message}")
            }
        }
    }

    // ── Purchase locked material (Phase 3: hybrid coins + Cashfree) ──
// coinsToApply: how many coins the user chose to apply as a discount
// (capped server-side by max_coins_per_purchase).
    fun purchaseMaterial(materialId: String, price: Double, title: String, coinsToApply: Int = 0) {
        viewModelScope.launch {
            _state.update { it.copy(purchasingId = materialId, purchaseError = null) }
            try {
                val res = api.initPurchase(materialId, InitPurchaseRequest(coinsToApply))
                val data = res.data

                when {
                    data?.alreadyPurchased == true -> {
                        _state.update { it.copy(
                            purchasingId    = null,
                            purchaseSuccess = "You already own \"$title\"",
                            purchasedIds    = it.purchasedIds + materialId
                        )}
                    }
                    // Free or fully covered by coins — already completed by initPurchase
                    data?.requiresPayment != true -> {
                        tokenStore.addPurchasedId(materialId)
                        _state.update { it.copy(
                            purchasingId    = null,
                            purchaseSuccess = "🎉 Unlocked! \"$title\" — full PDF now available",
                            purchasedIds    = it.purchasedIds + materialId
                        )}
                        loadStats()
                    }
                    // Remaining ₹ balance needs Cashfree — hand off to UI to launch SDK
                    else -> {
                        _state.update { it.copy(
                            purchasingId = null,
                            pendingPurchase = data,
                            pendingPurchaseMaterialId = materialId,
                            pendingPurchaseTitle = title,
                            pendingPurchaseOrderId = data.purchaseOrderId,
                        )}
                    }
                }
            } catch (e: Exception) {
                val msg = e.toUserMessage("Purchase failed")
                _state.update { it.copy(
                    purchasingId  = null,
                    purchaseError = when {
                        msg.contains("insufficient", true) || msg.contains("coins", true) ->
                            "Not enough coins. Watch an ad or complete daily tasks to earn more."
                        else -> msg
                    }
                )}
            }
        }
    }

    // Called by the screen once Cashfree SDK has launched (prevents
// re-launching on recomposition).
    fun consumePendingPurchase() {
        _state.update { it.copy(pendingPurchase = null) }
    }

    // User choice billing: token from Google's billing-choice screen when
    // the user picks Cashfree there (release builds). Rides along on the
    // confirm call so the backend reports the payment to the Play
    // Developer API. Null for every other purchase path.
    private var pendingExternalTxToken: String? = null

    // Called by the screen's Cashfree onSuccess callback.
    fun confirmMaterialPurchase(cfPaymentId: String) {
        val materialId      = _state.value.pendingPurchaseMaterialId ?: return
        val purchaseOrderId = _state.value.pendingPurchaseOrderId    ?: return
        val title           = _state.value.pendingPurchaseTitle      ?: ""
        viewModelScope.launch {
            _state.update { it.copy(isConfirmingPurchase = true) }
            try {
                val res = api.confirmPurchase(materialId, ConfirmPurchaseRequest(
                    purchaseOrderId          = purchaseOrderId,
                    cfPaymentId              = cfPaymentId,
                    externalTransactionToken = pendingExternalTxToken,
                ))
                pendingExternalTxToken = null
                tokenStore.addPurchasedId(materialId)
                _state.update { it.copy(
                    isConfirmingPurchase      = false,
                    purchaseSuccess           = res.message ?: "🎉 Unlocked! \"$title\" — full PDF now available",
                    purchasedIds              = it.purchasedIds + materialId,
                    // Update the open detail sheet in-place so the button flips to "Open PDF" immediately
                    selectedMaterial          = if (it.selectedMaterial?.id == materialId)
                        it.selectedMaterial.copy(isPurchased = true) else it.selectedMaterial,
                    pendingPurchaseMaterialId = null,
                    pendingPurchaseTitle      = null,
                    pendingPurchaseOrderId    = null,
                )}
                loadStats()
            } catch (e: Exception) {
                _state.update { it.copy(
                    isConfirmingPurchase   = false,
                    purchaseError          = "Payment received but unlock failed. Contact support — payment ID: $cfPaymentId",
                    pendingPurchaseOrderId = null,
                )}
            }
        }
    }

    // Called by the screen's Cashfree onFailure callback.
    fun handleMaterialPaymentFailure(code: Int, message: String) {
        pendingExternalTxToken = null
        _state.update { it.copy(
            purchaseError             = "Payment failed: $message",
            pendingPurchaseMaterialId = null,
            pendingPurchaseTitle      = null,
            pendingPurchaseOrderId    = null,
        )}
    }

    fun clearPurchaseMessages() {
        _state.update { it.copy(purchaseSuccess = null, purchaseError = null) }
    }

    // ── Release-build purchase path: Google Play Billing ─────────
    // Play policy requires digital-content purchases to go through Play
    // Billing — this is the release-build counterpart to purchaseMaterial()
    // above, which stays Cashfree/coins-only and is only ever called from
    // debug builds now (see the BuildConfig.DEBUG branch at the call sites
    // in StudyMaterialsScreen.kt / PdfViewerScreen.kt). No coin discount
    // here — Play always charges the material's full synced price.
    fun startGPlayMaterialPurchase(activity: android.app.Activity, materialId: String, title: String) {
        if (_state.value.purchasedIds.contains(materialId)) return
        viewModelScope.launch {
            _state.update { it.copy(purchasingId = materialId, purchaseError = null) }
            try {
                val details = gplayMaterial.queryProductDetails(materialId)
                if (details == null) {
                    _state.update { it.copy(
                        purchasingId  = null,
                        purchaseError = "This material isn't available for purchase yet. Please try again shortly."
                    )}
                    return@launch
                }
                val userId = tokenStore.getUserId() ?: ""
                // Subscribe BEFORE launching the flow: the shared billing
                // flows have replay=0, so an emission landing before the
                // collector attaches would be dropped and the await would
                // hang. yield() lets the async collector attach first.
                val outcomeDeferred = async { gplayMaterial.awaitPurchaseOrUserChoice(materialId) }
                yield()
                val result = gplayMaterial.launchPurchase(activity, details, obfuscatedAccountId = userId)
                if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                    outcomeDeferred.cancel()
                    _state.update { it.copy(
                        purchasingId  = null,
                        purchaseError = "Could not open Google Play checkout (${result.debugMessage})"
                    )}
                    return@launch
                }
                when (val outcome = outcomeDeferred.await()) {
                    is GPlayPurchaseOutcome.PlayPurchase -> {
                        val res = api.verifyGplayMaterialPurchase(materialId, VerifyGplayMaterialRequest(outcome.purchase.purchaseToken))
                        tokenStore.addPurchasedId(materialId)
                        _state.update { it.copy(
                            purchasingId     = null,
                            purchaseSuccess  = res.message ?: "🎉 Unlocked! \"$title\" — full PDF now available",
                            purchasedIds     = it.purchasedIds + materialId,
                            selectedMaterial = if (it.selectedMaterial?.id == materialId)
                                it.selectedMaterial.copy(isPurchased = true) else it.selectedMaterial,
                        )}
                        loadStats()
                    }

                    is GPlayPurchaseOutcome.AlternativeBillingChosen -> {
                        // User picked Cashfree on Google's billing-choice
                        // screen. Release builds skip initPurchase up front,
                        // so create the Cashfree order now, then hand off to
                        // the screen's Cashfree launcher via pendingPurchase —
                        // exactly the debug-path state shape. No coin
                        // discount, matching the Play path this replaces.
                        pendingExternalTxToken = outcome.externalTransactionToken
                        val res = api.initPurchase(materialId, InitPurchaseRequest(0))
                        val data = res.data
                        when {
                            // A null payload is a failed order-create, not a
                            // free unlock — without this guard the
                            // requiresPayment != true arm below would unlock
                            // the paid material for free.
                            data == null -> {
                                pendingExternalTxToken = null
                                _state.update { it.copy(
                                    purchasingId  = null,
                                    purchaseError = "Could not start payment. Please try again."
                                )}
                            }
                            data.alreadyPurchased == true -> {
                                pendingExternalTxToken = null
                                _state.update { it.copy(
                                    purchasingId    = null,
                                    purchaseSuccess = "You already own \"$title\"",
                                    purchasedIds    = it.purchasedIds + materialId
                                )}
                            }
                            data?.requiresPayment != true -> {
                                pendingExternalTxToken = null
                                tokenStore.addPurchasedId(materialId)
                                _state.update { it.copy(
                                    purchasingId    = null,
                                    purchaseSuccess = "🎉 Unlocked! \"$title\" — full PDF now available",
                                    purchasedIds    = it.purchasedIds + materialId
                                )}
                                loadStats()
                            }
                            else -> {
                                _state.update { it.copy(
                                    purchasingId              = null,
                                    pendingPurchase           = data,
                                    pendingPurchaseMaterialId = materialId,
                                    pendingPurchaseTitle      = title,
                                    pendingPurchaseOrderId    = data.purchaseOrderId,
                                )}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(purchasingId = null, purchaseError = e.toUserMessage("Purchase failed")) }
            }
        }
    }

// ── Rating ──────────────────────────────────────────────────
    /** Load the current user's existing rating for the open material. */
    fun loadMyRating(materialId: String) {
        viewModelScope.launch {
            try {
                val res = api.getMyRating(materialId)
                _state.update { it.copy(
                    myRatingStars  = res.data?.stars  ?: 0,
                    myRatingReview = res.data?.review,
                )}
            } catch (_: Exception) {}
        }
    }

    /** Submit or update a 1-5 star rating with an optional text review. */
    fun submitRating(materialId: String, stars: Int, review: String? = null) {
        if (stars < 1 || stars > 5) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingRating = true, ratingError = null) }
            try {
                val res = api.rateMaterial(
                    materialId,
                    com.example.bpscnotes.data.remote.api.RateMaterialRequest(stars, review?.trim()?.ifBlank { null })
                )
                val newAvg = res.data?.avgRating
                _state.update { it.copy(
                    isSubmittingRating = false,
                    myRatingStars      = stars,
                    myRatingReview     = review?.trim()?.ifBlank { null },
                    ratingSuccess      = "Thanks for your rating!",
                    ratingError        = null,
                    // Patch the open detail sheet immediately so the avg rating
                    // doesn't look stale until the sheet is closed and reopened.
                    selectedMaterial   = if (it.selectedMaterial?.id == materialId && newAvg != null)
                        it.selectedMaterial.copy(rating = newAvg) else it.selectedMaterial,
                )}
                // Refresh list so the updated avg rating reflects immediately there too
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(
                    isSubmittingRating = false,
                    ratingError        = e.message ?: "Rating failed",
                )}
            }
        }
    }

    fun clearRatingMessages() {
        _state.update { it.copy(ratingSuccess = null, ratingError = null) }
    }

    /** Called by UI after user grants WRITE_EXTERNAL_STORAGE permission */
    fun retryPendingDownload() {
        val material = _state.value.pendingDownloadMaterial ?: return
        _state.update { it.copy(needsStoragePermission = false, pendingDownloadMaterial = null) }
        downloadMaterial(material)
    }

    /** Called when user denies the permission */
    fun onStoragePermissionDenied() {
        _state.update { it.copy(
            needsStoragePermission  = false,
            pendingDownloadMaterial = null,
            toastMessage = "Storage permission denied. Grant it in Settings to download files."
        )}
    }

}