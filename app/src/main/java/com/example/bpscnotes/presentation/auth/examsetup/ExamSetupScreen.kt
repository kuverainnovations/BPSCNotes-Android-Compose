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
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.ExamDto
import com.example.bpscnotes.presentation.navigation.Routes.Screen

@Composable
fun ExamSetupScreen(
    navController: NavHostController,
    viewModel: ExamSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isDone) {
        if (state.isDone) {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.ExamSetup.route) { inclusive = true }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF2F4F8))) {
        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            },
            label = "exam_step"
        ) { step ->
            when (step) {
                ExamSetupStep.SELECT_PRIMARY    -> Step1PrimaryExam(state, viewModel)
                ExamSetupStep.SELECT_SECONDARY  -> Step2SecondaryExams(state, viewModel)
                ExamSetupStep.SELECT_PREP_LEVEL -> Step3PrepLevel(state, viewModel)
                ExamSetupStep.ALL_SET           -> Unit
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SHARED HEADER
// ─────────────────────────────────────────────────────────────
@Composable
private fun ExamSetupHeader(step: Int, title: String, subtitle: String, emoji: String) {
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
            Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                repeat(3) { i ->
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
                    if (i < 2) Box(Modifier.width(60.dp).height(2.dp).background(if (step > i + 1) Color(0xFF4CAF50) else Color.White.copy(0.3f)))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("$emoji $title", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 1 — Choose Primary Exam
// ─────────────────────────────────────────────────────────────
@Composable
private fun Step1PrimaryExam(state: ExamSetupUiState, vm: ExamSetupViewModel) {
    var search   by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val cats     = listOf("All", "BPSC", "Bihar State", "Central Govt")
    val filtered = state.exams.filter { e ->
        (search.isEmpty() || e.name.contains(search, true) || e.fullName.contains(search, true)) &&
                (category == "All" || e.category?.contains(category, true) == true)
    }

    Column(Modifier.fillMaxSize()) {
        ExamSetupHeader(1, "Choose Your Primary Exam", "We'll personalize content specifically for your exam", "🎯")

        OutlinedTextField(
            value = search, onValueChange = { search = it },
            placeholder = { Text("Search exam...", color = BpscColors.TextHint) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = BpscColors.TextHint) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent, focusedBorderColor = BpscColors.Primary
            ), singleLine = true
        )

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

        when {
            state.isLoadingExams -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = BpscColors.Primary) }
            state.examsError != null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚠️", fontSize = 40.sp)
                    Text("Failed to load exams", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = vm::loadExams) { Text("Retry") }
                }
            }
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.name }) { exam ->
                    ExamListCard(exam, state.selectedPrimary?.name == exam.name, false) { vm.selectPrimaryExam(exam) }
                }
            }
        }

        Box(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding()) {
            Button(onClick = { vm.proceedFromPrimary() }, enabled = state.selectedPrimary != null,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary, disabledContainerColor = BpscColors.Divider)) {
                Text("Next →", style = MaterialTheme.typography.titleMedium,
                    color = if (state.selectedPrimary != null) Color.White else BpscColors.TextHint)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 2 — Add More Exams (Optional)
// ─────────────────────────────────────────────────────────────
@Composable
private fun Step2SecondaryExams(state: ExamSetupUiState, vm: ExamSetupViewModel) {
    var search   by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val cats     = listOf("All", "BPSC", "Bihar State", "Central Govt")
    val filtered = state.exams.filter { e ->
        e.name != state.selectedPrimary?.name &&
                (search.isEmpty() || e.name.contains(search, true)) &&
                (category == "All" || e.category?.contains(category, true) == true)
    }

    Column(Modifier.fillMaxSize()) {
        ExamSetupHeader(2, "Add More Exams (Optional)", "Prepare for multiple exams simultaneously", "📚")

        OutlinedTextField(value = search, onValueChange = { search = it },
            placeholder = { Text("Search exam...", color = BpscColors.TextHint) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = BpscColors.TextHint) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent, focusedBorderColor = BpscColors.Primary
            ), singleLine = true)

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

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F0FE)).padding(12.dp),
                    Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                    Text(state.selectedPrimary?.emoji ?: "🎯", fontSize = 20.sp)
                    Column(Modifier.weight(1f)) {
                        Text("Primary: ${state.selectedPrimary?.name ?: ""}", style = MaterialTheme.typography.titleSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                        Text("Content will be prioritized for this exam", style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
                    }
                    Icon(Icons.Rounded.Star, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp))
                }
            }
            items(filtered, key = { it.name }) { exam ->
                val isSelected = state.selectedSecondary.any { it.name == exam.name }
                ExamListCard(exam, isSelected, true) { vm.toggleSecondaryExam(exam) }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 16.dp).navigationBarsPadding()) {
            if (state.selectedSecondary.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 16.dp, vertical = 12.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(state.selectedPrimary?.name ?: "", style = MaterialTheme.typography.titleSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                        Text(state.selectedSecondary.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { vm.proceedFromSecondary() }, shape = RoundedCornerShape(10.dp)) { Text("Next →") }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {}, modifier = Modifier.height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("← Back") }
                    Button(onClick = { vm.proceedFromSecondary() }, modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                        Text("Skip for now →", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 3 — Prep Level + Target Year + Personalization
// ─────────────────────────────────────────────────────────────
@Composable
private fun Step3PrepLevel(state: ExamSetupUiState, vm: ExamSetupViewModel) {
    var targetYear by remember { mutableStateOf(2026) }
    val years = listOf(2026 to "This year 🔥", 2027 to "Next year", 2028 to "Long term")

    Column(Modifier.fillMaxSize()) {
        ExamSetupHeader(3, "Your Preparation Level", "This helps us recommend the right content", "📊")

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Exam Plan card
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
                                Text(sec.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
                                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.TextHint.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("SECONDARY", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                    if (state.selectedPrimary?.subjects?.isNotEmpty() == true) {
                        Text("📚 Subjects you'll study:", style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.selectedPrimary!!.subjects) { sub ->
                                Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, BpscColors.Divider, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                    Text(sub, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Prep Level header
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📊", fontSize = 18.sp)
                Text("Your Preparation Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
            PREP_LEVELS.forEach { level ->
                val isSelected = state.selectedPrepLevel?.id == level.id
                Card(Modifier.fillMaxWidth().clickable { vm.selectPrepLevel(level) }
                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(if (isSelected) BpscColors.PrimaryLight else Color.White),
                    elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 1.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.spacedBy(14.dp), Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(if (isSelected) BpscColors.Primary.copy(0.12f) else BpscColors.Surface), Alignment.Center) { Text(level.emoji, fontSize = 26.sp) }
                        Column(Modifier.weight(1f)) {
                            Text(level.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(level.subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        }
                        if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Primary, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Target Year
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 18.sp)
                Text("Target Exam Year", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
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
                        "💼" to "Job alerts for your target exam category", "🏆" to "Subscription plans with your exam content first").forEach { (e, t) ->
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {}, modifier = Modifier.height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("← Back") }
                Button(onClick = { vm.saveAndFinish() }, enabled = state.selectedPrepLevel != null && !state.isSaving,
                    modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary, disabledContainerColor = BpscColors.Divider)) {
                    if (state.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Start Preparing 🚀", style = MaterialTheme.typography.titleMedium, color = if (state.selectedPrepLevel != null) Color.White else BpscColors.TextHint)
                }
            }
            state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// EXAM LIST CARD
// ─────────────────────────────────────────────────────────────
@Composable
private fun ExamListCard(exam: ExamDto, isSelected: Boolean, isMultiSelect: Boolean, note: String? = null, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick).border(if (isSelected) 2.dp else 1.dp, if (isSelected) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(if (isSelected) BpscColors.PrimaryLight else Color.White), elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 1.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) BpscColors.Primary.copy(0.1f) else BpscColors.Surface), Alignment.Center) { Text(exam.emoji, fontSize = 22.sp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(exam.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
                    note?.let { Box(Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.Primary).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) } }
                }
                Text(exam.fullName, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    exam.category?.let { Box(Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.Primary.copy(0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(it, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 10.sp) } }
                    if (exam.studentCount > 0) { Text("·", color = BpscColors.TextHint, fontSize = 10.sp); Icon(Icons.Rounded.Group, null, modifier = Modifier.size(10.dp), tint = BpscColors.TextHint); Text("${formatCount(exam.studentCount)} students", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp) }
                    if (exam.prepMonths > 0) { Text("·", color = BpscColors.TextHint, fontSize = 10.sp); Text("~${exam.prepMonths}m prep", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp) }
                }
            }
            exam.difficulty?.let { diff ->
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(when (diff.lowercase()) { "hard" -> Color(0xFFFFEBEE); "medium" -> Color(0xFFFFF8E1); else -> Color(0xFFE8F5E9) }).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(diff, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = when (diff.lowercase()) { "hard" -> Color(0xFFC62828); "medium" -> Color(0xFF856404); else -> Color(0xFF2E7D32) }, fontWeight = FontWeight.Bold)
                }
            }
            if (isMultiSelect) {
                Box(Modifier.size(20.dp).clip(if (isSelected) CircleShape else RoundedCornerShape(4.dp)).background(if (isSelected) BpscColors.Primary else Color.Transparent).border(1.5.dp, if (isSelected) BpscColors.Primary else BpscColors.TextHint, if (isSelected) CircleShape else RoundedCornerShape(4.dp)), Alignment.Center) {
                    if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            } else {
                Box(Modifier.size(20.dp).clip(CircleShape).border(1.5.dp, if (isSelected) BpscColors.Primary else BpscColors.TextHint, CircleShape), Alignment.Center) {
                    if (isSelected) Box(Modifier.size(10.dp).clip(CircleShape).background(BpscColors.Primary))
                }
            }
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 100_000 -> "${count / 100_000}.${(count % 100_000) / 10_000}L+"
    count >= 1000    -> "${count / 1000}K+"
    else             -> "$count"
}