package com.example.bpscnotes.presentation.studymaterials

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// FILE: presentation/studymaterials/StudyMaterialsViewModel.kt
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
    val sortBy:             String                 = "downloads",    // newest|downloads|rating
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
    val downloadProgress:   Float                  = 0f,
    val downloadedIds:      Set<String>            = emptySet(),

    // Bookmark (optimistic)
    val bookmarkedIds:      Set<String>            = emptySet(),

    // Toast messages
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
            } catch (e: Exception) {
                Log.w(TAG, "loadSubjects: ${e.message}")
            }
        }
    }

    // ── Stats ─────────────────────────────────────────────────
    private fun loadStats() {
        viewModelScope.launch {
            try {
                val res = api.getStats()
                _state.update { it.copy(stats = res.data) }
            } catch (e: Exception) {
                Log.w(TAG, "loadStats: ${e.message}")
            }
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
                    isLoadingList  = reset && it.materials.isEmpty(),
                    isLoadingMore  = loadMore,
                    isRefreshing   = reset && it.materials.isNotEmpty(),
                    listError      = null
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
                val data = res.data ?: throw Exception("Empty response")
                val newList = if (reset) data.materials else s.materials + data.materials

                // Build bookmarked set from response
                val bookmarked = newList.filter { it.isBookmarked }.map { it.id }.toSet()

                _state.update {
                    it.copy(
                        materials       = newList,
                        hasNextPage     = data.meta.hasNext,
                        currentPage     = page,
                        isLoadingList   = false,
                        isRefreshing    = false,
                        isLoadingMore   = false,
                        bookmarkedIds   = if (reset) bookmarked else it.bookmarkedIds + bookmarked,
                        listError       = null
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

    fun refresh() = loadMaterials(reset = true)
    fun loadMore() = loadMaterials(loadMore = true)

    // ── Filters ───────────────────────────────────────────────
    fun selectType(type: MaterialType?) {
        _state.update { it.copy(selectedType = type) }
        loadMaterials(reset = true)
    }

    fun selectSubject(subject: String) {
        _state.update { it.copy(selectedSubject = subject) }
        loadMaterials(reset = true)
    }

    fun setSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)  // debounce
            loadMaterials(reset = true)
        }
    }

    fun setSortBy(sort: String) {
        _state.update { it.copy(sortBy = sort) }
        loadMaterials(reset = true)
    }

    fun toggleBookmarksOnly() {
        _state.update { it.copy(showBookmarksOnly = !it.showBookmarksOnly) }
        loadMaterials(reset = true)
    }

    // ── Detail sheet ──────────────────────────────────────────
    fun openDetail(materialId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingDetail = true) }
            try {
                val res = api.getMaterial(materialId)
                _state.update { it.copy(selectedMaterial = res.data, isLoadingDetail = false) }
            } catch (e: Exception) {
                Log.e(TAG, "openDetail: ${e.message}", e)
                _state.update { it.copy(isLoadingDetail = false, toastMessage = e.message ?: "Failed to load") }
            }
        }
    }

    fun closeDetail() = _state.update { it.copy(selectedMaterial = null) }

    // ── Bookmark (optimistic) ─────────────────────────────────
    fun toggleBookmark(materialId: String) {
        val current = _state.value.bookmarkedIds.contains(materialId)
        // Optimistic update
        _state.update {
            it.copy(
                bookmarkedIds = if (current) it.bookmarkedIds - materialId else it.bookmarkedIds + materialId,
                selectedMaterial = if (it.selectedMaterial?.id == materialId)
                    it.selectedMaterial.copy(isBookmarked = !current) else it.selectedMaterial
            )
        }
        viewModelScope.launch {
            try {
                val res = api.toggleBookmark(materialId)
                val serverVal = res.data?.bookmarked ?: !current
                if (serverVal != !current) {
                    // Revert if server disagrees
                    _state.update { it.copy(bookmarkedIds = if (serverVal) it.bookmarkedIds + materialId else it.bookmarkedIds - materialId) }
                }
                _state.update { it.copy(toastMessage = if (serverVal) "🔖 Saved" else "Removed from saved") }
            } catch (e: Exception) {
                // Revert optimistic update on error
                _state.update { it.copy(bookmarkedIds = if (current) it.bookmarkedIds + materialId else it.bookmarkedIds - materialId) }
                Log.e(TAG, "toggleBookmark: ${e.message}", e)
            }
        }
    }

    // ── Download via Android DownloadManager ──────────────────
    fun downloadMaterial(material: StudyMaterialDto) {
        if (_state.value.downloadedIds.contains(material.id)) {
            _state.update { it.copy(toastMessage = "Already downloaded") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(downloadingId = material.id, downloadProgress = 0f) }
            try {
                // Get signed download URL from backend
                val res = api.recordDownload(material.id)
                val url = res.data?.downloadUrl ?: throw Exception("No download URL")

                // Use DownloadManager for background download + notification
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val fileName = "${material.title.replace("[^a-zA-Z0-9]".toRegex(), "_")}.pdf"
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle(material.title)
                    .setDescription("Downloading from BPSCNotes")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "BPSCNotes/$fileName")
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
                dm.enqueue(request)

                _state.update {
                    it.copy(
                        downloadingId  = null,
                        downloadedIds  = it.downloadedIds + material.id,
                        toastMessage   = "⬇️ Download started — check Downloads app"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "download: ${e.message}", e)
                _state.update { it.copy(downloadingId = null, toastMessage = "Download failed: ${e.message}") }
            }
        }
    }

    // ── Upload flow ───────────────────────────────────────────
    fun showUpload()   = _state.update { it.copy(showUploadSheet = true, uploadSuccess = null, uploadError = null) }
    fun hideUpload()   = _state.update { it.copy(showUploadSheet = false) }

    fun uploadMaterial(
        localUri:     android.net.Uri,
        title:        String,
        description:  String,
        subject:      String,
        type:         MaterialType,
        author:       String,
        tags:         List<String>,
        pageCount:    Int,
    ) {
        if (title.isBlank() || subject.isBlank()) {
            _state.update { it.copy(uploadError = "Title and subject are required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, uploadProgress = 0f, uploadError = null) }
            try {
                // 1. Get file metadata
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(localUri) ?: "application/pdf"
                val fileName = getFileName(localUri) ?: "upload.pdf"
                val fileSizeBytes = getFileSize(localUri)

                // 2. Get pre-signed upload URL from backend
                _state.update { it.copy(uploadProgress = 0.1f) }
                val urlRes = api.getUploadUrl(fileName, mimeType)
                val uploadUrl = urlRes.data?.uploadUrl ?: throw Exception("Failed to get upload URL")
                val fileKey   = urlRes.data.fileKey

                // 3. Upload directly to S3 via OkHttp with progress tracking
                _state.update { it.copy(uploadProgress = 0.2f) }
                val inputStream = contentResolver.openInputStream(localUri)
                    ?: throw Exception("Cannot read file")

                val fileBytes = inputStream.readBytes()
                inputStream.close()

                // Upload to S3
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val requestBody = okhttp3.RequestBody.create(
                    mimeType.toMediaTypeOrNull(), fileBytes
                )
                // AWS SDK v3 requires CRC32 checksum header in the PUT request
                val crc32Header = computeCrc32Header(fileBytes)
                val request = okhttp3.Request.Builder()
                    .url(uploadUrl)
                    .put(requestBody)
                    .addHeader("Content-Type", mimeType)
                    .addHeader("x-amz-checksum-crc32", crc32Header)
                    .build()

                _state.update { it.copy(uploadProgress = 0.5f) }
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "no body"
                    Log.e(TAG, "S3 upload failed HTTP " + response.code.toString() + ": " + errorBody)
                    throw Exception("S3 upload failed: HTTP " + response.code.toString())
                }
                _state.update { it.copy(uploadProgress = 0.8f) }

                // 4. Create material record in backend
                val createRes = api.createMaterial(
                    CreateMaterialRequest(
                        title        = title.trim(),
                        description  = description.trim().ifEmpty { null },
                        subject      = subject.trim(),
                        materialType = type.apiKey,
                        author       = author.trim().ifEmpty { null },
                        tags         = tags.filter { it.isNotBlank() },
                        fileKey      = fileKey,
                        fileSizeMb   = fileSizeBytes / (1024f * 1024f),
                        pageCount    = pageCount
                    )
                )
                _state.update {
                    it.copy(
                        isUploading   = false,
                        uploadProgress = 1f,
                        uploadSuccess = "✅ Uploaded! Will appear after admin review.",
                        showUploadSheet = false
                    )
                }
                loadStats()  // refresh stats header

            } catch (e: Exception) {
                Log.e(TAG, "uploadMaterial: ${e.message}", e)
                _state.update { it.copy(isUploading = false, uploadError = e.message ?: "Upload failed") }
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
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        } catch (e: Exception) { null }
    }

    private fun getFileSize(uri: android.net.Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                cursor.moveToFirst()
                cursor.getLong(sizeIndex)
            } ?: 0L
        } catch (e: Exception) { 0L }
    }
    // Compute CRC32 checksum required by AWS SDK v3 presigned URLs
    private fun computeCrc32Header(bytes: ByteArray): String {
        val crc = java.util.zip.CRC32()
        crc.update(bytes)
        val v = crc.value
        val buf = ByteArray(4)
        buf[0] = ((v shr 24) and 0xFF).toByte()
        buf[1] = ((v shr 16) and 0xFF).toByte()
        buf[2] = ((v shr  8) and 0xFF).toByte()
        buf[3] = ((v       ) and 0xFF).toByte()
        return android.util.Base64.encodeToString(buf, android.util.Base64.NO_WRAP)
    }

}