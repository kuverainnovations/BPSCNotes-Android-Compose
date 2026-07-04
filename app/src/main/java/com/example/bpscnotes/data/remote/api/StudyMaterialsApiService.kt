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
    PDF(        "pdf",         "PDF Notes",    "📄"),
    PYQ(        "pyq",         "Prev. Papers", "📝"),
    BOOK(       "book",        "Books",        "📚"),
    HANDWRITTEN("handwritten", "Handwritten",  "✏️");
    // VIDEO — disabled, Phase 2

    companion object {
        fun fromKey(key: String?) = values().firstOrNull { it.apiKey == key } ?: PDF
    }
}

data class StudyMaterialDto(

    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("subject")
    val subject: String,

    @SerializedName("material_type")
    val materialType: String,

    @SerializedName("author")
    val author: String?,

    @SerializedName("tags")
    val tags: List<String> = emptyList(),

    @SerializedName("file_size_bytes")
    val fileSizeBytes: Long = 0,

    @SerializedName("page_count")
    val pageCount: Int = 0,

    @SerializedName("is_premium")
    val isPremium: Boolean = false,

    @SerializedName("language")
    val language: String = "English",

    @SerializedName("is_featured")
    val isFeatured: Boolean = false,

    @SerializedName("is_trending")
    val isTrending: Boolean = false,

    @SerializedName("is_new")
    val isNew: Boolean = false,

    @SerializedName("download_count")
    val downloadCount: Int = 0,

    @SerializedName("view_count")
    val viewCount: Int = 0,

    @SerializedName("rating")
    val rating: Float = 0f,

    @SerializedName("created_at")
    val uploadedDate: String?,

    @SerializedName("uploader_name")
    val uploaderName: String?,

    @SerializedName("is_bookmarked")
    val isBookmarked: Boolean = false,

    @SerializedName("fileUrl")
    val fileUrl: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("rejection_reason")
    val rejectionReason: String? = null,

    @SerializedName("price")
    val price: Double = 0.0,

    @SerializedName("free_pages")
    val freePages: Int = 3,

    val downloadUrl: String? = null,


    @SerializedName("is_marketplace")
    val isMarketplace: Boolean = false,

    @SerializedName("is_purchased")
    val isPurchased: Boolean = false,          // true if current user has purchased this premium material

    // ── Negotiation (Phase 2) ────────────────────────────────
    @SerializedName("negotiation_round")
    val negotiationRound: Int = 0,

    @SerializedName("current_offer_price")
    val currentOfferPrice: Int? = null,

    @SerializedName("proposed_by")
    val proposedBy: String? = null,            // "admin" | "user"

    @SerializedName("negotiation_status")
    val negotiationStatus: String = "none",    // "none" | "awaiting_user" | "awaiting_admin" | "resolved"

    @SerializedName("buyer_count")
    val buyerCount: Int = 0,                   // social proof: "12 students bought this"
) {
    val type: MaterialType
        get() = MaterialType.fromKey(materialType)

    val fileSizeMb: Float
        get() = fileSizeBytes / (1024f * 1024f)

    val isFree: Boolean
        get() = price == 0.0

    val resolvedUrl: String? get() = fileUrl ?: downloadUrl

    /** True when there's an admin counter-offer awaiting this uploader's response */
    val hasNegotiationOffer: Boolean
        get() = status == "negotiating" && negotiationStatus == "awaiting_user"
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
    @SerializedName("material_type")  val materialType: String,
    val author: String?,
    val tags: List<String> = emptyList(),
    @SerializedName("file_size_bytes") val fileSizeBytes: Long = 0,
    @SerializedName("page_count")      val pageCount: Int = 0,
    @SerializedName("is_premium")      val isPremium: Boolean = false,
    @SerializedName("is_featured")     val isFeatured: Boolean = false,
    @SerializedName("is_trending")     val isTrending: Boolean = false,
    @SerializedName("is_new")          val isNew: Boolean = false,
    @SerializedName("download_count")  val downloadCount: Int = 0,
    val rating: Float = 0f,
    @SerializedName("created_at")      val uploadedDate: String?,
    @SerializedName("uploader_name")   val uploaderName: String?,
    @SerializedName("uploader_id")     val uploaderId: String? = null,
    @SerializedName("is_bookmarked")   val isBookmarked: Boolean = false,
    val fileUrl: String? = null,
    val downloadUrl: String? = null,
    val language: String = "English",
    // Marketplace / PDF lock fields
    val price: Double = 0.0,
    @SerializedName("free_pages")      val freePages: Int = 3,
    @SerializedName("is_purchased")    val isPurchased: Boolean = false,
    @SerializedName("buyer_count")     val buyerCount: Int = 0,
    // Google Play one-time product id, synced when the material's price
    // becomes final (approval / negotiation acceptance). Null until then —
    // the release-build Play Billing purchase path isn't available for a
    // material that hasn't synced yet.
    @SerializedName("gplay_product_id") val gplayProductId: String? = null,
) {
    val type: MaterialType get() = MaterialType.fromKey(materialType)
    val resolvedUrl: String? get() = fileUrl ?: downloadUrl
    val isFree: Boolean get() = price == 0.0
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

// ── Negotiation (Phase 2) ────────────────────────────────────
data class NegotiationOfferDto(
    val round: Int,
    @SerializedName("offered_by") val offeredBy: String,   // "admin" | "user"
    @SerializedName("offer_price") val offerPrice: Int,
    val message: String? = null,
    val action: String,    // "counter" | "accept" | "final_approve" | "final_reject"
    @SerializedName("created_at") val createdAt: String? = null,
)

data class NegotiationHistoryData(
    val materialId: String,
    val title: String,
    val status: String,
    val originalPrice: Double,
    val negotiationRound: Int,
    val currentOfferPrice: Int? = null,
    val proposedBy: String? = null,
    val negotiationStatus: String,
    val canRespond: Boolean = false,
    val isFinalRound: Boolean = false,
    val history: List<NegotiationOfferDto> = emptyList(),
)

data class NegotiationActionResultData(
    val status: String? = null,
    val finalPrice: Int? = null,
    val negotiationRound: Int? = null,
    val currentOfferPrice: Int? = null,
    val isFinalRound: Boolean = false,
)

data class CounterOfferRequest(val price: Double = 0.0, val message: String? = null)


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

// ── Marketplace / Downloads DTOs ────────────────────────────
data class DownloadHistoryItem(
    val id:            String  = "",
    val title:         String  = "",
    val subject:       String  = "",
    @SerializedName("materialType")  val materialType:  String  = "pdf",
    @SerializedName("fileSizeBytes") val fileSizeBytes: Long    = 0L,
    @SerializedName("pageCount")     val pageCount:     Int     = 0,
    @SerializedName("isPremium")     val isPremium:     Boolean = false,
    val price: Double = 0.0,
    @SerializedName("freePages")     val freePages:     Int     = 3,
    val rating:        Float   = 0f,
    @SerializedName("uploaderName")  val uploaderName:  String? = null,
    @SerializedName("downloadedAt")  val downloadedAt:  String  = "",
    val fileUrl:       String? = null,
    @SerializedName("isPurchased")   val isPurchased:   Boolean = false,
    val language:      String  = "English",
)

data class MyDownloadsData(val downloads: List<DownloadHistoryItem> = emptyList())

// ── Marketplace purchase — hybrid coins + Cashfree (Phase 3) ──
data class InitPurchaseRequest(val coinsToApply: Int = 0)

data class InitPurchaseData(
    val purchased:        Boolean = false,
    val alreadyPurchased: Boolean = false,
    val requiresPayment:  Boolean = false,
    val purchaseOrderId:  String? = null,
    val materialPrice:    Int     = 0,
    val coinsApplied:     Int     = 0,
    val coinDiscountInr:  Int     = 0,
    val amountDueInr:     Int     = 0,
    val paymentSessionId: String? = null,   // → Cashfree Android SDK
    val providerOrderId:  String? = null,
    val paymentEnvironment: String? = null, // 'sandbox' | 'production' — drives CFSession.Environment
    val materialTitle:    String? = null,
    // present when purchase completed immediately (free or fully coin-covered)
    val coinsSpent:       Int     = 0,
    val coinsBalance:     Int?    = null,
    val amountPaidInr:    Int     = 0,
    val fileUrl:          String? = null,
)

data class ConfirmPurchaseRequest(
    val purchaseOrderId: String,
    val cfPaymentId:     String,            // from Cashfree SDK
    val paymentMethod:   String? = null,
)

data class VerifyGplayMaterialRequest(val purchaseToken: String)

data class PurchaseResultData(
    val purchased:        Boolean = false,
    val alreadyPurchased: Boolean = false,
    val coinsSpent:       Int     = 0,
    val coinsBalance:     Int?    = 0,
    val coinDiscountInr:  Int     = 0,
    val amountPaidInr:    Int     = 0,
    val fileUrl:          String? = null
)

// ── Seller wallet (Phase 3) ───────────────────────────────────
data class WalletTransactionDto(
    val id: String,
    val type: String,           // "sale_credit" | "withdrawal" | "adjustment"
    val amount: Int,             // ₹
    val status: String,          // "pending" | "disbursed" | "failed"
    val description: String? = null,
    @SerializedName("balance_after") val balanceAfter: Int = 0,
    @SerializedName("material_title") val materialTitle: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("disbursed_at") val disbursedAt: String? = null,
)

data class WalletData(
    val balance: Int = 0,
    val totalEarned: Int = 0,
    val transactions: List<WalletTransactionDto> = emptyList(),
    val meta: PaginationMeta? = null,
)

data class WithdrawalRequest(
    val amount: Int,
    @SerializedName("upiId") val upiId: String? = null
)

data class WithdrawalResult(
    @SerializedName("newBalance") val newBalance: Int = 0
)

data class PreviewData(
    val previewUrl:  String?  = null,
    val totalPages:  Int      = 0,
    val freePages:   Int      = 3,
    val isPurchased: Boolean  = false
)

// ── Rating DTOs ───────────────────────────────────────────────
data class RateMaterialRequest(val stars: Int, val review: String? = null)
data class RateMaterialResponse(val stars: Int = 0, val avgRating: Float = 0f)
data class MyRatingData(val stars: Int = 0, val review: String? = null)

// ── Social proof (Phase 4) ─────────────────────────────────────
data class BuyersData(
    val buyers: List<String> = emptyList(),   // anonymized e.g. "Rahul K."
    val totalBuyers: Int = 0,
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
     * GET /study-materials/pinned
     * Returns ALL featured/pinned materials, unaffected by the main list's
     * pagination — so pinned items always show in the Pinned section,
     * not just when they happen to rank in the first page of results.
     */
    @GET("study-materials/pinned")
    suspend fun getPinned(): ApiResponse<MaterialListData>

    /**
     * GET /study-materials?type=pdf&subject=Polity&search=...&page=1&limit=20&sort=newest
     * sort: newest | downloads | rating
     * bookmarkedOnly: true (show saved only)
     */
    @GET("study-materials")
    suspend fun list(
        @Query("type")           type:          String? = null,
        @Query("subject")        subject:       String? = null,
        @Query("language")        language:       String? = null,
        @Query("search")         search:        String? = null,
        @Query("page")           page:          Int     = 1,
        @Query("limit")          limit:         Int     = 20,
        @Query("sort")           sort:          String? = null,
        @Query("bookmarkedOnly") bookmarkedOnly: Boolean = false,
        @Query("isPremium")      isPremium:     Boolean? = null   // null = all, true = premium only
    ): ApiResponse<MaterialListData>

    /** GET /study-materials/my-uploads */
    @GET("study-materials/my-uploads")
    suspend fun myUploads(): ApiResponse<MyUploadsData>

    // ── Negotiation (Phase 2) ────────────────────────────────────
    /** GET /study-materials/my-uploads/:id/negotiation — full offer history */
    @GET("study-materials/my-uploads/{id}/negotiation")
    suspend fun getNegotiationHistory(@Path("id") id: String): ApiResponse<NegotiationHistoryData>

    /** POST /study-materials/my-uploads/:id/negotiation/accept — accept admin's counter-offer */
    @POST("study-materials/my-uploads/{id}/negotiation/accept")
    suspend fun acceptNegotiation(@Path("id") id: String): ApiResponse<NegotiationActionResultData>

    /** POST /study-materials/my-uploads/:id/negotiation/counter — send a counter-offer back */
    @POST("study-materials/my-uploads/{id}/negotiation/counter")
    suspend fun counterNegotiation(@Path("id") id: String, @Body body: CounterOfferRequest): ApiResponse<NegotiationActionResultData>

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

    /** GET /study-materials/my-downloads — user's download history (paginated) */
    @GET("study-materials/my-downloads")
    suspend fun getMyDownloads(
        @Query("page")  page: Int  = 1,
        @Query("limit") limit: Int = 50
    ): ApiResponse<MyDownloadsData>

    /** DELETE /study-materials/my-downloads/:id — remove from history */
    @DELETE("study-materials/my-downloads/{materialId}")
    suspend fun removeDownload(@Path("materialId") materialId: String): ApiResponse<Unit>

    /**
     * POST /study-materials/:id/purchase/init — start a marketplace purchase.
     * Buyer may apply up to `max_coins_per_purchase` coins as a discount;
     * remaining ₹ balance (if any) must be paid via Cashfree using the
     * returned paymentSessionId. If amountDueInr is 0, purchased=true
     * immediately (free or fully coin-covered).
     */
    @POST("study-materials/{id}/purchase/init")
    suspend fun initPurchase(@Path("id") id: String, @Body body: InitPurchaseRequest): ApiResponse<InitPurchaseData>

    /** POST /study-materials/:id/purchase/confirm — finalize after Cashfree payment succeeds */
    @POST("study-materials/{id}/purchase/confirm")
    suspend fun confirmPurchase(@Path("id") id: String, @Body body: ConfirmPurchaseRequest): ApiResponse<PurchaseResultData>

    /** POST /study-materials/:id/purchase/gplay/verify — finalize after Google Play Billing succeeds (release builds) */
    @POST("study-materials/{id}/purchase/gplay/verify")
    suspend fun verifyGplayMaterialPurchase(@Path("id") id: String, @Body body: VerifyGplayMaterialRequest): ApiResponse<PurchaseResultData>

    /** GET /study-materials/wallet — seller's ₹ wallet balance + transaction history */
    @GET("study-materials/wallet")
    suspend fun getWallet(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): ApiResponse<WalletData>

    /** POST /study-materials/wallet/withdraw — request payout */
    @POST("study-materials/wallet/withdraw")
    suspend fun requestWithdrawal(@Body body: WithdrawalRequest): ApiResponse<WithdrawalResult>

    /** GET /study-materials/:id/preview — get signed preview (free pages only) */
    @GET("study-materials/{id}/preview")
    suspend fun getPreview(@Path("id") id: String): ApiResponse<PreviewData>

    // ── Rating ──────────────────────────────────────────────────
    /** POST /study-materials/:id/rate — submit or update a 1-5 star rating */
    @POST("study-materials/{id}/rate")
    suspend fun rateMaterial(
        @Path("id") id: String,
        @Body body: RateMaterialRequest
    ): ApiResponse<RateMaterialResponse>

    /** GET /study-materials/:id/my-rating — fetch current user's rating for this material */
    @GET("study-materials/{id}/my-rating")
    suspend fun getMyRating(@Path("id") id: String): ApiResponse<MyRatingData>

    /** GET /study-materials/:id/buyers — anonymized buyer list for social proof */
    @GET("study-materials/{id}/buyers")
    suspend fun getBuyers(@Path("id") id: String, @Query("limit") limit: Int = 5): ApiResponse<BuyersData>


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
        @Part("isPremium")                 isPremium:    RequestBody,
        @Part("freePages")                 freePages:    RequestBody,
        @Part("price")                     price:        RequestBody,
        @Part("language")                  language:     RequestBody,
    ): ApiResponse<UploadResultData>
}