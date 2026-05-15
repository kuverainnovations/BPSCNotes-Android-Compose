package com.example.bpscnotes.presentation.elibrary

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.bpscnotes.presentation.mylearning.MyLearningScreen
import com.example.bpscnotes.presentation.readingrooms.ReadingRoomsScreen
import com.example.bpscnotes.presentation.rooms.RoomsHubScreen

@Composable
fun ELibraryScreen(navController: NavHostController) {
    RoomsHubScreen(
        navController = navController,
    )
}
