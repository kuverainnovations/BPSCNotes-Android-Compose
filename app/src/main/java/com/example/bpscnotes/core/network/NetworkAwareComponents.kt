package com.example.bpscnotes.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

// ── Check current network state synchronously ─────────────────
fun isNetworkCurrentlyAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return true
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        cm.activeNetworkInfo?.isConnected == true
    }
}

// ── Network state flow ────────────────────────────────────────
fun networkStateFlow(context: Context): Flow<Boolean> = callbackFlow {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(true) }
        override fun onLost(network: Network)      { trySend(false) }
        override fun onUnavailable()               { trySend(false) }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    cm.registerNetworkCallback(request, callback)
    // Emit actual current state first
    trySend(isNetworkCurrentlyAvailable(context))
    awaitClose { cm.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

// ── Offline banner ────────────────────────────────────────────
// Shows ONLY after 2 seconds — prevents false "offline" flash on screen load.
// Safe to add at top of any screen Column.
@Composable
fun OfflineBanner() {
    val context = LocalContext.current

    // Start with current actual state — no false positive
    val initialOnline = remember { isNetworkCurrentlyAvailable(context) }
    var isOnline by remember { mutableStateOf(initialOnline) }
    var ready    by remember { mutableStateOf(false) }  // only show after delay

    // Wait 2s before activating banner — avoids showing on app startup
    LaunchedEffect(Unit) {
        delay(2000)
        ready = true
    }

    // Listen to network changes
    LaunchedEffect(Unit) {
        networkStateFlow(context).collect { online ->
            isOnline = online
        }
    }

    var showBackOnline by remember { mutableStateOf(false) }
    var wasOffline     by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        if (!isOnline && ready) {
            wasOffline = true
        } else if (isOnline && wasOffline) {
            showBackOnline = true
            delay(2500)
            showBackOnline = false
            wasOffline = false
        }
    }

    // Don't show anything until ready (2s delay passed)
    if (!ready) return

    AnimatedVisibility(
        visible = !isOnline || showBackOnline,
        enter   = slideInVertically() + fadeIn(),
        exit    = slideOutVertically() + fadeOut()
    ) {
        val isBack = isOnline && showBackOnline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isBack) Color(0xFF2E7D32) else Color(0xFFB71C1C))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isBack) Icons.Rounded.Wifi else Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isBack) "Back online — refreshing..."
                else "No internet — showing saved content",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Composable to observe network state ──────────────────────
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val initial = remember { isNetworkCurrentlyAvailable(context) }
    return networkStateFlow(context).collectAsState(initial = initial).value
}