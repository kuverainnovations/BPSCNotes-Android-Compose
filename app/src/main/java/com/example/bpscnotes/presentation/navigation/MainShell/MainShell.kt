package com.example.bpscnotes.presentation.navigation.MainShell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bpscnotes.presentation.dashboard.DashboardScreen
import com.example.bpscnotes.presentation.mylearning.MyLearningScreen
import com.example.bpscnotes.presentation.navigation.BottomNavItem
import com.example.bpscnotes.presentation.navigation.BpscBottomNav
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.elibrary.ELibraryScreen
import com.example.bpscnotes.presentation.profile.ProfileScreen

@Composable
fun MainShell(rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()

    val items = listOf(
        BottomNavItem(
            route      = Screen.Dashboard.route,
            label      = "Dashboard",
            icon       = Icons.Rounded.Home,
            badgeCount = 0
        ),
        BottomNavItem(
            route      = Screen.MyLearning.route,
            label      = "My Learning",
            icon       = Icons.AutoMirrored.Rounded.MenuBook,
            badgeCount = 0
        ),
        BottomNavItem(
            route      = Screen.ELibrary.route,
            label      = "E-Library",
            icon       = Icons.Rounded.LocalLibrary,
            badgeCount = 0
        ),
        BottomNavItem(
            route      = Screen.Profile.route,
            label      = "Profile",
            icon       = Icons.Rounded.Person,
            badgeCount = 0
        ),
    )

    Scaffold(
        bottomBar = {
            BpscBottomNav(
                navController = bottomNavController,
                items         = items
            )
        }
    ) { innerPadding ->
        NavHost(
            navController    = bottomNavController,
            startDestination = Screen.Dashboard.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route)  { DashboardScreen(rootNavController) }
            composable(Screen.MyLearning.route) { MyLearningScreen(rootNavController) }
            composable(Screen.ELibrary.route)   { ELibraryScreen(rootNavController) }
            composable(Screen.Profile.route)    { ProfileScreen(rootNavController) }
        }
    }
}