package com.example.bpscnotes.presentation.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.mock.MockData
import com.example.bpscnotes.domain.model.Course
import com.example.bpscnotes.domain.model.DailyTarget
import com.example.bpscnotes.domain.model.DayProgress
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.shared.BookmarkViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    val user        = MockData.currentUser
    val targets     = MockData.dailyTargets
    val weeklyData  = MockData.weeklyConsistency
    val courses     = MockData.courses
    val completed   = targets.count { it.isCompleted }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    var showTargetSheet by remember { mutableStateOf(false) }
    val bookmarkedIds by bookmarkViewModel.bookmarkedIds.collectAsState()

    ModalNavigationDrawer(
        drawerState    = drawerState,
        gesturesEnabled = true,
        drawerContent  = {
            BpscDrawer(
                user          = user,
                onClose       = { scope.launch { drawerState.close() } },
                navController = navController
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BpscColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardHeader(
                    name        = user.name,
                    coins       = user.coinBalance,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    navController = navController
                )
                TodayTargetCard(
                    completed     = completed,
                    total         = targets.size,
                    targets       = targets,
                    onCreateTarget = { showTargetSheet = true },
                    onClick       = { navController.navigate(Screen.DailyTargets.route) }
                )
                WeeklyConsistencyCard(data = weeklyData)
                QuickAccessSection(
                    navController = navController,
                    bookmarkCount = bookmarkedIds.size
                )
                RecommendedSection(courses = courses, navController = navController)
                MyScheduleSection(navController = navController)
                AchievementsSection()
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (showTargetSheet) {
                CreateTargetSheet(onDismiss = { showTargetSheet = false })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HEADER  — richer layered hero with floating stat cards
// ─────────────────────────────────────────────────────────────
@Composable
private fun DashboardHeader(
    name: String,
    coins: Int,
    onMenuClick: () -> Unit,
    navController: NavHostController
) {
    val targets   = MockData.dailyTargets
    val completed = targets.count { it.isCompleted }
    val progress by animateFloatAsState(
        targetValue   = if (targets.isNotEmpty()) completed.toFloat() / targets.size else 0f,
        animationSpec = tween(1200),
        label         = "ring"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF051D56),   // deep midnight navy
                        Color(0xFF0A2472),   // navy
                        Color(0xFF0D47A1),   // rich blue
                        Color(0xFF1565C0),   // mid blue
                        Color(0xFF1976D2),   // bright blue
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(600f, 700f)
                )
            )
            .statusBarsPadding()
    ) {
        // ── Layered decorative canvas ─────────────────────────
        Canvas(modifier = Modifier.matchParentSize()) {
            // Large top-right blob
            drawCircle(Color.White.copy(0.05f), 200.dp.toPx(), Offset(size.width + 50.dp.toPx(), -70.dp.toPx()))
            // Mid-right glow
            drawCircle(Color.White.copy(0.04f), 110.dp.toPx(), Offset(size.width - 10.dp.toPx(), size.height * 0.5f))
            // Bottom-left soft glow
            drawCircle(Color.White.copy(0.06f), 70.dp.toPx(), Offset(-25.dp.toPx(), size.height * 0.78f))
            // Arc decoration behind progress ring
            drawArc(
                color      = Color.White.copy(0.03f),
                startAngle = 150f,
                sweepAngle = 160f,
                useCenter  = false,
                topLeft    = Offset(size.width - 130.dp.toPx(), size.height * 0.15f),
                size       = androidx.compose.ui.geometry.Size(160.dp.toPx(), 160.dp.toPx()),
                style      = Stroke(width = 40.dp.toPx())
            )
            // Dot grid
            val sp = 28.dp.toPx()
            var x = sp
            while (x < size.width) {
                var y = sp
                while (y < size.height) {
                    drawCircle(Color.White.copy(0.05f), 1.dp.toPx(), Offset(x, y))
                    y += sp
                }
                x += sp
            }
        }

        // Shiny top accent line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(0.3f), Color.White.copy(0.7f), Color.White.copy(0.3f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 0.dp)
        ) {
            // ── Top bar ──────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Hamburger — pill style
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.12f))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onMenuClick),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement   = Arrangement.spacedBy(4.dp),
                        horizontalAlignment   = Alignment.Start,
                        modifier              = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Box(Modifier.width(16.dp).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                        Box(Modifier.width(11.dp).height(2.dp).background(Color.White.copy(0.6f), RoundedCornerShape(1.dp)))
                        Box(Modifier.width(16.dp).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                    }
                }

                // Logo — slightly larger, bolder
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(0.1f))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("🔥", fontSize = 16.sp)
                    Text(
                        "BPSCNotes",
                        style         = MaterialTheme.typography.titleMedium,
                        color         = Color.White,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Coins + Bell
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Coins pill — glowing gold border
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFFFB300).copy(0.15f))
                            .border(0.5.dp, Color(0xFFFFD54F).copy(0.5f), RoundedCornerShape(22.dp))
                            .clickable { navController.navigate(Screen.CoinWallet.route) }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("🪙", fontSize = 13.sp)
                        Text(
                            "$coins",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = BpscColors.CoinGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 13.sp
                        )
                    }
                    // Bell
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.12f))
                            .border(0.5.dp, Color.White.copy(0.2f), CircleShape)
                            .clickable { navController.navigate(Screen.NotificationSettings.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Notifications, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // ── Hero row: greeting + animated ring ───────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    // Online indicator + greeting
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(BpscColors.Success))
                        Text("Good Morning", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                    }

                    // Name — larger, bolder
                    Text(
                        text       = name.split(" ").first() + " 👋",
                        style      = MaterialTheme.typography.headlineMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    )

                    Spacer(Modifier.height(2.dp))

                    // Streak pill — flame icon + gold shimmer
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFFF8F00).copy(0.18f))
                            .border(0.5.dp, Color(0xFFFFB300).copy(0.4f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Rounded.Whatshot, null, tint = BpscColors.CoinGold, modifier = Modifier.size(14.dp))
                        Text("7 day streak — keep it up!", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.9f), fontWeight = FontWeight.SemiBold)
                    }
                }

                // Progress ring — larger, richer glow layers
                Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 7.dp.toPx()
                        val inset  = stroke / 2
                        val arcSz  = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                        // Outer ambient glow
                        drawArc(
                            color = Color.White.copy(0.05f), startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSz,
                            style = Stroke(width = stroke + 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Track
                        drawArc(
                            color = Color.White.copy(0.14f), startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSz,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        // Progress fill — sweep gradient
                        drawArc(
                            brush      = Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color(0xFFE3F2FD), Color.White)),
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter  = false,
                            style      = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft    = Offset(inset, inset),
                            size       = arcSz
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("done", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Stats strip — floating glass cards ───────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(0.16f), Color.White.copy(0.08f))
                        )
                    )
                    .border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(listOf(Color.White.copy(0.35f), Color.Transparent)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                val completed2 = targets.count { it.isCompleted }
                HeaderStat("📚", "$completed2/${targets.size}", "Topics")
                HeaderStatDivider()
                HeaderStat("🏆", "#3", "My Rank")
                HeaderStatDivider()
                HeaderStat("⏱️", "6.8h", "Study")
                HeaderStatDivider()
                HeaderStat("✅", "87%", "Accuracy")
            }
        }
    }
}

@Composable
private fun HeaderStat(icon: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier            = Modifier.width(66.dp)
    ) {
        Text(icon, fontSize = 15.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HeaderStatDivider() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(36.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(0.28f), Color.Transparent)))
    )
}

// ─────────────────────────────────────────────────────────────
// TODAY'S TARGET CARD  — elevated with colored left accent bar
// ─────────────────────────────────────────────────────────────
@Composable
private fun TodayTargetCard(
    completed: Int,
    total: Int,
    targets: List<DailyTarget>,
    onCreateTarget: () -> Unit,
    onClick: () -> Unit
) {
    val progress    = if (total > 0) completed.toFloat() / total else 0f
    val animProg by animateFloatAsState(progress, tween(1000), label = "prog")

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-12).dp)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Title row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Icon with gradient bg
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Brush.linearGradient(listOf(BpscColors.Primary, Color(0xFF1976D2)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.TrackChanges, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("Today's Focus", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
                        Text("$completed/$total Topics", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                }
                // "View All" pill — makes card clickability obvious
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BpscColors.PrimaryLight)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        "View All",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = BpscColors.Primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 11.sp
                    )
                    Icon(
                        Icons.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint     = BpscColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Progress bar — rounded, taller, gradient fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(BpscColors.PrimaryLight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animProg)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF42A5F5))))
                )
            }

            Spacer(Modifier.height(16.dp))

            // Target list
            targets.take(3).forEach { target ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (target.isCompleted) BpscColors.Success.copy(0.12f) else BpscColors.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (target.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            null,
                            tint     = if (target.isCompleted) BpscColors.Success else BpscColors.TextHint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        target.title,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = if (target.isCompleted) BpscColors.TextSecondary else BpscColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Subject tag — pill
                    Text(
                        target.subject,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = BpscColors.Primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BpscColors.PrimaryLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (targets.size > 3) {
                Text("+${targets.size - 3} more topics", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Primary, modifier = Modifier.padding(top = 6.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Create Target CTA — gradient filled
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .border(1.5.dp, BpscColors.Primary, RoundedCornerShape(13.dp))
                    .clickable(onClick = onCreateTarget),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                    Text("Create Custom Target", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WEEKLY CONSISTENCY  — richer chart with gradient fill
// ─────────────────────────────────────────────────────────────
@Composable
private fun WeeklyConsistencyCard(data: List<DayProgress>) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Weekly Consistency", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                    Text("Your study activity this week", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BpscColors.AccentLight)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Whatshot, null, tint = BpscColors.Accent, modifier = Modifier.size(14.dp))
                    Text("7 streak", style = MaterialTheme.typography.labelSmall, color = BpscColors.Accent, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                // Y-axis
                Column(
                    modifier            = Modifier.width(28.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("90", "60", "30", "0").forEach { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
                    }
                }

                // Chart canvas — gradient fill + smooth bezier
                val maxVal = 100f
                Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val w      = size.width
                    val h      = size.height
                    val count  = data.size
                    val stepX  = w / (count - 1).toFloat()
                    val points = data.mapIndexed { i, d ->
                        Offset(i * stepX, h - (d.score / maxVal) * h)
                    }

                    // Gradient fill under line
                    val fillPath = Path().apply {
                        moveTo(points.first().x, h)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, h)
                        close()
                    }
                    drawPath(
                        fillPath,
                        Brush.verticalGradient(
                            listOf(BpscColors.Primary.copy(0.18f), BpscColors.Primary.copy(0.0f)),
                            startY = 0f, endY = h
                        )
                    )

                    // Smooth bezier line
                    val linePath = Path()
                    points.forEachIndexed { i, pt ->
                        if (i == 0) linePath.moveTo(pt.x, pt.y)
                        else {
                            val prev  = points[i - 1]
                            val cpX1  = prev.x + (pt.x - prev.x) * 0.5f
                            val cpX2  = pt.x  - (pt.x - prev.x) * 0.5f
                            linePath.cubicTo(cpX1, prev.y, cpX2, pt.y, pt.x, pt.y)
                        }
                    }
                    drawPath(linePath, Color(0xFF1565C0), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

                    // Grid lines
                    listOf(0.25f, 0.5f, 0.75f).forEach { r ->
                        drawLine(Color(0xFFF0F0F0), Offset(0f, h * r), Offset(w, h * r), 1.dp.toPx())
                    }

                    // Dots — today bigger + orange
                    points.forEachIndexed { i, pt ->
                        val isToday = i == count - 1
                        drawCircle(if (isToday) BpscColors.Accent else BpscColors.Primary, if (isToday) 7.dp.toPx() else 4.5.dp.toPx(), pt)
                        drawCircle(Color.White, if (isToday) 3.5.dp.toPx() else 2.dp.toPx(), pt)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // X-axis labels
            Row(
                modifier              = Modifier.fillMaxWidth().padding(start = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEachIndexed { i, d ->
                    val isToday = i == data.size - 1
                    Text(
                        d.day,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = if (isToday) BpscColors.Primary else BpscColors.TextSecondary,
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize   = 10.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// QUICK ACCESS  — taller large cards, richer small cards
// ─────────────────────────────────────────────────────────────
@Composable
private fun QuickAccessSection(navController: NavHostController, bookmarkCount: Int) {
    val bkCount = 3

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        SectionHeader(title = "Quick Access")
        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                LargeQuickCard(
                    "Current Affairs", "Today's Updates",
                    Icons.Rounded.Newspaper,
                    listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF1976D2)),
                    Modifier.fillMaxWidth()
                ) { navController.navigate(Screen.CurrentAffairs.route) }

                if (bkCount > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFF8E1))
                            .border(1.5.dp, Color.White, RoundedCornerShape(20.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Rounded.Bookmark, null, tint = BpscColors.CoinGold, modifier = Modifier.size(11.dp))
                        Text("$bkCount", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    }
                }
            }
            LargeQuickCard(
                "My Courses", "Continue Learning",
                Icons.Rounded.MenuBook,
                listOf(Color(0xFFBF360C), Color(0xFFE64A19), Color(0xFFFF5722)),
                Modifier.weight(1f)
            ) { navController.navigate(Screen.MyLearning.route) }
        }

        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallQuickCard("Active\nRecall",  Icons.Rounded.Psychology,  BpscColors.PrimaryLight,    BpscColors.Primary,     Modifier.weight(1f)) { navController.navigate(Screen.ActiveRecall.route) }
            SmallQuickCard("Mock\nTests",     Icons.Rounded.Assignment,  Color(0xFFFFF4EC),           BpscColors.Accent,      Modifier.weight(1f)) { navController.navigate(Screen.MockTests.route) }
            SmallQuickCard("Group\nStudy",    Icons.Rounded.Groups,      Color(0xFFE8FDF4),           BpscColors.Success,     Modifier.weight(1f)) { navController.navigate(Screen.ReadingRooms.route) }
        }

        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallQuickCard("Job\nAlerts",  Icons.Rounded.Work,     Color(0xFFFFF0EA), Color(0xFFE67E22), Modifier.weight(1f)) { navController.navigate(Screen.JobVacancies.route) }
            SmallQuickCard("Downloads",   Icons.Rounded.Download, Color(0xFFEDE7F6), Color(0xFF7E57C2), Modifier.weight(1f)) { navController.navigate(Screen.Downloads.route) }
            SmallQuickCard("Premium",     Icons.Rounded.Star,     Color(0xFFFFF8E1), BpscColors.CoinGold, Modifier.weight(1f)) { navController.navigate(Screen.Subscription.route) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// RECOMMENDED SECTION
// ─────────────────────────────────────────────────────────────
@Composable
private fun RecommendedSection(courses: List<Course>, navController: NavHostController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Courses", "Topics", "Library")

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Recommended for You")
            TextButton(onClick = { navController.navigate(Screen.MyLearning.route) }) {
                Text("See all", color = BpscColors.Primary, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Tabs — pill style
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEachIndexed { index, tab ->
                val sel = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (sel) BpscColors.Primary else Color.White)
                        .border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(22.dp))
                        .clickable { selectedTab = index }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(tab, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal)
                }
            }
        }

        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(courses) { course ->
                CourseCard(course = course, onClick = { navController.navigate(Screen.CourseDetail.createRoute(course.id)) })
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, onClick: () -> Unit) {
    val subjectColors = mapOf(
        "All Subjects" to Pair(Color(0xFF1A6FE8), Color(0xFFE8F0FD)),
        "Bihar GK"     to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)),
        "Polity"       to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
        "Economy"      to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
        "Geography"    to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
        "History"      to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
    )
    val (accent, bg) = subjectColors[course.subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)
    val progress     = if (course.totalLessons > 0) course.completedLessons.toFloat() / course.totalLessons else 0f

    Card(
        modifier  = Modifier.width(168.dp).clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            Box(
                modifier         = Modifier.fillMaxWidth().height(96.dp).background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MenuBook, null, tint = accent, modifier = Modifier.size(38.dp))
                if (course.isPaid) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd).padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BpscColors.CoinGold)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("PRO", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(course.title, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(course.instructor, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress  = { progress },
                        modifier  = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color     = accent, trackColor = bg
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${(progress * 100).toInt()}% complete", style = MaterialTheme.typography.labelSmall, color = accent)
                } else {
                    Text(if (course.isPaid) "₹${course.price}" else "Free", style = MaterialTheme.typography.titleMedium, color = if (course.isPaid) BpscColors.Accent else BpscColors.Success, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// MY SCHEDULE  — card with left color accent stripe
// ─────────────────────────────────────────────────────────────
data class ScheduleItem(val title: String, val time: String, val icon: ImageVector, val isLive: Boolean = false, val color: Color)

@Composable
private fun MyScheduleSection(navController: NavHostController) {
    val scheduleItems = listOf(
        ScheduleItem("LIVE Class: General Science", "Today at 5:00 PM",      Icons.Rounded.PlayCircle, isLive = true, color = Color(0xFFE74C3C)),
        ScheduleItem("Mock Test: BPSC Prelims",      "Tomorrow at 10:00 AM", Icons.Rounded.Assignment,               color = BpscColors.Accent),
        ScheduleItem("Community & Doubt Solving",    "Join 15,000+ students",Icons.Rounded.Forum,                    color = BpscColors.Primary),
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(title = "My Schedule", subtitle = "Upcoming events")
        Spacer(Modifier.height(12.dp))

        scheduleItems.forEach { item ->
            Card(
                modifier  = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape     = RoundedCornerShape(18.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left accent stripe
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(72.dp)
                            .background(item.color, RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    )
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(item.color.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, null, tint = item.color, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.time,  style = MaterialTheme.typography.bodyMedium,  color = BpscColors.TextSecondary)
                        }
                        if (item.isLive) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFE74C3C))
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ACHIEVEMENTS  — larger circles with glow ring animation
// ─────────────────────────────────────────────────────────────
data class Achievement(val emoji: String, val label: String, val color: Color, val earned: Boolean)

@Composable
private fun AchievementsSection() {
    val achievements = listOf(
        Achievement("🔥", "7 Day\nStreak",  BpscColors.Accent,   true),
        Achievement("🏆", "Top 10\nRank",   BpscColors.CoinGold, true),
        Achievement("📚", "100\nTopics",    BpscColors.Primary,  true),
        Achievement("⚡", "Speed\nStar",    Color(0xFF9B59B6),   false),
        Achievement("🎯", "Perfect\nScore", BpscColors.Success,  false),
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(title = "Achievements")
        Spacer(Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(achievements) { a ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (a.earned) a.color.copy(0.12f) else BpscColors.Divider)
                            .border(2.dp, if (a.earned) a.color.copy(0.6f) else BpscColors.TextHint.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(a.emoji, fontSize = 28.sp, modifier = Modifier.alpha(if (a.earned) 1f else 0.3f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        a.label,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = if (a.earned) BpscColors.TextPrimary else BpscColors.TextHint,
                        fontWeight = if (a.earned) FontWeight.SemiBold else FontWeight.Normal,
                        lineHeight = 14.sp,
                        textAlign  = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CREATE TARGET BOTTOM SHEET  — unchanged logic, refined UI
// ─────────────────────────────────────────────────────────────
@Composable
fun CreateTargetSheet(onDismiss: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val addedItems = remember { mutableStateListOf<String>() }

    Box(
        modifier         = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().clickable(enabled = false, onClick = {}),
            shape     = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(BpscColors.Divider).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(20.dp))
                Text("Create Your Daily Target", style = MaterialTheme.typography.headlineSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                Text("Build your plan, one task at a time.", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BpscColors.Surface).padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value         = inputText,
                        onValueChange = { inputText = it },
                        modifier      = Modifier.weight(1f).padding(vertical = 12.dp),
                        textStyle     = MaterialTheme.typography.bodyLarge.copy(color = BpscColors.TextPrimary),
                        decorationBox = { inner ->
                            if (inputText.isEmpty()) Text("What's your next task?", color = BpscColors.TextHint, style = MaterialTheme.typography.bodyLarge)
                            inner()
                        },
                        singleLine = true
                    )
                    IconButton(
                        onClick  = { if (inputText.isNotBlank() && addedItems.size < 5) { addedItems.add(inputText.trim()); inputText = "" } },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(BpscColors.Primary)
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("${addedItems.size} / 5 targets added", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)

                if (addedItems.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    addedItems.forEachIndexed { index, item ->
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                            }
                            Text(item, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { addedItems.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Close, null, tint = BpscColors.TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick  = { if (addedItems.isNotEmpty()) onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled  = addedItems.isNotEmpty(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Text("Set Target (${addedItems.size} items)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SIDE DRAWER  — unchanged logic, polished UI
// ─────────────────────────────────────────────────────────────
@Composable
private fun BpscDrawer(
    user: com.example.bpscnotes.data.remote.dto.UserDto,
    onClose: () -> Unit,
    navController: NavHostController
) {
    val menuItems = listOf(
        Triple(Icons.Rounded.TrackChanges, "Daily Targets Module",  Screen.DailyTargets.route),
        Triple(Icons.Rounded.Quiz,         "Daily Quizzes",         Screen.DailyQuiz.route),
        Triple(Icons.Rounded.Newspaper,    "Daily Current Affairs", Screen.CurrentAffairs.route),
        Triple(Icons.Rounded.Star,         "Paid Content",          Screen.Subscription.route),
        Triple(Icons.Rounded.Download,     "Downloads",             Screen.Downloads.route),
        Triple(Icons.Rounded.Work,         "Latest Govt Vacancies", Screen.JobVacancies.route),
        Triple(Icons.Rounded.Psychology,   "Active Recall",         Screen.ActiveRecall.route),
        Triple(Icons.Rounded.Groups,       "E- Library",            Screen.ReadingRooms.route),
        Triple(Icons.Rounded.Notifications,"Notification Settings", Screen.NotificationSettings.route),
        Triple(Icons.Rounded.Settings,     "Settings",              Screen.Settings.route),
    )
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        drawerShape          = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = Color.White,
        windowInsets         = WindowInsets(0, 0, 0, 0),
        modifier             = Modifier.width(300.dp).fillMaxHeight()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF051D56), Color(0xFF0D47A1), Color(0xFF1565C0)),
                            Offset(0f, 0f), Offset(300f, 200f)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape)
                            .background(Color.White.copy(0.2f))
                            .border(1.5.dp, Color.White.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = user.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                            style      = MaterialTheme.typography.titleLarge,
                            color      = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(user.name,                  style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(user.email ?: user.mobile,  style = MaterialTheme.typography.bodyMedium,  color = Color.White.copy(0.72f))
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFFB300).copy(0.2f))
                                .border(0.5.dp, Color(0xFFFFD54F).copy(0.4f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🪙", fontSize = 11.sp)
                            Text("${user.coinBalance} Coins", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // ── Menu items
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(vertical = 4.dp)) {
                    menuItems.forEach { (icon, label, route) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClose(); navController.navigate(route) }
                                .padding(horizontal = 20.dp, vertical = 13.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(BpscColors.PrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                            }
                            Text(label, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.KeyboardArrowRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(16.dp))
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (scrollState.value < scrollState.maxValue) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp).align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(Color.White.copy(0f), Color.White)))
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = BpscColors.TextHint, modifier = Modifier.size(20.dp).align(Alignment.Center))
                    }
                }

                if (scrollState.maxValue > 0) {
                    val thumbOffset by remember { derivedStateOf { scrollState.value.toFloat() / scrollState.maxValue.toFloat() } }
                    Box(modifier = Modifier.align(Alignment.CenterEnd).width(3.dp).fillMaxHeight().padding(vertical = 8.dp).background(BpscColors.Divider, RoundedCornerShape(2.dp))) {
                        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f).align(Alignment.TopStart)
                            .offset(y = with(LocalDensity.current) { (scrollState.value * 0.3f).toDp() })
                            .background(BpscColors.Primary.copy(0.4f), RoundedCornerShape(2.dp)))
                    }
                }
            }

            // ── Footer
            Column(
                modifier              = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(0.dp)
            ) {
                HorizontalDivider(color = BpscColors.Divider)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("BPSCNotes", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("v1.0.0", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                        Text("·", color = BpscColors.TextHint)
                        Text("Privacy", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Primary, modifier = Modifier.clickable { })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick  = { onClose(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, Color(0xFFE74C3C)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE74C3C))
                ) {
                    Icon(Icons.Rounded.Logout, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SHARED COMPONENTS
// ─────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}

@Composable
private fun LargeQuickCard(title: String, subtitle: String, icon: ImageVector, gradient: List<Color>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier  = modifier.height(118.dp).clickable(onClick = onClick),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.linearGradient(gradient, Offset(0f, 0f), Offset(200f, 200f)))
                .padding(16.dp)
        ) {
            // Subtle watermark circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.08f))
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier            = Modifier.fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(title,    style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall,  color = Color.White.copy(0.75f))
                }
            }
        }
    }
}

@Composable
private fun SmallQuickCard(title: String, icon: ImageVector, iconBg: Color, iconTint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier  = modifier.height(92.dp).clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp))
            }
            Text(title, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp)
        }
    }
}