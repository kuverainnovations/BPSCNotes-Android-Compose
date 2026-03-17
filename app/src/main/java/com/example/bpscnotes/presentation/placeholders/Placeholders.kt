package com.example.bpscnotes.presentation.placeholders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$title\n(Coming Soon)", style = MaterialTheme.typography.headlineSmall)
    }
}

// ✅ Still placeholders — not built yet
@Composable fun SubscriptionScreen(nav: NavHostController)= PlaceholderScreen("Subscription")
@Composable fun DownloadsScreen(nav: NavHostController)   = PlaceholderScreen("Downloads")
@Composable fun CourseDetailScreen(nav: NavHostController, courseId: String) = PlaceholderScreen("Course Detail")
@Composable fun NotesReaderScreen(nav: NavHostController, noteId: String)    = PlaceholderScreen("Notes Reader")
