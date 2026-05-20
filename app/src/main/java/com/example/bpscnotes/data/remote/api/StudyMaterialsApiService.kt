package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

// ════════════════════════════════════════════════════════════
// FILE: data/remote/api/StudyMaterialsApiService.kt
// All DTOs + Retrofit interface for Study Materials
// ════════════════════════════════════════════════════════════

// ── Enums ─────────────────────────────────────────────────────
enum class MaterialType(val apiKey: String, val label: String, val emoji: String) {
    PDF("pdf",   "PDF Notes",      "📄"),
    PYQ("pyq",   "Prev. Papers",   "📝"),
    BOOK("book", "Books",          "📚"),
    VIDEO("video","Video Notes",   "🎬");

    companion object {
        fun fromKey(key: String?) = values().firstOrNull { it.apiKey == key } ?: PDF
    }
}

// ── DTOs ──────────────────────────────────────────────────────
data class StudyMaterialDto(
    val id: String,
    val title: String,
    val description: String?,
    val subject: String,
    @SerializedName("materialType")  val materialType: String,   // "pdf"|"pyq"|"book"|"video"
    val author: String?,
    val tags: List<String> = emptyList(),
    @SerializedName("fileSizeBytes") val fileSizeBytes: Long = 0,
    @SerializedName("pageCount")     val pageCount: Int = 0,
    @SerializedName("isPremium")     val isPremium: Boolean = false,
    @SerializedName("isFeatured")    val isFeatured: Boolean = false,
    @SerializedName("isTrending")    val isTrending: Boolean = false,
    @SerializedName("isNew")         val isNew: Boolean = false,
    @SerializedName("downloadCount") val downloadCount: Int = 0,
    @SerializedName("viewCount")     val viewCount: Int = 0,
    val rating: Float = 0f,
    @SerializedName("uploadedDate")  val uploadedDate: String?,
    @SerializedName("uploaderName")  val uploaderName: String?,
    @SerializedName("is_bookmarked") val isBookmarked: Boolean = false,
    val downloadUrl: String? = null,     // only present in detail call
    val status: String? = null,          // only in my-uploads
    @SerializedName("rejectionReason") val rejectionReason: String? = null
) {
    val type: MaterialType get() = MaterialType.fromKey(materialType)
    val fileSizeMb: Float  get() = fileSizeBytes / (1024f * 1024f)
}

data class PaginationMeta(
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
    val hasNext: Boolean,
)

data class MaterialListData(
    val materials: List<StudyMaterialDto>,
    val meta: PaginationMeta
)


data class UploadResultData(
    val id: String,
    val title: String,
    val status: String,
    val fileUrl: String?,
    val fileKey: String?
)
data class MaterialDetailData(
    val id: String, val title: String, val description: String?,
    val subject: String,
    @SerializedName("materialType")  val materialType: String,
    val author: String?,
    val tags: List<String> = emptyList(),
    @SerializedName("fileSizeBytes") val fileSizeBytes: Long = 0,
    @SerializedName("pageCount")     val pageCount: Int = 0,
    @SerializedName("isPremium")     val isPremium: Boolean = false,
    @SerializedName("isFeatured")    val isFeatured: Boolean = false,
    @SerializedName("isTrending")    val isTrending: Boolean = false,
    @SerializedName("isNew")         val isNew: Boolean = false,
    @SerializedName("downloadCount") val downloadCount: Int = 0,
    val rating: Float = 0f,
    @SerializedName("uploadedDate")  val uploadedDate: String?,
    @SerializedName("uploaderName")  val uploaderName: String?,
    @SerializedName("is_bookmarked") val isBookmarked: Boolean = false,
    val downloadUrl: String?
) {
    val type: MaterialType get() = MaterialType.fromKey(materialType)
}

data class StatsData(
    val total: Int, val pdfs: Int, val pyqs: Int,
    val books: Int, val totalDownloads: Int
)

data class SubjectsData(val subjects: List<String>)

data class UploadUrlData(val uploadUrl: String, val fileKey: String)

data class DownloadData(val downloadUrl: String)

data class BookmarkData(val bookmarked: Boolean)

data class MyUploadsData(val uploads: List<StudyMaterialDto>)

// ── Create DTO ────────────────────────────────────────────────
data class CreateMaterialRequest(
    val title: String,
    val description: String? = null,
    val subject: String,
    val materialType: String,
    val author: String? = null,
    val tags: List<String> = emptyList(),
    val fileKey: String,
    val fileSizeMb: Float? = null,
    val pageCount: Int? = null
)

// ════════════════════════════════════════════════════════════
// RETROFIT INTERFACE
// ════════════════════════════════════════════════════════════
interface StudyMaterialsApiService {

    /** GET /study-materials/stats */
    @GET("study-materials/stats")
    suspend fun getStats(): ApiResponse<StatsData>

    /** GET /study-materials/subjects */
    @GET("study-materials/subjects")
    suspend fun getSubjects(): ApiResponse<SubjectsData>

    /**
     * GET /study-materials?type=pdf&subject=Polity&search=...&page=1&limit=20&sort=newest
     * sort: newest | downloads | rating
     * bookmarkedOnly: true (show saved only)
     */
    @GET("study-materials")
    suspend fun list(
        @Query("type")           type:          String? = null,
        @Query("subject")        subject:       String? = null,
        @Query("search")         search:        String? = null,
        @Query("page")           page:          Int     = 1,
        @Query("limit")          limit:         Int     = 20,
        @Query("sort")           sort:          String? = null,
        @Query("bookmarkedOnly") bookmarkedOnly: Boolean = false
    ): ApiResponse<MaterialListData>

    /** GET /study-materials/my-uploads */
    @GET("study-materials/my-uploads")
    suspend fun myUploads(): ApiResponse<MyUploadsData>

    /**
     * GET /study-materials/upload-url?fileName=notes.pdf&mimeType=application/pdf
     * Returns a pre-signed S3 URL for direct upload from Android.
     */
    @GET("study-materials/upload-url")
    suspend fun getUploadUrl(
        @Query("fileName")  fileName:  String,
        @Query("mimeType")  mimeType:  String = "application/pdf"
    ): ApiResponse<UploadUrlData>

    /** POST /study-materials — create record AFTER S3 upload */
    @POST("study-materials")
    suspend fun createMaterial(@Body dto: CreateMaterialRequest): ApiResponse<StudyMaterialDto>

    /** GET /study-materials/:id — with signed download URL */
    @GET("study-materials/{id}")
    suspend fun getMaterial(@Path("id") id: String): ApiResponse<MaterialDetailData>

    /** POST /study-materials/:id/download — record download + get URL */
    @POST("study-materials/{id}/download")
    suspend fun recordDownload(@Path("id") id: String): ApiResponse<DownloadData>

    /** POST /study-materials/:id/bookmark — toggle bookmark */
    @POST("study-materials/{id}/bookmark")
    suspend fun toggleBookmark(@Path("id") id: String): ApiResponse<BookmarkData>

    /**
     * POST /study-materials/upload  (multipart/form-data)
     *
     * Single-step upload directly to our server — no AWS needed.
     * Replaces the two-step presigned URL + OkHttp PUT flow.
     * All fields except file are plain text parts.
     */
    @Multipart
    @POST("study-materials/upload")
    suspend fun uploadMaterial(
        @Part                               file:         MultipartBody.Part,
        @Part("title")                      title:        RequestBody,
        @Part("description")               description:  RequestBody,
        @Part("subject")                   subject:      RequestBody,
        @Part("materialType")              materialType: RequestBody,
        @Part("author")                    author:       RequestBody,
        @Part("tags")                      tags:         RequestBody,
        @Part("pageCount")                 pageCount:    RequestBody,
    ): ApiResponse<UploadResultData>
}