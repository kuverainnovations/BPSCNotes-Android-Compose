package com.example.bpscnotes.presentation.readingrooms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RoomSubject(val label: String, val emoji: String, val color: Color, val bg: Color) {
    All("All", "📚", Color(0xFF1565C0), Color(0xFFE8F0FD)),
    Polity("Polity", "⚖️", Color(0xFF9B59B6), Color(0xFFF3E8FD)),
    History("History", "🏛️", Color(0xFFE74C3C), Color(0xFFFEE8E8)),
    Geography("Geography", "🗺️", Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
    Economy("Economy", "💰", Color(0xFFE67E22), Color(0xFFFFF0EA)),
    BiharGK("Bihar GK", "🏔️", Color(0xFFF39C12), Color(0xFFFFF8E1)),
    Science("Science", "🔬", Color(0xFF2ECC71), Color(0xFFE8FDF4)),
    Mixed("Mixed", "🎯", Color(0xFF1565C0), Color(0xFFE8F0FD)),
}

enum class RoomType { Public, Private, Official }

data class RoomMember(
    val id: String,
    val name: String,
    val initials: String,
    val color: Color,
    val isActive: Boolean = true,
    val studyMinutes: Int = 0,
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderInitials: String,
    val senderColor: Color,
    val message: String,
    val time: String,
    val isCurrentUser: Boolean = false,
)

data class StudyRoom(
    val id: String,
    val name: String,
    val subject: RoomSubject,
    val type: RoomType,
    val activeUsers: Int,
    val maxUsers: Int,
    val todayFocus: String,
    val streak: Int,
    val adminName: String,
    val joinCode: String? = null,
    val isFeatured: Boolean = false,
    val tags: List<String> = emptyList(),
    val members: List<RoomMember> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val pomodoroMinutes: Int = 25,
)

val mockMembers = listOf(
    RoomMember("m1", "Rahul Kumar", "RK", Color(0xFF1565C0), true, 145),
    RoomMember("m2", "Priya Singh", "PS", Color(0xFF9B59B6), true, 210),
    RoomMember("m3", "Amit Yadav", "AY", Color(0xFF2ECC71), true, 98),
    RoomMember("m4", "Sneha Verma", "SV", Color(0xFFE74C3C), false, 320),
    RoomMember("m5", "Ravi Shankar", "RS", Color(0xFFE67E22), true, 175),
    RoomMember("m6", "Pooja Kumari", "PK", Color(0xFF1ABC9C), false, 260),
)

val mockMessages = listOf(
    ChatMessage(
        "c1",
        "m2",
        "Priya Singh",
        "PS",
        Color(0xFF9B59B6),
        "Anyone done the Article 14 notes?",
        "09:15"
    ),
    ChatMessage(
        "c2",
        "m3",
        "Amit Yadav",
        "AY",
        Color(0xFF2ECC71),
        "Yes! Check the pinned notes in E-Library",
        "09:16"
    ),
    ChatMessage(
        "c3",
        "m1",
        "Rahul Kumar",
        "RK",
        Color(0xFF1565C0),
        "Thanks! Also the 42nd Amendment is important for today's focus",
        "09:18",
        true
    ),
    ChatMessage(
        "c4",
        "m5",
        "Ravi Shankar",
        "RS",
        Color(0xFFE67E22),
        "Pomodoro starting in 2 mins, everyone ready? 🎯",
        "09:20"
    ),
    ChatMessage(
        "c5",
        "m2",
        "Priya Singh",
        "PS",
        Color(0xFF9B59B6),
        "Ready! Let's crush it 💪",
        "09:21"
    ),
)

val mockRooms = listOf(
    StudyRoom(
        "r1", "BPSC 70th Prep — Polity Focus",
        RoomSubject.Polity, RoomType.Official, 24, 50,
        "Fundamental Rights — Articles 12–35", 12,
        "BPSCNotes Official", isFeatured = true,
        tags = listOf("Official", "Beginner Friendly", "Notes Shared"),
        members = mockMembers, messages = mockMessages, pomodoroMinutes = 25
    ),

    StudyRoom(
        "r2", "History Marathon Group",
        RoomSubject.History, RoomType.Public, 18, 30,
        "Mughal Period + Modern India", 7,
        "Amit Yadav", isFeatured = true,
        tags = listOf("Active", "Discussion"),
        members = mockMembers.take(4), messages = mockMessages.take(3)
    ),

    StudyRoom(
        "r3", "Bihar GK Masters",
        RoomSubject.BiharGK, RoomType.Public, 31, 40,
        "Rivers of Bihar + Physical Geography", 21,
        "Priya Singh", isFeatured = true,
        tags = listOf("Popular", "Bihar Specialists"),
        members = mockMembers, messages = mockMessages
    ),

    StudyRoom(
        "r4", "Economy & Current Affairs",
        RoomSubject.Economy, RoomType.Public, 15, 25,
        "RBI Monetary Policy + Budget 2026", 5,
        "Sneha Verma",
        tags = listOf("Current Affairs"),
        members = mockMembers.take(3)
    ),

    StudyRoom(
        "r5", "Silent Study Room 📵",
        RoomSubject.Mixed, RoomType.Public, 42, 100,
        "Individual Study — No Distractions", 30,
        "BPSCNotes Official",
        tags = listOf("No Chat", "Focus Mode", "Pomodoro"),
        members = mockMembers, pomodoroMinutes = 50
    ),

    StudyRoom(
        "r6", "Geography Deep Dive",
        RoomSubject.Geography, RoomType.Public, 9, 20,
        "Indian Rivers + Mountain Systems", 3,
        "Ravi Shankar",
        tags = listOf("Detailed Notes"),
        members = mockMembers.take(2)
    ),

    StudyRoom(
        "r7", "Private Study Circle",
        RoomSubject.Mixed, RoomType.Private, 4, 10,
        "Mixed Revision Session", 15,
        "Rahul Kumar", joinCode = "BPSC2026",
        tags = listOf("Invite Only"),
        members = mockMembers.take(4)
    ),

    StudyRoom(
        "r8", "Science & Tech Weekly",
        RoomSubject.Science, RoomType.Public, 11, 25,
        "Space Missions + Environment",
        6, "Pooja Kumari",
        tags = listOf("Weekly Meet"),
        members = mockMembers.take(3)
    ),
)

// ─────────────────────────────────────────────────────────────
// MAIN — STATE MACHINE
// ─────────────────────────────────────────────────────────────
@Composable
fun ReadingRoomsScreen(
    navController: NavHostController,
    viewModel: ReadingRoomsViewModel = hiltViewModel()
) {
    var activeRoom by remember { mutableStateOf<StudyRoom?>(null) }

    if (activeRoom != null) {
        ActiveRoomScreen(
            room = activeRoom!!,
            onExit = { activeRoom = null }
        )
    } else {
        RoomLobbyScreen(
            navController = navController,
            viewModel = viewModel,
            onJoinRoom = { activeRoom = it }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// LOBBY SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun RoomLobbyScreen(
    navController: NavHostController,
    viewModel: ReadingRoomsViewModel,
    onJoinRoom: (StudyRoom) -> Unit,
) {

    val vmState by viewModel.uiState.collectAsState()
    var selectedSubject by remember { mutableStateOf(RoomSubject.All) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showJoinSheet by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val allRooms = vmState.rooms.map { dto ->
        StudyRoom(
            id = dto.id,
            name = dto.name,
            subject = RoomSubject.values().firstOrNull {
                it.name.equals(dto.subject, true)
            } ?: RoomSubject.All,
            type = if (dto.isPrivate) RoomType.Private else RoomType.Public,
            activeUsers = dto.currentMembers,
            maxUsers = dto.maxMembers,
            todayFocus = dto.todayFocus,
            streak = 0,
            adminName = dto.hostName,
            isFeatured = dto.isFeatured,
            tags = dto.tags ?: emptyList(),
            members = emptyList(),
            messages = emptyList(),
            pomodoroMinutes = dto.durationMins ?: 25
        )
    }

    val filtered = allRooms.filter { room ->
        val matchesSub = selectedSubject == RoomSubject.All || room.subject == selectedSubject
        val matchesSearch = searchQuery.isEmpty() ||
                room.name.contains(searchQuery, true) ||
                room.todayFocus.contains(searchQuery, true) ||
                room.tags.any { it.contains(searchQuery, true) }
        matchesSub && matchesSearch
    }

    val featured = filtered.filter { it.isFeatured }
    val others = filtered.filter { !it.isFeatured }

    if (vmState.isLoading && allRooms.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (vmState.error != null && allRooms.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(vmState.error!!)
        }
        return
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                            Offset(0f, 0f), Offset(400f, 400f)
                        )
                    )
                    .statusBarsPadding()
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        Color.White.copy(0.05f),
                        160.dp.toPx(),
                        Offset(size.width + 20.dp.toPx(), -50.dp.toPx())
                    )
                    drawCircle(
                        Color.White.copy(0.04f),
                        80.dp.toPx(),
                        Offset(-20.dp.toPx(), size.height * 0.7f)
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    // Top row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                                    .clickable { navController.popBackStack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowBack,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    "E- Library",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "Study together, rank higher",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(0.7f)
                                )
                            }
                        }
                        // Create + Join buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(0.15f))
                                    .clickable { showJoinSheet = true }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Login,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Join",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .clickable { showCreateSheet = true }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        null,
                                        tint = BpscColors.Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Create",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BpscColors.Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Search
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.15f))
                            .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            null,
                            tint = Color.White.copy(0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) Text(
                                    "Search rooms, topics...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(0.5f)
                                )
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) Icon(
                            Icons.Rounded.Close,
                            null,
                            tint = Color.White.copy(0.7f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" })
                    }

                    Spacer(Modifier.height(12.dp))

                    // Stats strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.1f))
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RoomStatChip("👥", "${mockRooms.sumOf { it.activeUsers }}", "Online Now")
                        Box(Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color.White.copy(0.2f)))
                        RoomStatChip("🏠", "${mockRooms.size}", "Rooms")
                        Box(Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color.White.copy(0.2f)))
                        RoomStatChip("🔥", "7", "My Streak")
                        Box(Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color.White.copy(0.2f)))
                        RoomStatChip("⏱️", "2.4h", "Today")
                    }

                    Spacer(Modifier.height(12.dp))

                    // Subject filter
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(RoomSubject.values()) { sub ->
                            val sel = selectedSubject == sub
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (sel) Color.White else Color.White.copy(0.15f))
                                    .clickable { selectedSubject = sub }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(sub.emoji, fontSize = 12.sp)
                                Text(
                                    sub.label, style = MaterialTheme.typography.bodyMedium,
                                    color = if (sel) BpscColors.Primary else Color.White,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Featured grid
                if (featured.isNotEmpty()) {
                    item {
                        Text(
                            "⭐ Featured Rooms",
                            style = MaterialTheme.typography.titleLarge,
                            color = BpscColors.TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(if (featured.size > 2) 340.dp else 160.dp),
                            userScrollEnabled = false
                        ) {
                            items(featured) { room ->
                                FeaturedRoomCard(room = room, onJoin = { viewModel.joinRoom(room.id) {
                                    onJoinRoom(room)
                                } })
                            }
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                // All rooms list
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "All Rooms",
                            style = MaterialTheme.typography.titleLarge,
                            color = BpscColors.TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${filtered.size} rooms",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary
                        )
                    }
                }
                items(others.ifEmpty { filtered }) { room ->
                    RoomListCard(room = room, onJoin = { viewModel.joinRoom(room.id) {
                        onJoinRoom(room)
                    } })
                }
            }
        }

        if (showCreateSheet) CreateRoomSheet(onDismiss = { showCreateSheet = false })
        if (showJoinSheet) JoinByCodeSheet(onDismiss = { showJoinSheet = false }, onJoin = { code ->
            val found = mockRooms.firstOrNull { it.joinCode == code }
            if (found != null) {
                showJoinSheet = false; onJoinRoom(found)
            }
        })
    }
}

// ─────────────────────────────────────────────────────────────
// FEATURED ROOM CARD (grid)
// ─────────────────────────────────────────────────────────────
@Composable
private fun FeaturedRoomCard(room: StudyRoom, onJoin: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onJoin),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(room.subject.color, room.subject.color.copy(alpha = 0.7f)),
                        Offset(0f, 0f), Offset(200f, 200f)
                    )
                )
        ) {
            // Decorative circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 60.dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.08f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: emoji + live badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(room.subject.emoji, fontSize = 26.sp)
                    // Live users
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2ECC71))
                        )
                        Text(
                            "${room.activeUsers}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        room.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Whatshot,
                            null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "${room.streak} day streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.85f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ROOM LIST CARD
// ─────────────────────────────────────────────────────────────
@Composable
private fun RoomListCard(room: StudyRoom, onJoin: () -> Unit) {
    val isFull = room.activeUsers >= room.maxUsers

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subject icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(room.subject.bg), contentAlignment = Alignment.Center
                ) {
                    Text(room.subject.emoji, fontSize = 22.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            room.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (room.type == RoomType.Private) Icon(
                            Icons.Rounded.Lock,
                            null,
                            tint = BpscColors.TextHint,
                            modifier = Modifier.size(14.dp)
                        )
                        if (room.type == RoomType.Official) Icon(
                            Icons.Rounded.Verified,
                            null,
                            tint = BpscColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Person,
                            null,
                            tint = BpscColors.TextHint,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            room.adminName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Join button
                Button(
                    onClick = onJoin,
                    enabled = !isFull,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = room.subject.color,
                        disabledContainerColor = BpscColors.Divider
                    ),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Text(
                        if (isFull) "Full" else "Join",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Today's focus
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(room.subject.bg)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Rounded.TrackChanges,
                    null,
                    tint = room.subject.color,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    "Today: ${room.todayFocus}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = room.subject.color,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live user count + capacity bar
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (room.activeUsers > 0) Color(0xFF2ECC71) else BpscColors.TextHint)
                        )
                        Text(
                            "${room.activeUsers}/${room.maxUsers} online",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary
                        )
                        Text("·", color = BpscColors.TextHint)
                        Icon(
                            Icons.Rounded.Whatshot,
                            null,
                            tint = BpscColors.CoinGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "${room.streak}d streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary
                        )
                    }
                    // Capacity bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BpscColors.Surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(room.activeUsers.toFloat() / room.maxUsers)
                                .fillMaxHeight()
                                .background(
                                    if (isFull) Color(0xFFE74C3C) else room.subject.color,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    room.tags.take(2).forEach { tag ->
                        Text(
                            tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = room.subject.color,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(room.subject.bg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Member avatars
            if (room.members.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    room.members.take(5).forEach { member ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(member.color)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                member.initials,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    if (room.members.size > 5) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(BpscColors.Surface)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+${room.members.size - 5}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.TextSecondary,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ACTIVE ROOM SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun ActiveRoomScreen(room: StudyRoom, onExit: () -> Unit) {
    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chat", "Members", "Leaderboard", "Pomodoro")

    // Pomodoro state
    var pomodoroSeconds by remember { mutableIntStateOf(room.pomodoroMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var sessionsDone by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (pomodoroSeconds > 0 && isRunning) {
                delay(1000)
                pomodoroSeconds--
            }
            if (pomodoroSeconds == 0) {
                isRunning = false; sessionsDone++; pomodoroSeconds = room.pomodoroMinutes * 60
            }
        }
    }

    val mins = pomodoroSeconds / 60
    val secs = pomodoroSeconds % 60
    val pomoProg = 1f - (pomodoroSeconds.toFloat() / (room.pomodoroMinutes * 60))

    Column(modifier = Modifier
        .fillMaxSize()
        .background(BpscColors.Surface)) {

        // ── Room header ───────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            room.subject.color.copy(
                                red = room.subject.color.red * 0.6f,
                                green = room.subject.color.green * 0.6f,
                                blue = room.subject.color.blue * 0.6f
                            ), room.subject.color
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.2f))
                                .clickable(onClick = onExit),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                room.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2ECC71))
                                )
                                Text(
                                    "${room.activeUsers} studying now",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(0.8f)
                                )
                            }
                        }
                    }
                    // Pomodoro mini indicator
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { activeTab = 3 },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 3.dp.toPx()
                            drawArc(
                                Color.White.copy(0.2f),
                                -90f,
                                360f,
                                false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
                            )
                            if (isRunning) drawArc(
                                Color.White,
                                -90f,
                                pomoProg * 360f,
                                false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    stroke,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                        Icon(
                            if (isRunning) Icons.Rounded.Timer else Icons.Rounded.TimerOff,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Today's focus
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.TrackChanges,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Focus: ${room.todayFocus}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tab bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val sel = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) Color.White else Color.White.copy(0.15f))
                                .clickable { activeTab = index }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tab,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) room.subject.color else Color.White,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Tab content ───────────────────────────────────────
        when (activeTab) {
            0 -> ChatTab(room = room)
            1 -> MembersTab(room = room)
            2 -> LeaderboardTab(room = room)
            3 -> PomodoroTab(
                mins = mins, secs = secs, progress = pomoProg,
                isRunning = isRunning, sessionsDone = sessionsDone,
                pomodoroMinutes = room.pomodoroMinutes,
                accentColor = room.subject.color,
                onToggle = { isRunning = !isRunning },
                onReset = { isRunning = false; pomodoroSeconds = room.pomodoroMinutes * 60 }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CHAT TAB
// ─────────────────────────────────────────────────────────────
@Composable
private fun ChatTab(room: StudyRoom) {
    val messages = remember { mutableStateListOf(*room.messages.toTypedArray()) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // Pinned topic card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BpscColors.PrimaryLight)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.PushPin,
                        null,
                        tint = BpscColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "📌 Today's Focus: ${room.todayFocus}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BpscColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            items(messages) { msg ->
                if (msg.isCurrentUser) {
                    // Sent message
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 14.dp,
                                            bottomEnd = 14.dp
                                        )
                                    )
                                    .background(BpscColors.Primary)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    msg.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    lineHeight = 20.sp
                                )
                            }
                            Text(
                                msg.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.TextHint,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                } else {
                    // Received message
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(msg.senderColor), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                msg.senderInitials,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                            Text(
                                msg.senderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = msg.senderColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 14.dp,
                                            bottomStart = 14.dp,
                                            bottomEnd = 14.dp
                                        )
                                    )
                                    .background(Color.White)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    msg.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = BpscColors.TextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                            Text(
                                msg.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.TextHint,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(BpscColors.Surface)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = inputText, onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = BpscColors.TextPrimary),
                    decorationBox = { inner ->
                        if (inputText.isEmpty()) Text(
                            "Type a message...",
                            color = BpscColors.TextHint,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        inner()
                    }
                )
                // Emoji quick buttons
                listOf("👍", "💡", "📌").forEach { e ->
                    Text(
                        e,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { inputText += e }
                            .padding(horizontal = 3.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) BpscColors.Primary else BpscColors.Divider)
                    .clickable {
                        if (inputText.isNotBlank()) {
                            messages.add(
                                ChatMessage(
                                    "c${messages.size + 1}", "m1", "Rahul Kumar", "RK",
                                    Color(0xFF1565C0), inputText.trim(),
                                    "${
                                        java.util.Calendar.getInstance()
                                            .get(java.util.Calendar.HOUR_OF_DAY)
                                    }:${
                                        "%02d".format(
                                            java.util.Calendar.getInstance()
                                                .get(java.util.Calendar.MINUTE)
                                        )
                                    }",
                                    true
                                )
                            )
                            inputText = ""
                            scope.launch { listState.animateScrollToItem(messages.size) }
                        }
                    }, contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// MEMBERS TAB
// ─────────────────────────────────────────────────────────────
@Composable
private fun MembersTab(room: StudyRoom) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "${room.members.size} Members",
                style = MaterialTheme.typography.titleMedium,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        items(room.members.sortedByDescending { it.studyMinutes }) { member ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(member.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            member.initials,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                member.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            if (member.isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2ECC71))
                                )
                            }
                        }
                        Text(
                            "${member.studyMinutes} min studied today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary
                        )
                    }
                    // Study time bar
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "${member.studyMinutes}m",
                            style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.Primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(BpscColors.Surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(member.studyMinutes / 320f)
                                    .fillMaxHeight()
                                    .background(member.color, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LEADERBOARD TAB
// ─────────────────────────────────────────────────────────────
@Composable
private fun LeaderboardTab(room: StudyRoom) {
    val sorted = room.members.sortedByDescending { it.studyMinutes }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Today's Leaderboard",
                    style = MaterialTheme.typography.titleMedium,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Resets midnight",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextHint
                )
            }
        }

        // Podium top 3
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                sorted.getOrNull(1)?.let { RoomPodiumItem(it, 2, 80.dp) }
                sorted.getOrNull(0)?.let { RoomPodiumItem(it, 1, 100.dp) }
                sorted.getOrNull(2)?.let { RoomPodiumItem(it, 3, 65.dp) }
            }
        }

        itemsIndexed(sorted) { index, member ->
            val medals = listOf("🥇", "🥈", "🥉")
            val isMe = member.id == "m1"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isMe) BpscColors.PrimaryLight else Color.White),
                elevation = CardDefaults.cardElevation(if (isMe) 3.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (index < 3) medals[index] else "#${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (index < 3) BpscColors.TextPrimary else BpscColors.TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(member.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            member.initials,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                member.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isMe) Text(
                                "You",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BpscColors.PrimaryLight)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        "${member.studyMinutes}m",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isMe) BpscColors.Primary else BpscColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomPodiumItem(member: RoomMember, pos: Int, height: Dp) {
    val medals = listOf(Color(0xFFFFD700), Color(0xFFC0C0C0), Color(0xFFCD7F32))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(member.color)
                .border(2.dp, medals[pos - 1], CircleShape), contentAlignment = Alignment.Center
        ) {
            Text(
                member.initials,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            member.name.split(" ").first(),
            style = MaterialTheme.typography.bodyMedium,
            color = BpscColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${member.studyMinutes}m",
            style = MaterialTheme.typography.labelSmall,
            color = medals[pos - 1],
            fontWeight = FontWeight.ExtraBold
        )
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(medals[pos - 1].copy(0.25f))
        )
    }
}

// ─────────────────────────────────────────────────────────────
// POMODORO TAB
// ─────────────────────────────────────────────────────────────
@Composable
private fun PomodoroTab(
    mins: Int, secs: Int, progress: Float,
    isRunning: Boolean, sessionsDone: Int,
    pomodoroMinutes: Int, accentColor: Color,
    onToggle: () -> Unit, onReset: () -> Unit,
) {
    val animProg by animateFloatAsState(progress, tween(500), label = "pomo")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            if (isRunning) "🎯 Focus Session" else "⏸ Paused",
            style = MaterialTheme.typography.titleLarge,
            color = BpscColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        // Big timer ring
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                val inset = stroke / 2
                val sz =
                    androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                drawArc(
                    Color(0xFFEEEEEE),
                    -90f,
                    360f,
                    false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                    topLeft = Offset(inset, inset),
                    size = sz
                )
                drawArc(
                    accentColor, -90f, animProg * 360f, false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        stroke,
                        cap = StrokeCap.Round
                    ), topLeft = Offset(inset, inset), size = sz
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%02d:%02d".format(mins, secs),
                    style = MaterialTheme.typography.displayMedium,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "of $pomodoroMinutes min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextSecondary
                )
            }
        }

        // Sessions done
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (i < sessionsDone % 4) accentColor else BpscColors.Divider)
                )
            }
        }
        Text(
            "$sessionsDone sessions completed today",
            style = MaterialTheme.typography.bodyMedium,
            color = BpscColors.TextSecondary
        )

        // Controls
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onReset,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(52.dp),
                border = BorderStroke(1.dp, BpscColors.Divider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.TextSecondary)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = onToggle,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(52.dp)
                    .width(160.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(
                    if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isRunning) "Pause" else "Start",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "📖 Pomodoro Tips",
                    style = MaterialTheme.typography.titleMedium,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                listOf(
                    "Focus for 25 min, then take a 5 min break",
                    "After 4 sessions, take a longer 15 min break",
                    "Keep phone away during focus session",
                    "Note down distracting thoughts, revisit in break"
                ).forEach { tip ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = accentColor, fontWeight = FontWeight.ExtraBold)
                        Text(
                            tip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CREATE ROOM SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRoomSheet(onDismiss: () -> Unit) {
    var roomName by remember { mutableStateOf("") }
    var selectedSub by remember { mutableStateOf(RoomSubject.Polity) }
    var isPrivate by remember { mutableStateOf(false) }
    var focusTopic by remember { mutableStateOf("") }
    var maxUsers by remember { mutableIntStateOf(20) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Create Study Room",
                style = MaterialTheme.typography.headlineSmall,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            HorizontalDivider(color = BpscColors.Divider)

            // Room name
            OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it },
                label = { Text("Room Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Focus topic
            OutlinedTextField(
                value = focusTopic,
                onValueChange = { focusTopic = it },
                label = { Text("Today's Focus Topic") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Subject chips
            Text(
                "Subject",
                style = MaterialTheme.typography.titleMedium,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RoomSubject.values().filter { it != RoomSubject.All }) { sub ->
                    val sel = selectedSub == sub
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (sel) sub.color else sub.bg)
                            .clickable { selectedSub = sub }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(sub.emoji, fontSize = 12.sp)
                        Text(
                            sub.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (sel) Color.White else sub.color,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Max users
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Max Members",
                    style = MaterialTheme.typography.titleMedium,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$maxUsers",
                    style = MaterialTheme.typography.titleMedium,
                    color = BpscColors.Primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Slider(
                value = maxUsers.toFloat(),
                onValueChange = { maxUsers = it.toInt() },
                valueRange = 5f..100f,
                steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = BpscColors.Primary,
                    activeTrackColor = BpscColors.Primary
                )
            )

            // Private toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Private Room",
                        style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Requires join code to enter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BpscColors.TextSecondary
                    )
                }
                Switch(
                    checked = isPrivate, onCheckedChange = { isPrivate = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BpscColors.Primary
                    )
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = roomName.isNotBlank() && focusTopic.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Room 🚀", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// JOIN BY CODE SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinByCodeSheet(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Join Private Room",
                style = MaterialTheme.typography.headlineSmall,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Enter the room code shared by the admin",
                style = MaterialTheme.typography.bodyLarge,
                color = BpscColors.TextSecondary
            )

            OutlinedTextField(
                value = code, onValueChange = { code = it.uppercase(); isError = false },
                label = { Text("Room Code (e.g. BPSC2026)") },
                isError = isError,
                supportingText = if (isError) {
                    { Text("Invalid room code. Try again.") }
                } else null,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )

            Button(
                onClick = {
                    val found = mockRooms.firstOrNull { it.joinCode == code }
                    if (found != null) onJoin(code) else isError = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp), shape = RoundedCornerShape(14.dp),
                enabled = code.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Icon(Icons.Rounded.Login, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Join Room", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
@Composable
private fun RoomStatChip(icon: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.width(64.dp)
    ) {
        Text(icon, fontSize = 13.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(0.6f),
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}