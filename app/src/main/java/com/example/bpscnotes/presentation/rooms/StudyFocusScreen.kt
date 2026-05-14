package com.example.bpscnotes.presentation.rooms

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.EndSessionResponseData
import com.example.bpscnotes.data.remote.api.MyTierResponseData
import com.example.bpscnotes.data.remote.api.TierMemberDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// StudyFocusScreen — Inside the Study Room
//
// Layout (single Column, no weight conflicts):
//  ┌──────────────────────────────────────────┐
//  │ TopBar: ← Back · Room name · Focus badge │
//  ├────────────────┬─────────────────────────┤
//  │  MY PROFILE    │   TIMER RING            │
//  │  Avatar+name   │   01:53  active         │
//  │  Lv2 · Coins   │   stats strip           │
//  ├────────────────┴─────────────────────────┤
//  │  STUDYING NOW  (filtered live only)       │
//  │  Vertical compact list · handles 1000+   │
//  │  Tap → chat sheet                        │
//  ├──────────────────────────────────────────┤
//  │  [⏹ End Session]                        │
//  └──────────────────────────────────────────┘
// ════════════════════════════════════════════════════════════

@Composable
fun StudyFocusScreen(
    navController:  NavHostController,
    viewModel:      StudySessionViewModel = hiltViewModel(),
    tiersViewModel: TierRoomsViewModel    = hiltViewModel()
) {
    val state      by viewModel.uiState.collectAsState()
    val tiersState by tiersViewModel.uiState.collectAsState()

    var showEndConfirm by remember { mutableStateOf(false) }
    var chatWithMember by remember { mutableStateOf<TierMemberDto?>(null) }

    androidx.activity.compose.BackHandler(
        enabled = state.status == SessionStatus.ACTIVE || state.status == SessionStatus.AFK
    ) { showEndConfirm = true }

    // End confirm dialog
    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            icon  = { Text("⏱️", fontSize = 28.sp) },
            title = { Text("End Session?", fontWeight = FontWeight.ExtraBold) },
            text  = { Text("Your ${state.activeMinutes} min will be saved and coins awarded.") },
            confirmButton = {
                Button(
                    onClick = { showEndConfirm = false; viewModel.endSession() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("End Session") }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) {
                    Text("Keep Studying", color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Chat sheet
    chatWithMember?.let { member ->
        ChatSheet(member = member, onDismiss = { chatWithMember = null })
    }

    when (state.status) {
        SessionStatus.STARTING -> StartingScreen()
        SessionStatus.ENDED    -> SessionSummaryScreen(
            summary   = state.summary,
            tierData  = tiersState.myTierData,
            onDismiss = { viewModel.clearSession(); navController.popBackStack() }
        )
        else -> ActiveRoomScreen(
            state        = state,
            tiersState   = tiersState,
            onBack       = { showEndConfirm = true },
            onEnd        = { showEndConfirm = true },
            onMemberTap  = { chatWithMember = it },
            tiersViewModel=tiersViewModel
        )
    }
}

// ════════════════════════════════════════════════════════════
// STARTING SCREEN
// ════════════════════════════════════════════════════════════
@Composable
private fun StartingScreen() {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF051D56), Color(0xFF1565C0)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
            Text("Joining room…", style = MaterialTheme.typography.titleMedium,
                color = Color.White, fontWeight = FontWeight.Bold)
            Text("Setting up your session", style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.6f))
        }
    }
}

// ════════════════════════════════════════════════════════════
// ACTIVE ROOM SCREEN — main study UI
// ════════════════════════════════════════════════════════════
@Composable
private fun ActiveRoomScreen(
    state:       StudySessionUiState,
    tiersState:  TierRoomsUiState,
    onBack:      () -> Unit,
    onEnd:       () -> Unit,
    onMemberTap: (TierMemberDto) -> Unit,
    tiersViewModel: TierRoomsViewModel
) {
    // Local timer — increments every second
    var elapsedSeconds by remember { mutableIntStateOf(state.activeMinutes * 60) }
    LaunchedEffect(state.status, "timer") {
        while (state.status == SessionStatus.ACTIVE || state.status == SessionStatus.AFK) {
            delay(1000L); elapsedSeconds++
        }
    }

    // Refresh members list every 30 seconds as a safety net.
    // Handles cases where the WebSocket presence event is missed (poor network).
    // This ensures Balu's "🟢 live" disappears within 30s of him leaving.
    LaunchedEffect(state.status, "member_refresh") {
        while (state.status == SessionStatus.ACTIVE || state.status == SessionStatus.AFK) {
            delay(30_000L)
            val tierKey = tiersState.selectedTierKey
            if (tierKey.isNotEmpty()) {
                tiersViewModel.loadMembers(tierKey)
            }
        }
    }
    val h = elapsedSeconds / 3600
    val m = (elapsedSeconds % 3600) / 60
    val s = elapsedSeconds % 60
    val timeStr = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)

    val ringProgress by animateFloatAsState(
        (elapsedSeconds % 3600) / 3600f, tween(1000), label = "ring"
    )

    // Tier colour
    val tierColor = remember(state.tierColorHex) {
        try { Color(android.graphics.Color.parseColor(state.tierColorHex ?: "#9E9E9E")) }
        catch (e: Exception) { Color(0xFF1565C0) }
    }

    // Filter members: live only + exclude current user (matched by name from tiersState)
    val myName    = tiersState.myTierData?.userStats?.let { "" } ?: ""
    val liveMembers = tiersState.members.filter { it.isStudyingNow }

    // Full dark background — unified, no mismatch
    val bgGradient = Brush.verticalGradient(
        listOf(Color(0xFF030D2E), Color(0xFF051D56), Color(0xFF071E3D))
    )

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // ── TOP BAR ──────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.tierEmoji ?: "🥈"} ${state.tierName ?: "Silver Room"}",
                        style = MaterialTheme.typography.titleMedium, color = Color.White,
                        fontWeight = FontWeight.ExtraBold)
                    if (liveMembers.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(BpscColors.Success))
                            Text("${liveMembers.size} studying now",
                                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.65f))
                        }
                    }
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(0.12f)).padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("📚 Focus", style = MaterialTheme.typography.labelSmall,
                        color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // ── AFK BANNER ─────────────────────────────────────
            if (state.status == SessionStatus.AFK) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Row(modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AFK Detected", style = MaterialTheme.typography.labelMedium,
                                color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Idle time not counted toward coins",
                                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                        }
                        TextButton(onClick = {}) {
                            Text("I'm Back", color = Color.White, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // ── PROFILE + TIMER SECTION ───────────────────────
            // My profile card (left 45%) + Timer ring (right 55%)
            Row(
                modifier              = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // ── MY PROFILE CARD ─────────────────────────
                Card(
                    modifier = Modifier.weight(0.45f),
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color.White.copy(0.08f)),
                    border   = BorderStroke(1.dp, tierColor.copy(0.3f))
                ) {
                    Column(
                        modifier              = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        // Avatar with level ring
                        Box(modifier = Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                val stroke = 3.dp.toPx()
                                drawArc(tierColor.copy(0.3f), -90f, 360f, false,
                                    style = Stroke(stroke))
                                drawArc(tierColor, -90f, ringProgress * 360f, false,
                                    style = Stroke(stroke, cap = StrokeCap.Round))
                            }
                            Box(
                                modifier = Modifier.size(50.dp).clip(CircleShape)
                                    .background(tierColor.copy(0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (tiersState.myTierData?.userStats?.let { "Y" }
                                        ?: (state.tierName?.firstOrNull()?.toString() ?: "Y")),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold
                                )
                            }
                            // Online dot
                            Box(
                                modifier = Modifier.size(14.dp).align(Alignment.BottomEnd)
                                    .clip(CircleShape).background(Color(0xFF030D2E)).padding(2.dp)
                            ) { Box(Modifier.fillMaxSize().clip(CircleShape).background(BpscColors.Success)) }
                        }

                        Text("You", style = MaterialTheme.typography.labelMedium,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)

                        // Level badge
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(tierColor.copy(0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Lv${tiersState.myTierData?.userStats?.xpLevel ?: 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tierColor, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = Color.White.copy(0.1f))

                        // Session coins earned
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🪙 +${state.coinsThisSession}",
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                            Text("this session", style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.5f), fontSize = 9.sp)
                        }

                        // XP earned
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 10.sp)
                            Text("+${state.xpThisSession} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64B5F6), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // ── TIMER RING ──────────────────────────────
                Column(
                    modifier            = Modifier.weight(0.55f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            val stroke = 8.dp.toPx()
                            val inset  = stroke / 2
                            val sz     = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                            drawArc(Color.White.copy(0.12f), -90f, 360f, false,
                                style = Stroke(stroke), topLeft = Offset(inset, inset), size = sz)
                            drawArc(
                                brush = Brush.sweepGradient(listOf(tierColor.copy(0.6f), Color.White)),
                                startAngle = -90f, sweepAngle = ringProgress * 360f,
                                useCenter = false,
                                style = Stroke(stroke, cap = StrokeCap.Round),
                                topLeft = Offset(inset, inset), size = sz
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(timeStr, style = MaterialTheme.typography.headlineMedium,
                                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                            Text("active", style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.55f))
                            Spacer(Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(CircleShape)
                                    .background(if (state.wasAfkLastBeat) Color(0xFFFF6B35) else BpscColors.Success))
                                Text(if (state.wasAfkLastBeat) "AFK" else "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (state.wasAfkLastBeat) Color(0xFFFF6B35) else BpscColors.Success,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Last beat badge
                    if (state.coinsLastBeat > 0 || state.xpLastBeat > 0) {
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(0.1f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.coinsLastBeat > 0)
                                Text("🪙+${state.coinsLastBeat}", style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
                            if (state.xpLastBeat > 0)
                                Text("⚡+${state.xpLastBeat}", style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold)
                        }
                    }

                    // AFK count chip
                    if (state.afkCount > 0) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFF6B35).copy(0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("💤 ${state.afkCount} AFK", style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF6B35))
                        }
                    }
                }
            }

            // ── DIVIDER ────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp)
                .background(Color.White.copy(0.08f)))

            // ── STUDYING NOW SECTION ──────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(BpscColors.Success))
                    Text("Studying Now", style = MaterialTheme.typography.titleSmall,
                        color = Color.White, fontWeight = FontWeight.ExtraBold)
                    if (liveMembers.isNotEmpty()) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(BpscColors.Success.copy(0.15f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text("${liveMembers.size}", style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
                Text("Tap to message", style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.4f))
            }

            // ── MEMBERS LIST — vertical, handles 1000+ ────────
            if (liveMembers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("👋", fontSize = 32.sp)
                        Text("You're the first one here!", style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.5f))
                        Text("Others will appear when they join", style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.3f))
                    }
                }
            } else {
                LazyColumn(
                    modifier            = Modifier.weight(1f),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Show max 50 live members. Backend should page large rooms.
                    items(liveMembers.take(50), key = { it.id }) { member ->
                        LiveMemberRow(member = member, onClick = { onMemberTap(member) })
                    }
                    if (liveMembers.size > 50) {
                        item(key = "more") {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center) {
                                Text("+ ${liveMembers.size - 50} more studying",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.4f))
                            }
                        }
                    }
                }
            }

            // ── END SESSION BUTTON ────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF030D2E))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick  = onEnd,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    enabled  = state.status != SessionStatus.ENDING,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Color(0xFF1A1F3A),
                        contentColor           = Color.White,
                        disabledContainerColor = Color(0xFF0D1020),
                        disabledContentColor   = Color.White.copy(0.3f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(0.12f))
                ) {
                    if (state.status == SessionStatus.ENDING) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Saving session…", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Rounded.StopCircle, null, modifier = Modifier.size(18.dp), tint = Color(0xFFEF5350))
                        Spacer(Modifier.width(8.dp))
                        Text("End Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// LIVE MEMBER ROW — compact, vertical list, handles scale
// ════════════════════════════════════════════════════════════
@Composable
private fun LiveMemberRow(member: TierMemberDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White.copy(0.06f)),
        border   = BorderStroke(1.dp, Color.White.copy(0.07f))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Avatar with live ring
            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape)
                    .background(BpscColors.Primary.copy(0.2f)), contentAlignment = Alignment.Center) {
                    Text(member.name.first().uppercaseChar().toString(),
                        style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Box(modifier = Modifier.size(12.dp).align(Alignment.BottomEnd)
                    .clip(CircleShape).background(Color(0xFF030D2E)).padding(2.dp)) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(BpscColors.Success))
                }
            }

            // Name + stats
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(member.name, style = MaterialTheme.typography.titleSmall,
                    color = Color.White, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Lv${member.xpLevel}", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.45f), fontSize = 10.sp)
                    Text("🔥${member.streak}", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.45f), fontSize = 10.sp)
                    Text("${member.totalStudyMinutes / 60}h total",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.45f), fontSize = 10.sp)
                }
            }

            // Message icon
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(BpscColors.Primary.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Send, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// CHAT SHEET
// ════════════════════════════════════════════════════════════
data class ChatMessage(
    val id: String, val text: String, val isMe: Boolean,
    val senderName: String, val timeLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSheet(member: TierMemberDto, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputText  by remember { mutableStateOf("") }
    val messages   = remember {
        mutableStateListOf(
            ChatMessage("1", "Hey! Are you studying the same topic? 📚", false, member.name.split(" ").first(), "Just now")
        )
    }
    val listState  = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun sendMessage() {
        if (inputText.isNotBlank()) {
            messages.add(ChatMessage(System.currentTimeMillis().toString(),
                inputText.trim(), true, "You", "Now"))
            inputText = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color(0xFF0D1628),
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(480.dp)) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(BpscColors.Primary.copy(0.3f)),
                    contentAlignment = Alignment.Center) {
                    Text(member.name.first().uppercaseChar().toString(),
                        style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(member.name, style = MaterialTheme.typography.titleSmall,
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(BpscColors.Success))
                        Text("Studying · Lv${member.xpLevel} · ${member.totalStudyMinutes/60}h total",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.55f))
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.6f))
                }
            }
            HorizontalDivider(color = Color.White.copy(0.07f))

            // Messages
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                state = listState, contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages, key = { it.id }) { msg -> ChatBubble(message = msg) }
            }

            HorizontalDivider(color = Color.White.copy(0.07f))

            // Input row
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically) {
                OutlinedTextField(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text("Message…", style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.3f)) },
                    shape         = RoundedCornerShape(24.dp),
                    maxLines      = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BpscColors.Primary,
                        unfocusedBorderColor = Color.White.copy(0.15f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = BpscColors.Primary
                    )
                )
                Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(if (inputText.isNotBlank()) BpscColors.Primary else Color.White.copy(0.1f))
                    .clickable(enabled = inputText.isNotBlank()) { sendMessage() },
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Send, null,
                        tint = if (inputText.isNotBlank()) Color.White else Color.White.copy(0.3f),
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start) {
        if (!message.isMe) {
            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(BpscColors.Primary.copy(0.3f)),
                contentAlignment = Alignment.Center) {
                Text(message.senderName.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
        }
        Column(modifier = Modifier.widthIn(max = 230.dp),
            horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start) {
            Box(modifier = Modifier.clip(RoundedCornerShape(
                topStart = if (message.isMe) 16.dp else 4.dp,
                topEnd   = if (message.isMe) 4.dp else 16.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(if (message.isMe) BpscColors.Primary else Color.White.copy(0.1f))
                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White)
            }
            Text(message.timeLabel, style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.3f), fontSize = 9.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════
// SESSION SUMMARY SCREEN
// ════════════════════════════════════════════════════════════
@Composable
private fun SessionSummaryScreen(
    summary:   EndSessionResponseData?,
    tierData:  MyTierResponseData?,
    onDismiss: () -> Unit
) {
    val animProg by animateFloatAsState(
        if (summary != null && summary.durationMinutes > 0)
            (summary.activeMinutes.toFloat() / summary.durationMinutes).coerceIn(0f, 1f) else 0f,
        tween(1200), label = "sum")

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF030D2E), Color(0xFF051D56), BpscColors.Surface))),
        contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Text("✅", fontSize = 60.sp)
            Spacer(Modifier.height(8.dp))
            Text("Session Complete!", style = MaterialTheme.typography.headlineMedium,
                color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(summary?.message ?: "Great work!",
                style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(Modifier.height(24.dp))

            // Arc ring
            Box(Modifier.size(100.dp), Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx(); val inset = stroke / 2
                    val sz = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(0.12f), -90f, 360f, false,
                        style = Stroke(stroke), topLeft = Offset(inset, inset), size = sz)
                    drawArc(Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)), -90f,
                        animProg * 360f, false, style = Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset), size = sz)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${summary?.activeMinutes ?: 0}m",
                        style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("active", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
                }
            }
            Spacer(Modifier.height(20.dp))

            // Stats grid
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SumStat("⏱️", "${summary?.durationMinutes ?: 0}m", "Total")
                    SumStat("🎯", "${summary?.activeMinutes ?: 0}m", "Active")
                    SumStat("🪙", "+${summary?.totalCoins ?: 0}", "Coins")
                    SumStat("⚡", "+${summary?.totalXp ?: 0}", "XP")
                }
            }

            if ((summary?.bonusCoins ?: 0) > 0) {
                Spacer(Modifier.height(10.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BpscColors.CoinGold.copy(0.12f)),
                    border = BorderStroke(1.dp, BpscColors.CoinGold.copy(0.3f))) {
                    Row(modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🎉", fontSize = 20.sp)
                        Text("Bonus +${summary?.bonusCoins} coins (30+ min session!)",
                            style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            tierData?.nextTier?.let { next ->
                Spacer(Modifier.height(10.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BpscColors.PrimaryLight)) {
                    Row(modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(next.iconEmoji ?: "🥇", fontSize = 20.sp)
                        Column {
                            Text("Progress to ${next.name}",
                                style = MaterialTheme.typography.titleSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                            Text("${(tierData.nextTierProgress * 100).toInt()}% complete — keep going!",
                                style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text("Back to Rooms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SumStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 18.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
    }
}
