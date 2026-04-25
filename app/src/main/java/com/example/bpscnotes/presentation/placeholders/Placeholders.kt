package com.example.bpscnotes.presentation.placeholders

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Course detail DTOs ────────────────────────────────────────



// ── Screen ────────────────────────────────────────────────────





@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$title\n(Coming Soon)", style = MaterialTheme.typography.headlineSmall)
    }
}

// ✅ Still placeholders — not built yet
@Composable fun SubscriptionScreen(nav: NavHostController)= PlaceholderScreen("Subscription")
@Composable fun DownloadsScreen(nav: NavHostController)   = PlaceholderScreen("Downloads")
@Composable fun NotesReaderScreen(nav: NavHostController, noteId: String)    = PlaceholderScreen("Notes Reader")

