package com.example.bpscnotes.data.remote.dto

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
    val success: Boolean = false,
    val message: String  = "",
    val data: T?         = null
)
