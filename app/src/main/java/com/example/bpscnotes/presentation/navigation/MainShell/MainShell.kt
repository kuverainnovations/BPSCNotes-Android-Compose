package com.example.bpscnotes.presentation.navigation.MainShell

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.Person
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import dagger.hilt.android.EntryPointAccessors
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.example.bpscnotes.core.permissions.NotificationPermissionEffect
import com.example.bpscnotes.presentation.rooms.TierRoomsViewModel

@Composable
fun MainShell(
    rootNavController: NavHostController,
    sessionViewModel:  StudySessionViewModel,
    tiersViewModel:    TierRoomsViewModel,
    adManager: AdManager/*, tokenStore: com.example.bpscnotes.data.local.TokenStore*/) {
    val str = LocalStrings.current
    val bottomNavController = rememberNavController()

    val innerEntry by bottomNavController.currentBackStackEntryAsState()

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

    // ── Runtime permissions ─────────────────────────────────
    // Ask for notification permission once after the user lands on the home tabs.
    // The effect handles Android version checks, rationale dialog, and permanent denial.
    NotificationPermissionEffect()

    // Notification badge count — read from shared prefs (set by DashboardViewModel after fetch)
    val context = androidx.compose.ui.platform.LocalContext.current
    val notifCount by androidx.compose.runtime.produceState(initialValue = 0, key1 = Unit) {
        // Check shared prefs for unread notification count cached by DashboardViewModel
        val prefs = context.getSharedPreferences("bpsc_prefs", android.content.Context.MODE_PRIVATE)
        value = prefs.getInt("unread_notifications", 0)
    }
    val items = listOf(
        BottomNavItem(route = Screen.Dashboard.route,  label = str.navDashboard,   icon = Icons.Rounded.Home,                       badgeCount = notifCount),
        BottomNavItem(route = Screen.MyLearning.route, label = str.navMyLearning,  icon = Icons.AutoMirrored.Rounded.MenuBook,      badgeCount = 0),
        BottomNavItem(route = Screen.RoomsHub.route,   label = str.navRooms,       icon = Icons.Rounded.LocalLibrary,               badgeCount = 0),
        BottomNavItem(route = Screen.Profile.route,    label = str.navProfile,     icon = Icons.Rounded.Person,                     badgeCount = 0),
    )

    // Use the ViewModel passed from BpscNavHost — correctly scoped to Screen.Main entry.
    // This is the SAME instance used by StudyFocusScreen so elapsedSeconds is shared.
    val sessionStateForPip by sessionViewModel.uiState.collectAsState()

    // Track if user is currently inside StudyFocusScreen
    val currentRoute by rootNavController.currentBackStackEntryAsState()
    val isOnStudyFocus = try {
        currentRoute?.destination?.route == Screen.StudyFocus.route
    } catch (_: Exception) { false }

    val sessionIsActive = sessionStateForPip.status == com.example.bpscnotes.presentation.rooms.SessionStatus.ACTIVE ||
            sessionStateForPip.status == com.example.bpscnotes.presentation.rooms.SessionStatus.AFK
    val showPip = sessionIsActive && !isOnStudyFocus

    // ── Double-back-press to exit ───────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }
    val backPressedOnce   = remember { androidx.compose.runtime.mutableStateOf(false) }
    val scope             = androidx.compose.runtime.rememberCoroutineScope()

    val activity = context as? Activity

    BackHandler(enabled = true) {
        val currentBottomRoute = bottomNavController.currentDestination?.route

        // Non-Dashboard tab → go home first
        if (currentBottomRoute != null && currentBottomRoute != Screen.Dashboard.route) {
            bottomNavController.navigate(Screen.Dashboard.route) {
                popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState    = true
            }
            return@BackHandler
        }

        // Already on Dashboard: double-press to exit
        if (backPressedOnce.value) {
            backPressedOnce.value = false      // reset before leaving
            activity?.moveTaskToBack(true)

        } else {
            backPressedOnce.value = true
            scope.launch {
                snackbarHostState.showSnackbar(
                    message  = "Press back again to exit",
                    duration = SnackbarDuration.Short
                )
                kotlinx.coroutines.delay(2000)
                backPressedOnce.value = false
            }
        }
    }

    val scaffoldBg = MaterialTheme.colorScheme.background
    Scaffold(
        containerColor = scaffoldBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BpscBottomNav(
                navController = bottomNavController,
                items         = items
            )
        }
    ) { innerPadding ->
        val cs = MaterialTheme.colorScheme
        Box(
            Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // ── Hide NavHost until inner nav is ready (eliminates white flash) ──
            // innerEntry is NULL for ~50ms when MainShell re-enters composition
            // (bottomNavController exists but NavHost hasn't rendered startDestination yet).
            // We keep NavHost in the tree so it can register routes, but cover it with
            // an opaque background overlay until the first route is settled.
            NavHost(
                navController    = bottomNavController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    // Invisible while no route is active — avoids white flash
                    alpha = if (innerEntry != null) 1f else 0f
                }
            ) {
                composable(Screen.Dashboard.route)  { DashboardScreen(rootNavController, adManager = adManager) }
                composable(Screen.MyLearning.route) { MyLearningScreen(rootNavController, fromScreen = "main-shell") }
                //composable(Screen.ELibrary.route)   { ELibraryScreen(rootNavController) }
                composable(Screen.RoomsHub.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        try { rootNavController.getBackStackEntry(Screen.Main.route) }
                        catch (_: Exception) { backStackEntry }
                    }

                    val sessionVM: StudySessionViewModel = hiltViewModel(parentEntry)
                    val tiersVM: TierRoomsViewModel = hiltViewModel(parentEntry)

                    RoomsHubScreen(
                        navController    = rootNavController,
                        sessionViewModel = sessionVM,
                        tiersViewModel   = tiersVM,
                        adManager        = adManager
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