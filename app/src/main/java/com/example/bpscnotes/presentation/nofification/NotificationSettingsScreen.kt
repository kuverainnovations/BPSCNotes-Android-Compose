package com.example.bpscnotes.presentation.nofification

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.AppStrings
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.network.NotifCountBus
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.http.*
import javax.inject.Inject
import com.example.bpscnotes.presentation.navigation.popBackStackSafe


// ════════════════════════════════════════════════════════════
// NOTIFICATIONS — fully dynamic
// Was: static mockNotifications() list, no ViewModel
// Now: GET /notifications, POST /notifications/:id/read, POST /notifications/read-all
// ════════════════════════════════════════════════════════════

data class NotificationDto(
    val id:        String,
    val title:     String,
    val body:      String,
    @SerializedName("created_at") val createdAt: String = "",
    val type:      String = "system",
    @SerializedName("is_read") val isRead: Boolean = false
)

data class NotificationsResponseData(
    val notifications: List<NotificationDto> = emptyList(),
    @SerializedName("unread_count") val unreadCount: Int = 0
)

data class MarkReadRequest(val ids: List<String> = emptyList())

interface NotificationsApiService {
    @GET("notifications")
    suspend fun getNotifications(
        @Query("page")  page:  Int = 1,
        @Query("limit") limit: Int = 30
    ): ApiResponse<NotificationsResponseData>

    @POST("notifications/mark-read")
    suspend fun markRead(@Body body: MarkReadRequest): ApiResponse<Any>

    @POST("notifications/mark-read")
    suspend fun markAllRead(@Body body: MarkReadRequest = MarkReadRequest()): ApiResponse<Any>
}

data class NotificationsUiState(
    val notifications: List<NotificationDto> = emptyList(),
    val unreadCount:   Int     = 0,
    val isLoading:     Boolean = true,
    val isRefreshing:  Boolean = false,
    val error:         String? = null,
    val toastMessage:  String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val api: NotificationsApiService
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init { load() }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = !refresh && it.notifications.isEmpty(), isRefreshing = refresh) }
            try {
                val res = api.getNotifications()
                _state.update {
                    it.copy(notifications = res.data?.notifications ?: emptyList(),
                        unreadCount = res.data?.unreadCount ?: 0,
                        isLoading = false, isRefreshing = false, error = null)
                }
            } catch (e: Exception) {
                Log.e("NotifVM", e.message ?: "", e)
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun markRead(id: String) {
        // Optimistic update — mark as read in UI immediately
        _state.update { s ->
            s.copy(
                notifications = s.notifications.map { if (it.id == id) it.copy(isRead = true) else it },
                unreadCount = (s.unreadCount - 1).coerceAtLeast(0)
            )
        }
        // Then sync with server
        viewModelScope.launch {
            try {
                api.markRead(MarkReadRequest(ids = listOf(id)))
            } catch (e: Exception) {
                android.util.Log.e("NotifVM", "markRead failed: ${e.message}", e)
                // Revert optimistic update on failure
                _state.update { s ->
                    s.copy(
                        notifications = s.notifications.map { if (it.id == id) it.copy(isRead = false) else it },
                        unreadCount = s.unreadCount + 1
                    )
                }
            }
        }
    }

    fun markAllRead() {
        NotifCountBus.emit()  // tell Dashboard to refresh badge immediately
        // Optimistic update
        _state.update { s ->
            s.copy(
                notifications = s.notifications.map { it.copy(isRead = true) },
                unreadCount   = 0,
                toastMessage  = "All notifications marked as read ✓"
            )
        }
        viewModelScope.launch {
            try {
                api.markAllRead(MarkReadRequest())
            } catch (e: Exception) {
                android.util.Log.e("NotifVM", "markAllRead failed: ${e.message}", e)
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toastMessage = null) }
    fun refresh()    = load(refresh = true)
}

@Composable
fun NotificationSettingsScreen(
    navController: NavHostController,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val str = LocalStrings.current
    val state        by viewModel.state.collectAsState()
    val snackbarHost  = remember { SnackbarHostState() }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearToast()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }, containerColor = BpscColors.Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Header
            Box(modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                    Offset(0f, 0f), Offset(600f, 300f)))/*.statusBarsPadding()*/) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 46.dp, bottom = 30.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f))
                            .clickable { navController.popBackStackSafe() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(str.notifTitle, style = MaterialTheme.typography.titleLarge,
                                color = Color.White, fontWeight = FontWeight.ExtraBold)
                            if (state.unreadCount > 0)
                                Text("${state.unreadCount} unread", style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(0.7f))
                        }
                    }
                    if (state.unreadCount > 0)
                        TextButton(onClick = viewModel::markAllRead) {
                            Text(str.notifMarkRead, color = Color.White.copy(0.85f),
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                }
                state.error != null && state.notifications.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 40.sp)
                        Text(state.error!!, color = BpscColors.TextSecondary, style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        Button(onClick = viewModel::refresh, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text(str.retry) }
                    }
                }
                state.notifications.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🔔", fontSize = 52.sp)
                        Text(str.notifNone, style = MaterialTheme.typography.titleLarge,
                            color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text(str.notifSettingsHint,
                            style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary,
                            textAlign = TextAlign.Center)
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                    val groups = state.notifications.groupBy { it.createdAt.take(10).ifEmpty { str.notifToday } }
                    groups.forEach { (date, items) ->
                        item(key = "h_$date") {
                            Text(formatDateHeader(date,str), style = MaterialTheme.typography.labelMedium,
                                color = BpscColors.TextSecondary, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        }
                        items(items, key = { it.id }) { notif ->
                            NotifCard(notif,str) { if (!notif.isRead) viewModel.markRead(notif.id) }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                                color = BpscColors.Divider, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotifCard(n: NotificationDto, str: AppStrings, onClick: () -> Unit) {
    val (icon, iconBg, iconTint) = notifStyle(n.type,str)
    Row(modifier = Modifier.fillMaxWidth()
        .background(if (!n.isRead) BpscColors.Primary.copy(0.03f) else Color.White)
        .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(n.title, style = MaterialTheme.typography.titleSmall, color = BpscColors.TextPrimary,
                    fontWeight = if (!n.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (!n.isRead) Box(Modifier.size(8.dp).clip(CircleShape).background(BpscColors.Primary))
            }
            Text(n.body, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(formatRelTime(n.createdAt,str), style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
        }
    }
}

private fun notifStyle(type: String, str: AppStrings): Triple<ImageVector, Color, Color> = when (type) {
    "daily_target" -> Triple(Icons.Rounded.CheckCircle,        Color(0xFFE8F5E9), Color(0xFF2E7D32))
    "quiz"         -> Triple(Icons.Rounded.Quiz,               Color(0xFFE3F2FD), Color(0xFF1565C0))
    "job"          -> Triple(Icons.Rounded.Work,               Color(0xFFF3E5F5), Color(0xFF7B1FA2))
    "streak"       -> Triple(Icons.Rounded.LocalFireDepartment, Color(0xFFFFF3E0), Color(0xFFE65100))
    "rank"         -> Triple(Icons.Rounded.Leaderboard,        Color(0xFFFFF8E1), Color(0xFFFF8F00))
    "coins"        -> Triple(Icons.Rounded.MonetizationOn,     Color(0xFFFFF8E1), Color(0xFFFF8F00))
    "live_class"   -> Triple(Icons.Rounded.LiveTv,             Color(0xFFFCE4EC), Color(0xFFC62828))
    else           -> Triple(Icons.Rounded.Notifications,      Color(0xFFF5F5F5), Color(0xFF616161))
}

private fun formatDateHeader(date: String, str: AppStrings): String {
    val sdf   = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val today = sdf.format(java.util.Date())
    val cal   = java.util.Calendar.getInstance().also { it.add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val yest  = sdf.format(cal.time)
    return when (date) {
        today -> str.notifToday; yest -> str.notifYesterday
        else  -> try {
            java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(sdf.parse(date) ?: return date)
        } catch (e: Exception) { date }
    }
}

private fun formatRelTime(iso: String, str: AppStrings): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val diff = System.currentTimeMillis() - (sdf.parse(iso)?.time ?: return "")
        val mins = diff / 60000L
        when { mins < 1 -> str.notifJustNow; mins < 60 -> "${mins}m ago"; mins < 1440 -> "${mins / 60}h ago"; else -> "${mins / 1440}d ago" }
    } catch (e: Exception) { "" }
}