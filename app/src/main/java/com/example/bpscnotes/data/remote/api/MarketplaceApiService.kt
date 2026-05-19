package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.example.bpscnotes.presentation.marketplace.FileAccessData
import com.example.bpscnotes.presentation.marketplace.MarketplaceDetailData
import com.example.bpscnotes.presentation.marketplace.MarketplaceListData
import com.example.bpscnotes.presentation.marketplace.PurchaseData
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MarketplaceApiService {
    @GET("marketplace")
    suspend fun list(
        @Query("page")    page: Int = 1,
        @Query("limit")   limit: Int = 30,
        @Query("subject") subject: String? = null,
        @Query("search")  search: String? = null,
        @Query("sort")    sort: String? = null
    ): ApiResponse<MarketplaceListData>

    @GET("marketplace/my-listings")
    suspend fun myListings(): ApiResponse<Any>

    @GET("marketplace/my-purchases")
    suspend fun myPurchases(): ApiResponse<Any>

    @GET("marketplace/{id}")
    suspend fun getDetail(@Path("id") id: String): ApiResponse<MarketplaceDetailData>

    @GET("marketplace/{id}/access")
    suspend fun getFileAccess(@Path("id") id: String): ApiResponse<FileAccessData>

    @POST("marketplace/{id}/purchase")
    suspend fun purchase(@Path("id") id: String): ApiResponse<PurchaseData>
}
