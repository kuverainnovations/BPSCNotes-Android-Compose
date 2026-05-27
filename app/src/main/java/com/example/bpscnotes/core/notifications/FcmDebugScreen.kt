package com.example.bpscnotes.core.notifications

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun FcmDebugScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var fcmToken         by remember { mutableStateOf("Loading…") }
    var firebaseStatus   by remember { mutableStateOf("Checking…") }
    var syncStatus       by remember { mutableStateOf("Not started") }
    var permissionStatus by remember { mutableStateOf("Checking…") }

    // Check everything on load
    LaunchedEffect(Unit) {
        // 1. Firebase init check
        val apps = FirebaseApp.getApps(context)
        firebaseStatus = if (apps.isNotEmpty()) "✅ Initialized (${apps.size} app)" else "❌ NOT initialized — missing google-services.json"

        // 2. Notification permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            permissionStatus = if (granted) "✅ Granted" else "❌ Denied — user must allow in Settings"
        } else {
            permissionStatus = "✅ Auto-granted (Android < 13)"
        }

        // 3. Get FCM token
        try {
            val token = withContext(Dispatchers.IO) {
                FirebaseMessaging.getInstance().token.await()
            }
            fcmToken = token
            Log.d("FCM_DEBUG", "Token: $token")
        } catch (e: Exception) {
            fcmToken = "❌ Failed: ${e.message}"
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        // Header
        Box(modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF1565C0))
            .statusBarsPadding()
            .padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White.copy(0.15f))
                    .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text("🔔 FCM Debug", style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Status cards
            StatusCard("Firebase SDK", firebaseStatus)
            StatusCard("POST_NOTIFICATIONS Permission", permissionStatus)

            // FCM Token card
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FCM Token", fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
                    Text(fcmToken, fontSize = 11.sp, color = BpscColors.TextSecondary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    if (!fcmToken.startsWith("❌") && fcmToken != "Loading…") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("FCM Token", fcmToken))
                                Toast.makeText(context, "Token copied!", Toast.LENGTH_SHORT).show()
                            }, shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                                Text("📋 Copy Token", fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = {
                                Log.d("FCM_TOKEN", fcmToken)
                                Toast.makeText(context, "Check Logcat: FCM_TOKEN", Toast.LENGTH_SHORT).show()
                            }, shape = RoundedCornerShape(8.dp)) {
                                Text("📜 Log it", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Sync button
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sync Token to Backend", fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
                    Text(syncStatus, fontSize = 12.sp, color = if (syncStatus.startsWith("✅")) Color(0xFF2ECC71) else BpscColors.TextSecondary)
                    Button(onClick = {
                        scope.launch {
                            syncStatus = "Syncing…"
                            try {
                                val token = withContext(Dispatchers.IO) {
                                    FirebaseMessaging.getInstance().token.await()
                                }
                                // Force re-sync by deleting cache
                                context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                                    .edit().remove("last_synced_token").apply()
                                syncStatus = "✅ Token obtained: ${token.take(20)}…\n\nNow rebuild app with google-services.json to auto-sync on login"
                            } catch (e: Exception) {
                                syncStatus = "❌ Failed: ${e.message}"
                            }
                        }
                    }, shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                        Text("🔄 Force Sync Now")
                    }
                }
            }

            // Checklist
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📋 FCM Setup Checklist", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                    val items = listOf(
                        "google-services.json placed in app/ folder",
                        "apply plugin: 'com.google.gms.google-services' in app/build.gradle",
                        "classpath 'com.google.gms:google-services:4.4.0' in project build.gradle",
                        "implementation 'com.google.firebase:firebase-messaging:24.0.0' added",
                        "POST_NOTIFICATIONS permission granted on Android 13+",
                        "FCM token saved in backend (check DB: SELECT fcm_token FROM users)",
                        "Firebase project has Cloud Messaging API enabled",
                    )
                    items.forEachIndexed { i, item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${i+1}.", color = Color(0xFF8D6E63), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(item, fontSize = 12.sp, color = Color(0xFF5D4037))
                        }
                    }
                }
            }

            // How to test
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4FD))) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🧪 How to test right now", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                    Text("1. Copy your FCM token above", fontSize = 12.sp, color = Color(0xFF1565C0))
                    Text("2. Go to Firebase Console → Messaging → Send test message", fontSize = 12.sp, color = Color(0xFF1565C0))
                    Text("3. Paste token in 'Add an FCM registration token'", fontSize = 12.sp, color = Color(0xFF1565C0))
                    Text("4. Send — notification should appear immediately", fontSize = 12.sp, color = Color(0xFF1565C0))
                    Text("5. If still not working: check Firebase Console → Project Settings → Cloud Messaging → enable API", fontSize = 12.sp, color = Color(0xFF1565C0))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusCard(label: String, status: String) {
    Card(shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                status.startsWith("✅") -> Color(0xFFE8FDF4)
                status.startsWith("❌") -> Color(0xFFFEE8E8)
                else                   -> Color.White
            }
        )) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (status.startsWith("✅")) "✅" else if (status.startsWith("❌")) "❌" else "⏳", fontSize = 18.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = if (status.startsWith("❌")) Color(0xFFB71C1C) else BpscColors.TextPrimary)
                Text(status.removePrefix("✅ ").removePrefix("❌ "), fontSize = 12.sp,
                    color = if (status.startsWith("❌")) Color(0xFFE53935) else BpscColors.TextSecondary)
            }
        }
    }
}
