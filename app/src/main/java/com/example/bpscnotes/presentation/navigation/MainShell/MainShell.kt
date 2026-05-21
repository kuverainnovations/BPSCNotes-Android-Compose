package com.example.bpscnotes.presentation.navigation.MainShell

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import dagger.hilt.android.EntryPointAccessors
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.presentation.dashboard.DashboardScreen
import com.example.bpscnotes.presentation.mylearning.MyLearningScreen
import com.example.bpscnotes.presentation.navigation.BottomNavItem
import com.example.bpscnotes.presentation.navigation.BpscBottomNav
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.elibrary.ELibraryScreen
import com.example.bpscnotes.presentation.profile.ProfileScreen
import com.example.bpscnotes.presentation.rooms.RoomsHubScreen
import com.example.bpscnotes.presentation.rooms.StudySessionViewModel
import com.example.bpscnotes.presentation.rooms.TierRoomsViewModel

@Composable
fun MainShell(rootNavController: NavHostController,
              adManager: AdManager/*, tokenStore: com.example.bpscnotes.data.local.TokenStore*/) {
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
            route      = Screen.RoomsHub.route,
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
            modifier = Modifier
                //.padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Dashboard.route)  { DashboardScreen(rootNavController, adManager = adManager) }
            composable(Screen.MyLearning.route) { MyLearningScreen(rootNavController, fromScreen = "main-shell") }
            //composable(Screen.ELibrary.route)   { ELibraryScreen(rootNavController) }
            composable(Screen.RoomsHub.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    rootNavController.getBackStackEntry(Screen.Main.route)
                }

                val sessionVM: StudySessionViewModel = hiltViewModel(parentEntry)
                val tiersVM: TierRoomsViewModel = hiltViewModel(parentEntry)

                RoomsHubScreen(
                    navController = rootNavController,
                    sessionViewModel = sessionVM,
                    tiersViewModel = tiersVM
                )
            }
            composable(Screen.Profile.route)    { ProfileScreen(rootNavController) }
        }
    }
}