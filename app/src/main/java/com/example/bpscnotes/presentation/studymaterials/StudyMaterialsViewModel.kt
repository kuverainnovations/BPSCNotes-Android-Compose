package com.example.bpscnotes.presentation.studymaterials

import android.app.Application
import android.app.DownloadManager
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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

data class StudyMaterialsUiState(
    // List
    val materials:          List<StudyMaterialDto> = emptyList(),
    val isLoadingList:      Boolean                = true,
    val isRefreshing:       Boolean                = false,
    val listError:          String?                = null,
    val hasNextPage:        Boolean                = false,
    val currentPage:        Int                    = 1,
    val isLoadingMore:      Boolean                = false,

    // Filters
    val selectedType:       MaterialType?          = null,
    val selectedSubject:    String                 = "All",
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
    val isUploading:        Boolean                = false,
    val uploadProgress:     Float                  = 0f,
    val uploadSuccess:      String?                = null,
    val uploadError:        String?                = null,

    // My uploads tab
    val myUploads:          List<StudyMaterialDto> = emptyList(),
    val isLoadingMyUploads: Boolean                = false,

    // Download
    val downloadingId:      String?                = null,
    val downloadedIds:      Set<String>            = emptySet(),

    // Bookmark (optimistic)
    val bookmarkedIds:      Set<String>            = emptySet(),

    // Toast
    val toastMessage:       String?                = null,
)

@HiltViewModel
class StudyMaterialsViewModel @Inject constructor(
    private val api: StudyMaterialsApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(StudyMaterialsUiState())
    val state: StateFlow<StudyMaterialsUiState> = _state.asStateFlow()

    companion object { private const val TAG = "StudyMaterialsVM" }

    private var searchJob: Job? = null

    init {
        loadSubjects()
        loadStats()
        loadMaterials(reset = true)
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
    fun loadMaterials(reset: Boolean = false, loadMore: Boolean = false) {
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
                        listError = e.message ?: "Failed to load materials")
                }
            }
        }
    }

    fun refresh()   = loadMaterials(reset = true)
    fun loadMore()  = loadMaterials(loadMore = true)

    // ── Filters ───────────────────────────────────────────────
    fun selectType(type: MaterialType?)   { _state.update { it.copy(selectedType    = type)    }; loadMaterials(reset = true) }
    fun selectSubject(subject: String)    { _state.update { it.copy(selectedSubject = subject) }; loadMaterials(reset = true) }
    fun setSortBy(sort: String)           { _state.update { it.copy(sortBy          = sort)    }; loadMaterials(reset = true) }
    fun toggleBookmarksOnly()             { _state.update { it.copy(showBookmarksOnly = !it.showBookmarksOnly) }; loadMaterials(reset = true) }

    fun setSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(400); loadMaterials(reset = true) }
    }

    // ── Detail sheet ──────────────────────────────────────────
    fun openDetail(materialId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingDetail = true) }
            try {
                val res = api.getMaterial(materialId)
                _state.update { it.copy(selectedMaterial = res.data, isLoadingDetail = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingDetail = false, toastMessage = e.message ?: "Failed to load") }
            }
        }
    }

    fun closeDetail() = _state.update { it.copy(selectedMaterial = null) }

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

    // ── Download via DownloadManager ──────────────────────────
    fun downloadMaterial(material: StudyMaterialDto) {
        if (_state.value.downloadedIds.contains(material.id)) {
            _state.update { it.copy(toastMessage = "Already downloaded") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(downloadingId = material.id) }
            try {
                val res = api.recordDownload(material.id)
                val url = res.data?.downloadUrl ?: throw Exception("No download URL")

                val dm       = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val fileName = "${material.title.replace("[^a-zA-Z0-9]".toRegex(), "_")}.pdf"
                val request  = DownloadManager.Request(Uri.parse(url))
                    .setTitle(material.title)
                    .setDescription("Downloading from BPSCNotes")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "BPSCNotes/$fileName")
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
                dm.enqueue(request)

                _state.update {
                    it.copy(
                        downloadingId = null,
                        downloadedIds = it.downloadedIds + material.id,
                        toastMessage  = "⬇️ Download started — check Downloads app"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "download: ${e.message}", e)
                _state.update { it.copy(downloadingId = null, toastMessage = "Download failed: ${e.message}") }
            }
        }
    }

    // ── Upload ────────────────────────────────────────────────
    fun showUpload() = _state.update { it.copy(showUploadSheet = true, uploadSuccess = null, uploadError = null) }
    fun hideUpload() = _state.update { it.copy(showUploadSheet = false) }

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
        pageCount: Int
    ) {
        viewModelScope.launch {

            try {

                _state.update {
                    it.copy(
                        isUploading = true,
                        uploadProgress = 0f,
                        uploadError = null
                    )
                }


                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Unable to open file")

                val bytes = inputStream.readBytes()

                val requestFile = bytes.toRequestBody(
                    "application/pdf".toMediaTypeOrNull()
                )

                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    "upload.pdf",
                    requestFile
                )

                val response = api.uploadMaterial(
                    file = filePart,
                    title = title.toRequestBody("text/plain".toMediaTypeOrNull()),
                    description = description.toRequestBody("text/plain".toMediaTypeOrNull()),
                    subject = subject.toRequestBody("text/plain".toMediaTypeOrNull()),
                    materialType = type.apiKey.toRequestBody("text/plain".toMediaTypeOrNull()),
                    author = author.toRequestBody("text/plain".toMediaTypeOrNull()),
                    tags = Gson().toJson(tags)
                        .toRequestBody("text/plain".toMediaTypeOrNull()),
                    pageCount = pageCount.toString()
                        .toRequestBody("text/plain".toMediaTypeOrNull())
                )

                _state.update {
                    it.copy(
                        isUploading = false,
                        uploadProgress = 1f,
                        showUploadSheet = false,
                        toastMessage = response.message ?: "Upload successful"
                    )
                }

                refresh()

            } catch (e: Exception) {

                _state.update {
                    it.copy(
                        isUploading = false,
                        uploadError = e.message ?: "Upload failed"
                    )
                }
            }
        }
    }

    // ── My uploads ────────────────────────────────────────────
    fun loadMyUploads() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMyUploads = true) }
            try {
                val res = api.myUploads()
                _state.update { it.copy(myUploads = res.data?.uploads ?: emptyList(), isLoadingMyUploads = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingMyUploads = false) }
            }
        }
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
}
