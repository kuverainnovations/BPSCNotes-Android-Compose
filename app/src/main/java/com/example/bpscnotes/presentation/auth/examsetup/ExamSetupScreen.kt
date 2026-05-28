package com.example.bpscnotes.presentation.auth.examsetup

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.ExamDto
import com.example.bpscnotes.presentation.navigation.Routes.Screen

// ════════════════════════════════════════════════════════════
// ExamSetupScreen — SIMPLIFIED (2 steps only)
//
// STEP 1: Select exams (combined primary + secondary on ONE screen)
//         - First tap → marked as PRIMARY
//         - More taps → SECONDARY
//         - "Next →" enabled after at least 1 selected
//
// STEP 2: Target Year + Personalization preview ("What we'll do for you")
//         - 2026 / 2027 / 2028 selection
//         - "Start Preparing 🚀"
//
// REMOVED: Beginner / Intermediate / Advanced prep level (entire section removed)
// ════════════════════════════════════════════════════════════

@Composable
fun ExamSetupScreen(
    navController: NavHostController,
    viewModel: ExamSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current

    LaunchedEffect(state.isDone) {
        if (state.isDone) {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.ExamSetup.route) { inclusive = true }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF2F4F8))) {
        AnimatedContent(
            targetState  = state.currentStep,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            },
            label = "exam_step"
        ) { step ->
            when (step) {
                ExamSetupStep.SELECT_PRIMARY,
                ExamSetupStep.SELECT_SECONDARY  -> StepSelectExams(state, viewModel)
                ExamSetupStep.SELECT_PREP_LEVEL -> StepTargetYear(state, viewModel)
                ExamSetupStep.ALL_SET           -> Unit
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SHARED HEADER — blue gradient with 2-step stepper
// ─────────────────────────────────────────────────────────────
@Composable
private fun SetupHeader(step: Int, title: String, subtitle: String, emoji: String) {
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(
                listOf(Color(0xFF0D2B8E), Color(0xFF1565C0), Color(0xFF1976D2)),
                Offset(0f, 0f), Offset(500f, 300f)
            ))
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column {
            // 2-step stepper (was 3 — prep level removed)
            Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                repeat(2) { i ->
                    val stepNum  = i + 1
                    val isDone   = step > stepNum
                    val isActive = step == stepNum
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(when { isDone -> Color(0xFF4CAF50); isActive -> Color.White; else -> Color.White.copy(0.2f) })
                            .border(if (!isDone && !isActive) 1.5.dp else 0.dp, Color.White.copy(0.4f), CircleShape),
                        Alignment.Center
                    ) {
                        if (isDone) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        else Text("$stepNum", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold,
                            color = if (isActive) BpscColors.Primary else Color.White.copy(0.6f))
                    }
                    if (i < 1) Box(Modifier.width(60.dp).height(2.dp).background(if (step > i + 1) Color(0xFF4CAF50) else Color.White.copy(0.3f)))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("$emoji $title", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 1: Select Exams — Primary + Secondary on ONE screen
// ─────────────────────────────────────────────────────────────
@Composable
private fun StepSelectExams(state: ExamSetupUiState, vm: ExamSetupViewModel) {
    val str = LocalStrings.current
    var search   by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val cats     = listOf("All", "BPSC", "Bihar State", "Central Govt")
    val filtered = state.exams.filter { e ->
        (search.isEmpty() || e.name.contains(search, true) || e.fullName.contains(search, true)) &&
                (category == "All" || e.category?.contains(category, true) == true)
    }

    Column(Modifier.fillMaxSize()) {
        SetupHeader(
            step     = 1,
            title    = "Choose Your Exams",
            subtitle = "Tap once to set primary · Tap again to add secondary",
            emoji    = "🎯"
        )

        // Info banner explaining dual-tap behaviour
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFE8F0FE))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("💡", fontSize = 14.sp)
            Text(
                "First exam you tap becomes your PRIMARY. Tap more to add secondary exams.",
                style = MaterialTheme.typography.bodySmall,
                color = BpscColors.Primary
            )
        }

        // Search
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            placeholder = { Text("Search exam...", color = BpscColors.TextHint) },
            leadingIcon  = { Icon(Icons.Rounded.Search, null, tint = BpscColors.TextHint) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent, focusedBorderColor = BpscColors.Primary
            ), singleLine = true
        )

        // Category chips
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            items(cats) { cat ->
                val sel = cat == category
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) BpscColors.Primary else Color.White)
                    .border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
                    .clickable { category = cat }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(cat, style = MaterialTheme.typography.bodyMedium,
                        color = if (sel) Color.White else BpscColors.TextPrimary,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        // Exam list
        when {
            state.isLoadingExams -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = BpscColors.Primary) }
            state.examsError != null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚠️", fontSize = 40.sp)
                    Text("Failed to load exams", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = vm::loadExams) { Text(str.retry) }
                }
            }
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.name }) { exam ->
                    val isPrimary  = state.selectedPrimary?.name == exam.name
                    val isSecondary = state.selectedSecondary.any { it.name == exam.name }
                    ExamTile(
                        exam        = exam,
                        isPrimary   = isPrimary,
                        isSecondary = isSecondary,
                        onClick     = {
                            when {
                                // FIX: Clicking primary again deselects it so user can re-choose
                                isPrimary   -> vm.deselectPrimaryExam()
                                // Clicking secondary again deselects it
                                isSecondary -> vm.toggleSecondaryExam(exam)
                                // No primary yet → first tap sets primary
                                state.selectedPrimary == null -> vm.selectPrimaryExam(exam)
                                // Primary set, not secondary → add as secondary
                                else        -> vm.toggleSecondaryExam(exam)
                            }
                        }
                    )
                }
            }
        }

        // Bottom bar — shows selection summary + Next button
        Box(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.selectedPrimary != null) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 14.dp, vertical = 10.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                        Text(state.selectedPrimary!!.emoji, fontSize = 16.sp)
                        Column(Modifier.weight(1f)) {
                            Text(state.selectedPrimary!!.name, style = MaterialTheme.typography.titleSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                            if (state.selectedSecondary.isNotEmpty())
                                Text("+ ${state.selectedSecondary.joinToString(", ") { it.name }}", style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
                        }
                    }
                }
                Button(
                    onClick  = { vm.proceedFromPrimary() },
                    enabled  = state.selectedPrimary != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary, disabledContainerColor = BpscColors.Divider)
                ) {
                    Text("Next →", style = MaterialTheme.typography.titleMedium,
                        color = if (state.selectedPrimary != null) Color.White else BpscColors.TextHint)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 2: Target Year + Personalization (NO prep level)
// ─────────────────────────────────────────────────────────────
@Composable
private fun StepTargetYear(state: ExamSetupUiState, vm: ExamSetupViewModel) {
    var targetYear by remember { mutableStateOf(2026) }
    val years = listOf(2026 to "This year 🔥", 2027 to "Next year", 2028 to "Long term")

    Column(Modifier.fillMaxSize()) {
        SetupHeader(step = 2, title = "Target Exam Year", subtitle = "We'll tailor your study plan accordingly", emoji = "📅")

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Exam Plan summary
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📋", fontSize = 18.sp)
                        Text("Your Exam Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    state.selectedPrimary?.let { pri ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BpscColors.PrimaryLight).padding(12.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                            Text(pri.emoji, fontSize = 20.sp)
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(pri.name, style = MaterialTheme.typography.titleSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.Primary).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("PRIMARY", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                    state.selectedSecondary.forEach { sec ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BpscColors.Surface).padding(12.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                            Text(sec.emoji, fontSize = 20.sp)
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(sec.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.TextHint.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("SECONDARY", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            // Year selector
            Text("When is your target exam?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                years.forEach { (year, label) ->
                    val isSel = targetYear == year
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (isSel) BpscColors.Primary else Color.White)
                        .border(1.dp, if (isSel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(14.dp))
                        .clickable { targetYear = year }.padding(vertical = 14.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$year", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (isSel) Color.White else BpscColors.TextPrimary)
                            Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White.copy(0.8f) else BpscColors.TextSecondary)
                        }
                    }
                }
            }

            // Personalization preview
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Color(0xFFEEF2FF)), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✨ What we'll personalize for you", style = MaterialTheme.typography.titleSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                    listOf("📚" to "Courses recommended for your exam", "🎯" to "Daily targets based on your syllabus",
                        "📰" to "Current affairs filtered for your exam", "❓" to "Quizzes from your exam's previous papers",
                        "💼" to "Job alerts for your target exam category", "🏆" to "Subscription plans with your exam content first"
                    ).forEach { (e, t) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(e, fontSize = 14.sp)
                            Text(t, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        }
                    }
                }
            }
        }

        // Bottom CTA
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.goBackToExamSelection() }, modifier = Modifier.height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("← Back") }
                Button(
                    onClick  = { vm.saveAndFinish(/*targetYear = targetYear*/) },
                    enabled  = !state.isSaving,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary, disabledContainerColor = BpscColors.Divider)
                ) {
                    if (state.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Start Preparing 🚀", style = MaterialTheme.typography.titleMedium)
                }
            }
            state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// EXAM TILE — shows PRIMARY / SECONDARY / unselected state
// ─────────────────────────────────────────────────────────────
@Composable
private fun ExamTile(exam: ExamDto, isPrimary: Boolean, isSecondary: Boolean, onClick: () -> Unit) {
    val selected  = isPrimary || isSecondary
    val bgColor   = when { isPrimary -> BpscColors.PrimaryLight; isSecondary -> Color(0xFFF0F9F4); else -> Color.White }
    val border    = when { isPrimary -> 2.dp; isSecondary -> 1.5.dp; else -> 1.dp }
    val borderColor = when { isPrimary -> BpscColors.Primary; isSecondary -> BpscColors.Success; else -> BpscColors.Divider }

    Card(Modifier.fillMaxWidth().clickable(onClick = onClick).border(border, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(bgColor), elevation = CardDefaults.cardElevation(if (selected) 3.dp else 1.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (isPrimary) BpscColors.Primary.copy(0.1f) else if (isSecondary) BpscColors.Success.copy(0.08f) else BpscColors.Surface), Alignment.Center) { Text(exam.emoji, fontSize = 22.sp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(exam.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
                    if (isPrimary) {
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.Primary).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("PRIMARY · tap to change", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    if (isSecondary) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.Success.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("SECONDARY", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) }
                }
                Text(exam.fullName, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    exam.category?.let { Box(Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.Primary.copy(0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(it, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 10.sp) } }
                    if (exam.studentCount > 0) { Text("·", color = BpscColors.TextHint, fontSize = 10.sp); Icon(Icons.Rounded.Group, null, modifier = Modifier.size(10.dp), tint = BpscColors.TextHint); Text("${formatCount(exam.studentCount)} students", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp) }
                    if (exam.prepMonths > 0) { Text("·", color = BpscColors.TextHint, fontSize = 10.sp); Text("~${exam.prepMonths}m prep", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp) }
                }
            }
            // Selection indicator
            Box(Modifier.size(22.dp).clip(CircleShape)
                .background(if (isPrimary) BpscColors.Primary else if (isSecondary) BpscColors.Success else Color.Transparent)
                .border(1.5.dp, if (selected) Color.Transparent else BpscColors.TextHint, CircleShape), Alignment.Center) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 100_000 -> "${count / 100_000}.${(count % 100_000) / 10_000}L+"
    count >= 1000    -> "${count / 1000}K+"
    else             -> "$count"
}