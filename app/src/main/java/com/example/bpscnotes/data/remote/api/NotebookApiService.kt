package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ══════════════════════════════════════════════════════════════
// NOTEBOOK DTOs — personal study notes (dashboard → Notebook)
// ══════════════════════════════════════════════════════════════

data class NoteDto(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val color: String? = null,                       // 'yellow' | 'blue' | 'green' | 'pink' | 'purple' | 'orange'
    val subject: String? = null,                     // 'Polity', 'History', … or null
    @SerializedName("is_pinned")  val isPinned: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("updated_at") val updatedAt: String = "",
)

data class NotesListData(val notes: List<NoteDto> = emptyList())
data class NoteData(val note: NoteDto)

data class CreateNoteRequest(
    val title: String,
    val content: String,
    val color: String? = null,
    val subject: String? = null,
)

// All fields optional — send only what changed. isPinned toggles pin.
data class UpdateNoteRequest(
    val title: String? = null,
    val content: String? = null,
    val color: String? = null,
    val isPinned: Boolean? = null,
    val subject: String? = null,
)

interface NotebookApiService {

    /** GET /notebook — the user's notes, pinned first, newest first */
    @GET("notebook")
    suspend fun getNotes(@Query("search") search: String? = null): ApiResponse<NotesListData>

    /** POST /notebook — create a note */
    @POST("notebook")
    suspend fun createNote(@Body dto: CreateNoteRequest): ApiResponse<NoteData>

    /** PATCH /notebook/:id — partial update (title/content/color/pin) */
    @PATCH("notebook/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body dto: UpdateNoteRequest): ApiResponse<NoteData>

    /** DELETE /notebook/:id */
    @DELETE("notebook/{id}")
    suspend fun deleteNote(@Path("id") id: String): ApiResponse<Unit>
}
