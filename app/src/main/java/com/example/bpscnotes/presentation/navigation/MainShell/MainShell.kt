package com.example.bpscnotes.presentation.navigation.MainShell

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.Person
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.bpscnotes.presentation.rooms.StudyRoomPipOverlay
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.presentation.rooms.TierRoomsViewModel

@Composable
fun MainShell(
    rootNavController: NavHostController,
    sessionViewModel:  StudySessionViewModel,
    tiersViewModel:    TierRoomsViewModel,
              adManager: AdManager/*, tokenStore: com.example.bpscnotes.data.local.TokenStore*/) {
    val bottomNavController = rememberNavController()

    // Listen for tab switch requests from other screens (e.g. CoinWallet → "go to RoomsHub")
    // Screens navigate to Screen.Main with savedStateHandle["tab"] = route
    val mainBackStackEntry = rootNavController.currentBackStackEntry
    val requestedTab = mainBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("tab", null)
        ?.collectAsState()
    LaunchedEffect(requestedTab?.value) {
        val tab = requestedTab?.value
        if (!tab.isNullOrBlank()) {
            bottomNavController.navigate(tab) {
                popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState    = true
            }
            mainBackStackEntry?.savedStateHandle?.set("tab", null) // clear after use
        }
    }

    val str   = LocalStrings.current
    val items = listOf(
        BottomNavItem(route = Screen.Dashboard.route,  label = str.navDashboard,   icon = Icons.Rounded.Home,                       badgeCount = 0),
        BottomNavItem(route = Screen.MyLearning.route, label = str.navMyLearning,  icon = Icons.AutoMirrored.Rounded.MenuBook,      badgeCount = 0),
        BottomNavItem(route = Screen.RoomsHub.route,   label = str.navRooms,       icon = Icons.Rounded.LocalLibrary,               badgeCount = 0),
        BottomNavItem(route = Screen.Profile.route,    label = str.navProfile,     icon = Icons.Rounded.Person,                     badgeCount = 0),
    )

    // Use the ViewModel passed from BpscNavHost — correctly scoped to Screen.Main entry.
    // This is the SAME instance used by StudyFocusScreen so elapsedSeconds is shared.
    val sessionStateForPip by sessionViewModel.uiState.collectAsState()

    // Track if user is currently inside StudyFocusScreen
    val currentRoute by rootNavController.currentBackStackEntryFlow
        .collectAsState(initial = rootNavController.currentBackStackEntry)
    val isOnStudyFocus = currentRoute?.destination?.route == Screen.StudyFocus.route

    val sessionIsActive = sessionStateForPip.status == com.example.bpscnotes.presentation.rooms.SessionStatus.ACTIVE ||
                          sessionStateForPip.status == com.example.bpscnotes.presentation.rooms.SessionStatus.AFK
    val showPip = sessionIsActive && !isOnStudyFocus

    Scaffold(
        bottomBar = {
            BpscBottomNav(
                navController = bottomNavController,
                items         = items
            )
        }
    ) { innerPadding ->
        Box(Modifier.consumeWindowInsets(innerPadding)) {
            NavHost(
                navController    = bottomNavController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.fillMaxSize()
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

            // ── STUDY ROOM PIP OVERLAY ─────────────────────────────────
            // Floating mini-session card when user navigates away from the room.
            // Session keeps running and earning coins in the background.
            StudyRoomPipOverlay(
                isVisible    = showPip,
                tierName     = sessionStateForPip.tierName ?: str.roomsTitle,
                tierEmoji    = sessionStateForPip.tierEmoji ?: "📚",
                tierColorHex = sessionStateForPip.tierColorHex,
                startedAt    = sessionStateForPip.startedAt,
                coinsEarned  = sessionStateForPip.coinsThisSession,
                onReturn     = {
                    rootNavController.navigate(Screen.StudyFocus.route) {
                        launchSingleTop = true
                    }
                },
                onEndSession = {
                    sessionViewModel.endSession()
                }
            )
        }
    }
}