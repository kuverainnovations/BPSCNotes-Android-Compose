package com.example.bpscnotes.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper.
 *
 * Every endpoint from the BPSCNotes backend returns this shape:
 * {
 *   "success": true,
 *   "message": "OK",
 *   "data": { ... }
 * }
 *
 * Usage:
 *   suspend fun getCourses(): ApiResponse<CoursesResponseData>
 *   val courses = response.data?.courses ?: emptyList()
 */

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: T?
)