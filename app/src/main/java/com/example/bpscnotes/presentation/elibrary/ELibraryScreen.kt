package com.example.bpscnotes.presentation.elibrary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.example.bpscnotes.presentation.mylearning.MyLearningScreen
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.readingrooms.ReadingRoomsScreen
import com.example.bpscnotes.presentation.rooms.RoomsHubScreen

@Composable
fun ELibraryScreen(navController: NavHostController) {
    LaunchedEffect(Unit) {
        navController.navigate(Screen.RoomsHub.route) {
            launchSingleTop = true
        }
    }
}
