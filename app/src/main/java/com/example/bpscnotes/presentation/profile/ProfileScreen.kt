package com.example.bpscnotes.presentation.profile

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen

data class UserProfile(
    val name: String,
    val email: String,
    val mobile: String,
    val bio: String,
    val joinedDate: String,
    val daysActive: Int,
    val examTarget: String,
    val prepLevel: String,
    val district: String,
    val coinBalance: Int,
    val streak: Int,
    val totalStudyHours: Float,
    val accuracy: Float,
    val rank: Int,
    val totalUsers: Int,
    val percentile: Float,
    val weeklyHeatmap: List<Int>,   // 0-4 intensity per day (28 days)
    val subjectAccuracy: Map<String, Float>,
    val coinHistory: List<CoinTransaction>,
    val achievements: List<Achievement>,
    val referralCode: String,
)

data class CoinTransaction(
    val id: String,
    val description: String,
    val amount: Int,        // positive = earned, negative = spent
    val date: String,
    val icon: String,
)

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val color: Color,
    val isEarned: Boolean,
    val earnedDate: String? = null,
    val progress: Float = 0f,   // for locked achievements
)

val mockProfile = UserProfile(
    name         = "Rahul Kumar",
    email        = "rahul@example.com",
    mobile       = "+91 98765 43210",
    bio          = "Aspiring civil servant | BPSC 70th aspirant | Study hard, dream big 🎯",
    joinedDate   = "15 Jan 2026",
    daysActive   = 64,
    examTarget   = "BPSC 70th CCE",
    prepLevel    = "Intermediate",
    district     = "Patna, Bihar",
    coinBalance  = 142,
    streak       = 7,
    totalStudyHours = 128.5f,
    accuracy     = 87f,
    rank         = 3,
    totalUsers   = 18500,
    percentile   = 95.2f,
    weeklyHeatmap = listOf(0,1,2,0,3,4,2, 1,2,3,2,4,3,1, 0,2,4,3,2,1,3, 2,3,1,4,3,2,4),
    subjectAccuracy = mapOf(
        "Polity"    to 91f,
        "History"   to 84f,
        "Geography" to 88f,
        "Economy"   to 79f,
        "Bihar GK"  to 94f,
        "Science"   to 82f,
    ),
    coinHistory = listOf(
        CoinTransaction("ct1", "Daily Quiz Completed",          +5,   "Today",           "🎯"),
        CoinTransaction("ct2", "Course Purchase — Polity",      -80,  "Yesterday",       "🛍️"),
        CoinTransaction("ct3", "Mock Test — Top 10",            +20,  "2 days ago",      "🏆"),
        CoinTransaction("ct4", "7 Day Streak Bonus",            +15,  "3 days ago",      "🔥"),
        CoinTransaction("ct5", "Active Recall — 10 cards",      +10,  "4 days ago",      "🧠"),
        CoinTransaction("ct6", "Referral Bonus — Amit joined",  +50,  "5 days ago",      "🎁"),
        CoinTransaction("ct7", "Notes Download",                -5,   "1 week ago",      "📄"),
        CoinTransaction("ct8", "Daily Login Bonus",             +2,   "1 week ago",      "☀️"),
    ),
    achievements = listOf(
        Achievement("a1",  "🔥", "7 Day Streak",     "Study 7 days in a row",        Color(0xFFE74C3C),  true,  "10 Mar 2026"),
        Achievement("a2",  "🏆", "Top 10 Rank",      "Reach top 10 on leaderboard",  Color(0xFFFFD700),  true,  "08 Mar 2026"),
        Achievement("a3",  "📚", "100 Topics Done",  "Complete 100 study topics",    Color(0xFF1565C0),  true,  "05 Mar 2026"),
        Achievement("a4",  "🎯", "Quiz Master",      "Score 90%+ in 5 quizzes",      Color(0xFF9B59B6),  true,  "01 Mar 2026"),
        Achievement("a5",  "🧠", "Recall Champion",  "Review 200 flashcards",        Color(0xFF1ABC9C),  true,  "25 Feb 2026"),
        Achievement("a6",  "⚡", "Speed Star",       "Finish quiz in under 10 min",  Color(0xFFE67E22),  false, null, 0.6f),
        Achievement("a7",  "💯", "Perfect Score",    "Score 100% in any quiz",       Color(0xFF2ECC71),  false, null, 0.3f),
        Achievement("a8",  "📖", "Bookworm",         "Download 20 study materials",  Color(0xFFE74C3C),  false, null, 0.4f),
        Achievement("a9",  "👥", "Team Player",      "Join 5 group study rooms",     Color(0xFF3498DB),  false, null, 0.8f),
        Achievement("a10", "🌟", "All-Rounder",      "Score 80%+ in all subjects",   Color(0xFFFFD700),  false, null, 0.5f),
    ),
    referralCode = "RAHUL2026",
)

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(navController: NavHostController) {
    var showEditSheet    by remember { mutableStateOf(false) }
    var showReferSheet   by remember { mutableStateOf(false) }
    var showCoinHistory  by remember { mutableStateOf(false) }
    var showExamSheet    by remember { mutableStateOf(false) }
    val profile          = mockProfile

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Hero header ──────────────────────────────────
            item {
                ProfileHeroSection(
                    profile      = profile,
                    onEdit       = { showEditSheet = true },
                    onShare      = { },
                    navController = navController
                )
            }

            // ── Rank + Stats ─────────────────────────────────
            item { RankStatsSection(profile = profile) }

            // ── Weekly heatmap ───────────────────────────────
            item { WeeklyHeatmapSection(heatmap = profile.weeklyHeatmap) }

            // ── Subject accuracy ─────────────────────────────
            item { SubjectAccuracySection(subjects = profile.subjectAccuracy) }

            // ── Coin wallet summary ──────────────────────────
            item {
                CoinSummarySection(
                    balance   = profile.coinBalance,
                    history   = profile.coinHistory.take(3),
                    onViewAll = { navController.navigate(Screen.CoinWallet.route) }
                )
            }

            // ── Achievements ─────────────────────────────────
            item { AchievementsSection(achievements = profile.achievements) }

            // ── Quick actions ────────────────────────────────
            item {
                QuickActionsSection(
                    profile       = profile,
                    onEditProfile = { showEditSheet = true },
                    onExamTarget  = { showExamSheet = true },
                    onRefer       = { showReferSheet = true },
                    onNotifications = { navController.navigate(Screen.NotificationSettings.route) },
                    onPrivacy     = { navController.navigate(Screen.Settings.route) },
                    onDownloads   = { navController.navigate(Screen.Downloads.route) },
                    onSubscription = { navController.navigate(Screen.Subscription.route) },
                    onLogout      = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Sheets
        if (showEditSheet)   EditProfileSheet(profile = profile, onDismiss = { showEditSheet = false })
        if (showReferSheet)  ReferralSheet(profile = profile, onDismiss = { showReferSheet = false })
        if (showExamSheet)   ExamTargetSheet(profile = profile, onDismiss = { showExamSheet = false })
        if (showCoinHistory) CoinHistorySheet(history = profile.coinHistory, balance = profile.coinBalance, onDismiss = { showCoinHistory = false })
    }
}

// ─────────────────────────────────────────────────────────────
// HERO SECTION
// ─────────────────────────────────────────────────────────────
@Composable
private fun ProfileHeroSection(profile: UserProfile, onEdit: () -> Unit, onShare: () -> Unit, navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.linearGradient(
                listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                Offset(0f, 0f), Offset(400f, 400f)
            ))
            .statusBarsPadding()
    ) {
        // Decorative blobs
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
            drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
        }

        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Top row — title + actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("My Profile", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onShare), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onEdit), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Avatar + info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.White.copy(0.2f)).border(3.dp, Color.White.copy(0.5f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(
                            profile.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                            style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold
                        )
                    }
                    // Camera icon
                    Box(modifier = Modifier.size(24.dp).align(Alignment.BottomEnd).clip(CircleShape).background(BpscColors.Primary).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(profile.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(profile.bio, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f), maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(0.15f)).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.LocationOn, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(11.dp))
                            Text(profile.district, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f), fontSize = 10.sp)
                        }
                        Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(0.15f)).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🎓", fontSize = 10.sp)
                            Text(profile.examTarget, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f), fontSize = 10.sp)
                        }
                    }
                }
            }

            // Quick stats strip
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.12f)).padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeroStat("🔥", "${profile.streak}", "Day Streak")
                Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                HeroStat("⏱️", "${profile.totalStudyHours.toInt()}h", "Study Time")
                Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                HeroStat("🪙", "${profile.coinBalance}", "Coins")
                Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                HeroStat("📅", "${profile.daysActive}", "Days Active")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// RANK + STATS SECTION
// ─────────────────────────────────────────────────────────────
@Composable
private fun RankStatsSection(profile: UserProfile) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("🏆 My Performance")

        // Rank card
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(3.dp)) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0)), Offset(0f, 0f), Offset(400f, 150f)))) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Rank badge
                    Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.White.copy(0.15f)).border(2.dp, Color(0xFFFFD700), CircleShape), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆", fontSize = 20.sp)
                            Text("#${profile.rank}", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("All India Rank", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                        Text("Top ${100 - profile.percentile.toInt()}% students", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(0.15f)).padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Percentile", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                            Text("${profile.percentile}%", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFD700), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Stats grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("✅", "${profile.accuracy.toInt()}%", "Accuracy",   Color(0xFF2ECC71), Color(0xFFE8FDF4), Modifier.weight(1f))
            StatCard("⏱️", "${profile.totalStudyHours.toInt()}h", "Total Study", Color(0xFF1565C0), Color(0xFFE8F0FD), Modifier.weight(1f))
            StatCard("📊", "${profile.prepLevel}", "Level", Color(0xFF9B59B6), Color(0xFFF3E8FD), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(icon: String, value: String, label: String, color: Color, bg: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = bg), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 20.sp)
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(0.7f), fontSize = 9.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WEEKLY HEATMAP
// ─────────────────────────────────────────────────────────────
@Composable
private fun WeeklyHeatmapSection(heatmap: List<Int>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("📅 Study Heatmap", subtitle = "Last 28 days")
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Day labels
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                        Text(day, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                // 4 weeks grid
                heatmap.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEach { intensity ->
                            val color = when (intensity) {
                                0    -> BpscColors.Surface
                                1    -> BpscColors.Primary.copy(alpha = 0.2f)
                                2    -> BpscColors.Primary.copy(alpha = 0.4f)
                                3    -> BpscColors.Primary.copy(alpha = 0.7f)
                                else -> BpscColors.Primary
                            }
                            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(color))
                        }
                    }
                }
                // Legend
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Less", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
                    Spacer(Modifier.width(4.dp))
                    listOf(0, 1, 2, 3, 4).forEach { i ->
                        val color = when (i) { 0 -> BpscColors.Surface; 1 -> BpscColors.Primary.copy(0.2f); 2 -> BpscColors.Primary.copy(0.4f); 3 -> BpscColors.Primary.copy(0.7f); else -> BpscColors.Primary }
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
                        Spacer(Modifier.width(3.dp))
                    }
                    Text("More", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SUBJECT ACCURACY
// ─────────────────────────────────────────────────────────────
@Composable
private fun SubjectAccuracySection(subjects: Map<String, Float>) {
    val subjectColors = mapOf("Polity" to Color(0xFF9B59B6), "History" to Color(0xFFE74C3C), "Geography" to Color(0xFF1ABC9C), "Economy" to Color(0xFFE67E22), "Bihar GK" to Color(0xFFF39C12), "Science" to Color(0xFF2ECC71))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("📊 Subject-wise Performance")
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                subjects.entries.sortedByDescending { it.value }.forEach { (subject, accuracy) ->
                    val color = subjectColors[subject] ?: BpscColors.Primary
                    val animAcc by animateFloatAsState(accuracy / 100f, tween(1200), label = "acc$subject")
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(subject, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (accuracy >= 90f) Text("⭐", fontSize = 12.sp)
                                Text("${accuracy.toInt()}%", style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(color.copy(0.1f))) {
                            Box(modifier = Modifier.fillMaxWidth(animAcc).fillMaxHeight().background(color, RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// COIN SUMMARY
// ─────────────────────────────────────────────────────────────
@Composable
private fun CoinSummarySection(balance: Int, history: List<CoinTransaction>, onViewAll: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("🪙 Coin Wallet")
            TextButton(onClick = onViewAll) { Text("View all", color = BpscColors.Primary, style = MaterialTheme.typography.bodyMedium) }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(3.dp)) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Current Balance", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🪙", fontSize = 24.sp)
                                Text("$balance", style = MaterialTheme.typography.displaySmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                                Text("coins", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))
                            }
                            Text("= ₹${(balance * 0.10).toInt()} discount value", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.6f))
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("How to earn:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
                            listOf("Daily quiz +5", "Streak bonus +15", "Referral +50").forEach {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontSize = 10.sp)
                            }
                        }
                    }
                    // Recent transactions
                    history.forEach { txn ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(0.1f)).padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(txn.icon, fontSize = 16.sp)
                            Text(txn.description, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.85f), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${if (txn.amount > 0) "+" else ""}${txn.amount} 🪙",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (txn.amount > 0) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ACHIEVEMENTS
// ─────────────────────────────────────────────────────────────
@Composable
private fun AchievementsSection(achievements: List<Achievement>) {
    val earned  = achievements.filter { it.isEarned }
    val locked  = achievements.filter { !it.isEarned }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("🏅 Achievements")
            Text("${earned.size}/${achievements.size} earned", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
        }

        // Earned row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 4.dp)) {
            items(earned) { achievement ->
                AchievementBadge(achievement = achievement)
            }
        }

        // Locked section
        Text("🔒 In Progress", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
        locked.forEach { achievement ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(BpscColors.Surface).border(2.dp, BpscColors.Divider, CircleShape), contentAlignment = Alignment.Center) {
                        Text(achievement.emoji, fontSize = 20.sp, modifier = Modifier.alpha(0.4f))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(achievement.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(achievement.description, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(BpscColors.Surface)) {
                            Box(modifier = Modifier.fillMaxWidth(achievement.progress).fillMaxHeight().background(achievement.color, RoundedCornerShape(3.dp)))
                        }
                    }
                    Text("${(achievement.progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = achievement.color, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.width(76.dp)) {
        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(achievement.color.copy(0.15f)).border(2.dp, achievement.color, CircleShape), contentAlignment = Alignment.Center) {
            Text(achievement.emoji, fontSize = 26.sp)
        }
        Text(achievement.title, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (achievement.earnedDate != null) Text(achievement.earnedDate, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────
// QUICK ACTIONS
// ─────────────────────────────────────────────────────────────
@Composable
private fun QuickActionsSection(
    profile: UserProfile,
    onEditProfile: () -> Unit,
    onExamTarget: () -> Unit,
    onRefer: () -> Unit,
    onNotifications: () -> Unit,
    onPrivacy: () -> Unit,
    onDownloads: () -> Unit,
    onSubscription: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("⚙️ Settings & More")

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val actions = listOf(
                    Triple(Icons.Rounded.Person,           "Edit Profile",            Color(0xFF1565C0)) to onEditProfile,
                    Triple(Icons.Rounded.TrackChanges,     "Exam Target — ${profile.examTarget}", Color(0xFF9B59B6)) to onExamTarget,
                    Triple(Icons.Rounded.CardGiftcard,     "Refer & Earn — ${profile.referralCode}", Color(0xFF2ECC71)) to onRefer,
                    Triple(Icons.Rounded.Notifications,    "Notification Settings",   Color(0xFFE67E22)) to onNotifications,
                    Triple(Icons.Rounded.Download,         "My Downloads",            Color(0xFF1ABC9C)) to onDownloads,
                    Triple(Icons.Rounded.Star,             "Subscription & Plans",    BpscColors.CoinGold) to onSubscription,
                    Triple(Icons.Rounded.Lock,             "Privacy & Security",      Color(0xFF95A5A6)) to onPrivacy,
                )
                actions.forEachIndexed { index, (triple, action) ->
                    val (icon, label, color) = triple
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = action).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(0.1f)), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                        }
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
                    }
                    if (index < actions.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                }
            }
        }

        // App info
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Info, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("About BPSCNotes", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary)
                        Text("Version 1.0.0 · Joined ${profile.joinedDate}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                // Logout
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onLogout).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFEE8E8)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Logout, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(18.dp))
                    }
                    Text("Logout", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE74C3C), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFFE74C3C).copy(0.5f), modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("BPSCNotes · Only What Matters", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ─────────────────────────────────────────────────────────────
// EDIT PROFILE SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(profile: UserProfile, onDismiss: () -> Unit) {
    var name     by remember { mutableStateOf(profile.name) }
    var bio      by remember { mutableStateOf(profile.bio) }
    var district by remember { mutableStateOf(profile.district) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Edit Profile", style = MaterialTheme.typography.headlineSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
            HorizontalDivider(color = BpscColors.Divider)

            // Avatar
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(80.dp)) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(BpscColors.PrimaryLight).border(2.dp, BpscColors.Primary, CircleShape), contentAlignment = Alignment.Center) {
                        Text(name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                            style = MaterialTheme.typography.headlineMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(modifier = Modifier.size(26.dp).align(Alignment.BottomEnd).clip(CircleShape).background(BpscColors.Primary).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, leadingIcon = { Icon(Icons.Rounded.Person, null) })
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2, maxLines = 3)
            OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District / Location") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, leadingIcon = { Icon(Icons.Rounded.LocationOn, null) })

            Text("📞 ${profile.mobile}  ·  ✉️ ${profile.email}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Save Changes", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// EXAM TARGET SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamTargetSheet(profile: UserProfile, onDismiss: () -> Unit) {
    var selectedExam  by remember { mutableStateOf(profile.examTarget) }
    var selectedLevel by remember { mutableStateOf(profile.prepLevel) }
    val exams  = listOf("BPSC 70th CCE", "BPSC 71st CCE", "BPSC APO", "BPSC AE", "Bihar SI", "Bihar SSC", "SSC CGL", "SSC CHSL", "Railway NTPC")
    val levels = listOf("Beginner", "Intermediate", "Advanced")

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Change Exam Target", style = MaterialTheme.typography.headlineSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
            HorizontalDivider(color = BpscColors.Divider)

            Text("Select Exam", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exams) { exam ->
                    val sel = selectedExam == exam
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (sel) BpscColors.Primary else BpscColors.Surface)
                        .border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
                        .clickable { selectedExam = exam }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(exam, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Text("Preparation Level", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                levels.forEach { level ->
                    val sel = selectedLevel == level
                    val color = when (level) { "Beginner" -> Color(0xFF2ECC71); "Advanced" -> Color(0xFFE74C3C); else -> Color(0xFFE67E22) }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) color else color.copy(0.1f))
                        .border(1.dp, color, RoundedCornerShape(12.dp))
                        .clickable { selectedLevel = level }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(level, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else color, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Save Target", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// REFERRAL SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferralSheet(profile: UserProfile, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎁", fontSize = 48.sp)
            Text("Refer & Earn", style = MaterialTheme.typography.headlineSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
            Text("Invite friends to BPSCNotes and earn coins together!", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
            HorizontalDivider(color = BpscColors.Divider)

            // Rewards breakdown
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ReferralReward("🪙", "50 coins", "You earn")
                ReferralReward("🎁", "30 coins", "Friend gets")
                ReferralReward("👥", "5 friends", "Max/month")
            }

            // Referral code
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your Referral Code", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(BpscColors.PrimaryLight).border(1.5.dp, BpscColors.Primary, RoundedCornerShape(14.dp)).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(profile.referralCode, style = MaterialTheme.typography.headlineMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BpscColors.Primary).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.ContentCopy, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("Copy", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Share with Friends", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ReferralReward(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 24.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
    }
}

// ─────────────────────────────────────────────────────────────
// COIN HISTORY SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoinHistorySheet(history: List<CoinTransaction>, balance: Int, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0)))).padding(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🪙 Coin History", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Balance:", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                        Text("$balance coins", style = MaterialTheme.typography.titleMedium, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { txn ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BpscColors.Surface), elevation = CardDefaults.cardElevation(0.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (txn.amount > 0) Color(0xFFE8FDF4) else Color(0xFFFEE8E8)), contentAlignment = Alignment.Center) {
                                Text(txn.icon, fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(txn.description, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(txn.date, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                            }
                            Text(
                                "${if (txn.amount > 0) "+" else ""}${txn.amount} 🪙",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (txn.amount > 0) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
@Composable
private fun HeroStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(64.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}