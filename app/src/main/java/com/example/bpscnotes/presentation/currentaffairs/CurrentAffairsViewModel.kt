package com.example.bpscnotes.presentation.currentaffairs

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.bpscnotes.core.workers.CaMcqSubmitWorker
import com.example.bpscnotes.data.remote.api.CurrentAffairsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.core.network.toUserMessage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class CurrentAffairsUiState(
    val allArticles: List<CAArticle> = emptyList(),   // full unfiltered list — for category chips
    val articles: List<CAArticle> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class CaArticleDetailState(
    val article: CAArticle? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CurrentAffairsViewModel @Inject constructor(
    private val api: CurrentAffairsApiService,
    private val bus: RefreshEventBus,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrentAffairsUiState())
    val uiState: StateFlow<CurrentAffairsUiState> = _uiState.asStateFlow()

    // Local bookmark state — toggled optimistically and synced with API
    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    // Article detail screen state — fetched by id since the list payload
    // doesn't include full_content (kept light for the listing screen).
    private val _articleDetailState = MutableStateFlow(CaArticleDetailState())
    val articleDetailState: StateFlow<CaArticleDetailState> = _articleDetailState.asStateFlow()

    // PDF download — separate from articleDetailState since it's a
    // one-off action rather than screen-load state.
    private val _isDownloadingPdf = MutableStateFlow(false)
    val isDownloadingPdf: StateFlow<Boolean> = _isDownloadingPdf.asStateFlow()
    private val _pdfError = MutableStateFlow<String?>(null)
    val pdfError: StateFlow<String?> = _pdfError.asStateFlow()
    fun clearPdfError() { _pdfError.update { null } }

    init {
        loadArticles()
        loadMcqMarkingConfig()

        // ── Refresh on bus events ─────────────────────────────
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.CoinsChanged -> refresh()
                    is RefreshEvent.CaBookmarkChanged -> applyBookmarkState(event.affairId, event.isBookmarked)
                    else -> {}
                }
            }
        }
    }

    fun loadArticles(category: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.articles.isEmpty(), error = null) }
            try {
                // Always fetch ALL articles — no server-side category filter
                // Categories are filtered locally so chips never disappear
                val response  = api.getAffairs(limit = 60, category = null)
                val serverList = response.data?.affairs ?: emptyList()

                val serverBookmarked = serverList.filter { it.isBookmarked }.map { it.id }.toSet()
                // Merge: keep any locally-toggled bookmarks even if cache returns stale data
                val mergedBookmarked = serverBookmarked + _bookmarkedIds.value
                _bookmarkedIds.value = mergedBookmarked

                val articles = serverList.map { dto ->
                    dto.toUiModel(isBookmarked = mergedBookmarked.contains(dto.id))
                }

                _uiState.update { it.copy(allArticles = articles, articles = articles, isLoading = false) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = e.toUserMessage("Failed to load current affairs")
                    )
                }
            }
        }
    }

    /**
     * Optimistic bookmark toggle:
     * 1. Update local state immediately (instant UI feedback)
     * 2. Call API in background
     * 3. Revert if API fails
     */
    fun toggleBookmark(id: String) {
        val wasBookmarked = _bookmarkedIds.value.contains(id)

        // Optimistic update
        _bookmarkedIds.update { current ->
            if (wasBookmarked) current - id else current + id
        }
        // Also update the article list so the card redraws correctly
        _uiState.update { state ->
            state.copy(
                articles = state.articles.map { article ->
                    if (article.id == id) article.copy(isBookmarked = !wasBookmarked) else article
                }
            )
        }

        // Background API call
        viewModelScope.launch {
            try {
                api.toggleBookmark(id)
                // Let any other live CurrentAffairsViewModel instance (list vs.
                // detail screen each have their own) know about this change.
                bus.emit(RefreshEvent.CaBookmarkChanged(id, !wasBookmarked))
            } catch (e: Exception) {
                // Revert on failure
                _bookmarkedIds.update { current ->
                    if (wasBookmarked) current + id else current - id
                }
                _uiState.update { state ->
                    state.copy(
                        articles = state.articles.map { article ->
                            if (article.id == id) article.copy(isBookmarked = wasBookmarked) else article
                        }
                    )
                }
            }
        }
    }

    /** Applies a bookmark state change that originated from another screen's
     *  ViewModel instance (via the bus) — no API call, just local state sync. */
    private fun applyBookmarkState(id: String, isBookmarked: Boolean) {
        _bookmarkedIds.update { current -> if (isBookmarked) current + id else current - id }
        _uiState.update { state ->
            state.copy(articles = state.articles.map { a -> if (a.id == id) a.copy(isBookmarked = isBookmarked) else a })
        }
        _articleDetailState.update { state ->
            val a = state.article
            if (a != null && a.id == id) state.copy(article = a.copy(isBookmarked = isBookmarked)) else state
        }
    }

    fun refresh() = loadArticles()

    fun clearError() { _uiState.update { it.copy(error = null) } }

    // ── Article detail (full-screen page) ──────────────────────────
    fun loadArticleDetail(affairId: String) {
        viewModelScope.launch {
            _articleDetailState.update { it.copy(isLoading = it.article == null, error = null) }
            try {
                val dto = api.getAffairDetail(affairId).data?.affair
                    ?: throw IllegalStateException("Article not found")
                // Bias toward "bookmarked" so we never clobber a local optimistic
                // toggle made on another screen with stale server data — same
                // merge rule loadArticles() uses.
                if (dto.isBookmarked) _bookmarkedIds.update { it + dto.id }
                _articleDetailState.update {
                    it.copy(article = dto.toUiModel(isBookmarked = _bookmarkedIds.value.contains(dto.id)), isLoading = false)
                }
            } catch (e: Exception) {
                _articleDetailState.update {
                    it.copy(isLoading = false, error = e.toUserMessage("Failed to load article"))
                }
            }
        }
    }

    // ── MCQs ─────────────────────────────────────────────────────
    private val _mcqs = MutableStateFlow<List<com.example.bpscnotes.data.remote.api.CaMcqDto>>(emptyList())
    val mcqs: StateFlow<List<com.example.bpscnotes.data.remote.api.CaMcqDto>> = _mcqs.asStateFlow()

    private val _mcqLoading = MutableStateFlow(false)
    val mcqLoading: StateFlow<Boolean> = _mcqLoading.asStateFlow()

    private val _mcqError = MutableStateFlow<String?>(null)
    val mcqError: StateFlow<String?> = _mcqError.asStateFlow()

    private val _mcqAnswers = MutableStateFlow<Map<String, CaMcqAnswerDto>>(emptyMap())
    val mcqAnswers: StateFlow<Map<String, CaMcqAnswerDto>> = _mcqAnswers.asStateFlow()

    // Global negative marking config — admin-set once, applies to every
    // CA / Practice MCQ session (these are lightweight practice questions
    // attached to an article, not a per-test record like `quizzes`).
    private val _mcqMarkingConfig = MutableStateFlow(com.example.bpscnotes.data.remote.api.CaMcqMarkingConfigDto())
    val mcqMarkingConfig: StateFlow<com.example.bpscnotes.data.remote.api.CaMcqMarkingConfigDto> = _mcqMarkingConfig.asStateFlow()

    fun loadMcqMarkingConfig() {
        viewModelScope.launch {
            try {
                val data = api.getMcqMarkingConfig().data
                if (data != null) _mcqMarkingConfig.value = data.config
            } catch (_: Exception) {
                // Non-fatal — practice continues without negative marking if this fails
            }
        }
    }

    fun loadMcqs(affairId: String) {
        viewModelScope.launch {
            _mcqLoading.value = true
            _mcqError.value   = null
            _mcqs.value       = emptyList()
            _mcqAnswers.value = emptyMap()
            try {
                val data = api.getMcqs(affairId).data
                _mcqs.value = data?.mcqs ?: emptyList()
            } catch (e: Exception) {
                _mcqError.value = e.toUserMessage("Failed to load questions")
                _mcqs.value = emptyList()
            }
            _mcqLoading.value = false
        }
        loadMcqMarkingConfig()
    }

    /** After user taps — resolve correct answer from already-loaded mcqs */
    fun fetchMcqAnswer(mcqId: String) {
        val mcq = _mcqs.value.find { it.id == mcqId } ?: return
        val answer = CaMcqAnswerDto(correct = mcq.correct, explanation = mcq.explanation)
        _mcqAnswers.update { it + (mcqId to answer) }
    }

    fun clearMcqError() { _mcqError.value = null }

    fun submitMcqAttempt(affairId: String, answers: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = com.example.bpscnotes.data.remote.api.CaMcqSubmitRequest(
                    answers = answers.map { (qId, ans) ->
                        com.example.bpscnotes.data.remote.api.CaMcqAnswerSubmitDto(questionId = qId, answer = ans)
                    }
                )
                api.submitMcqAttempt(affairId, payload)
            } catch (e: Exception) {
                Log.w("CurrentAffairsVM", "submitMcqAttempt failed, scheduling retry: ${e.message}")
                scheduleRetry(affairId, answers)
            }
        }
    }

    private fun scheduleRetry(affairId: String, answers: Map<String, String>) {
        val request = OneTimeWorkRequestBuilder<CaMcqSubmitWorker>()
            .setInputData(workDataOf(
                CaMcqSubmitWorker.KEY_AFFAIR_ID to affairId,
                CaMcqSubmitWorker.KEY_ANSWERS_JSON to Gson().toJson(answers),
            ))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext).enqueue(request)
    }

    /** Called by TrackStudyTime — logs time silently in background */
    fun logStudyTime(activityType: String, durationSecs: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                api.logActivity(
                    com.example.bpscnotes.data.remote.api.LogActivityRequest(
                        activityType = activityType,
                        durationSecs = durationSecs
                    )
                )
            } catch (_: Exception) { /* silent — tracking must never crash the app */ }
        }
    }

    // ── PDF export ───────────────────────────────────────────────
    // Downloads the article PDF via the authenticated Retrofit client
    // (the endpoint requires the same JWT as every other current-affairs
    // call), saves it into app-private cache, and hands back a
    // FileProvider content:// Uri for the caller to open/share — same
    // shape as CourseDetailViewModel.downloadCertificateForShare().
    suspend fun downloadArticlePdf(affairId: String, title: String): Uri? {
        return withContext(Dispatchers.IO) {
            _isDownloadingPdf.value = true
            _pdfError.value = null
            try {
                val response = api.downloadArticlePdf(affairId)
                if (!response.isSuccessful) throw Exception("Download failed: HTTP ${response.code()}")
                val body = response.body() ?: throw Exception("Empty PDF response")

                val dir = File(appContext.cacheDir, "current-affairs-pdfs").apply { mkdirs() }
                val safeName = title.ifBlank { "article" }
                    .replace(Regex("[^a-zA-Z0-9 _-]"), "")
                    .trim()
                    .take(60)
                    .ifBlank { "article" }
                val file = File(dir, "${safeName}_$affairId.pdf")

                body.byteStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }

                FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Log.e("CurrentAffairsVM", "downloadArticlePdf: ${e.message}", e)
                _pdfError.value = e.toUserMessage("Could not download PDF")
                null
            } finally {
                _isDownloadingPdf.value = false
            }
        }
    }
}