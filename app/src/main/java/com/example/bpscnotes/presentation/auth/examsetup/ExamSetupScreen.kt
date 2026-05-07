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
import androidx.compose.ui.draw.*
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
import kotlin.collections.isNotEmpty

@Composable
fun ExamSetupScreen(
    navController: NavHostController,
    viewModel: ExamSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Navigate to Main when done
    LaunchedEffect(state.isDone) {
        if (state.isDone) {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.ExamSetup.route) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Animated step transitions
        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            },
            label = "step"
        ) { step ->
            when (step) {
                ExamSetupStep.SELECT_PRIMARY -> PrimaryExamStep(
                    state     = state,
                    onSelect  = { viewModel.selectPrimaryExam(it) },
                    onNext    = { viewModel.proceedFromPrimary() },
                    onSkip    = { viewModel.skip() },
                    onRetry   = { viewModel.loadExams() }
                )
                ExamSetupStep.SELECT_SECONDARY -> SecondaryExamStep(
                    state    = state,
                    onToggle = { viewModel.toggleSecondaryExam(it) },
                    onNext   = { viewModel.proceedFromSecondary() },
                    onBack   = { /* handled by state */ }
                )
                ExamSetupStep.SELECT_PREP_LEVEL -> PrepLevelStep(
                    state    = state,
                    onSelect = { viewModel.selectPrepLevel(it) },
                    onFinish = { viewModel.saveAndFinish() },
                    onSkip   = { viewModel.skip() }
                )
                ExamSetupStep.ALL_SET -> Unit
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 1 — Choose Primary Exam
// ─────────────────────────────────────────────────────────────

@Composable
private fun PrimaryExamStep(
    state: ExamSetupUiState,
    onSelect: (ExamDto) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(
                    listOf(Color(0xFF051D56), Color(0xFF0D47A1), Color(0xFF1976D2)),
                    Offset(0f, 0f), Offset(400f, 300f)
                ))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column {
                // Step indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    StepDots(current = 0, total = 3)
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("🎯", fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Which exam are you\npreparing for?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp
                )
                Text(
                    "Select your primary exam target",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f), modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Content
        when {
            state.isLoadingExams -> {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                }
            }
            state.examsError != null -> {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 40.sp)
                        Text("Couldn't load exams", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
                        Text(state.examsError!!, textAlign = TextAlign.Center, color = BpscColors.TextSecondary)
                        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text("Try Again") }
                    }
                }
            }
            else -> {
                // Group exams by category
                val grouped = state.exams.groupBy { it.category }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (category, exams) ->
                        item(key = "header_$category") {
                            Text(
                                category.uppercase(),
                                style     = MaterialTheme.typography.labelSmall,
                                color     = BpscColors.TextHint,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier  = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(exams, key = { it.name }) { exam ->
                            val isSelected = state.selectedPrimary?.name == exam.name
                            ExamSelectionCard(
                                exam       = exam,
                                isSelected = isSelected,
                                onClick    = { onSelect(exam) },
                                isMultiSelect = false
                            )
                        }
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }

        // Bottom CTA
        Box(
            modifier = Modifier.fillMaxWidth().background(Color.White)
                .padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding()
        ) {
            Button(
                onClick  = onNext,
                enabled  = state.selectedPrimary != null && !state.isLoadingExams,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Text(
                    text  = if (state.selectedPrimary != null)
                        "Continue with ${state.selectedPrimary!!.emoji} ${state.selectedPrimary!!.name}"
                    else "Select an exam to continue",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 2 — Add More Exams (optional)
// ─────────────────────────────────────────────────────────────

@Composable
private fun SecondaryExamStep(
    state: ExamSetupUiState,
    onToggle: (ExamDto) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(
                    listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF303F9F)),
                    Offset(0f, 0f), Offset(400f, 300f)
                ))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    StepDots(current = 1, total = 3)
                    TextButton(onClick = onNext) {
                        Text("Skip", color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("📚", fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Any other exams\nyou're targeting?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp
                )
                Text(
                    "Select up to 3 more exams (optional)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f), modifier = Modifier.padding(top = 4.dp)
                )
                if (state.selectedSecondary.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${state.selectedSecondary.size}/3 selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.CoinGold, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val grouped = state.exams.groupBy { it.category }

        LazyColumn(
            modifier            = Modifier.weight(1f),
            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            grouped.forEach { (category, exams) ->
                item(key = "header2_$category") {
                    Text(category.uppercase(), style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(exams, key = { "sec_${it.name}" }) { exam ->
                    val isPrimary   = exam.name == state.selectedPrimary?.name
                    val isSelected  = state.selectedSecondary.any { it.name == exam.name }
                    val isDisabled  = isPrimary || (!isSelected && state.selectedSecondary.size >= 3)

                    ExamSelectionCard(
                        exam         = exam,
                        isSelected   = isSelected,
                        onClick      = { if (!isDisabled) onToggle(exam) },
                        isMultiSelect = true,
                        isDisabled   = isDisabled,
                        note         = if (isPrimary) "Primary exam" else null
                    )
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }

        Box(
            modifier = Modifier.fillMaxWidth().background(Color.White)
                .padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding()
        ) {
            Button(
                onClick  = onNext,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Text(
                    text  = if (state.selectedSecondary.isEmpty()) "Skip for now" else "Continue →",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 3 — Preparation Level
// ─────────────────────────────────────────────────────────────

@Composable
private fun PrepLevelStep(
    state: ExamSetupUiState,
    onSelect: (PrepLevel) -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(
                    listOf(Color(0xFF004D40), Color(0xFF00695C), Color(0xFF00796B)),
                    Offset(0f, 0f), Offset(400f, 300f)
                ))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    StepDots(current = 2, total = 3)
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("📊", fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "What's your current\nprep level?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp
                )
                Text(
                    "We'll personalise your study plan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f), modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PREP_LEVELS.forEach { level ->
                val isSelected = state.selectedPrepLevel?.id == level.id
                val borderColor = if (isSelected) Color(0xFF00796B) else BpscColors.Divider
                val bgColor     = if (isSelected) Color(0xFFE0F2F1) else Color.White

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(level) }.border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp)),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = bgColor),
                    elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(if (isSelected) Color(0xFF00796B).copy(0.15f) else Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                            Text(level.emoji, fontSize = 26.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(level.label, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                            Text(level.subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        }
                        if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF00796B), modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Summary card
            if (state.selectedPrimary != null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BpscColors.PrimaryLight), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Your Plan", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                        Text("🎯 ${state.selectedPrimary!!.name}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary)
                        if (state.selectedSecondary.isNotEmpty()) {
                            Text("📚 +${state.selectedSecondary.size} more exam${if (state.selectedSecondary.size > 1) "s" else ""}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        }
                        state.selectedPrepLevel?.let {
                            Text("${it.emoji} ${it.label} level", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        }
                    }
                }
            }
        }

        // Bottom CTA
        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding()) {
            Button(
                onClick  = onFinish,
                enabled  = state.selectedPrepLevel != null && !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Start Preparing 🚀", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (state.saveError != null) {
                Spacer(Modifier.height(6.dp))
                Text(state.saveError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS
// ─────────────────────────────────────────────────────────────

@Composable
private fun ExamSelectionCard(
    exam: ExamDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    isMultiSelect: Boolean,
    isDisabled: Boolean = false,
    note: String? = null
) {
    val borderColor = if (isSelected) BpscColors.Primary else BpscColors.Divider
    val bgColor     = when { isSelected -> BpscColors.PrimaryLight; isDisabled -> Color(0xFFF5F5F5); else -> Color.White }
    val alpha       = if (isDisabled && !isSelected) 0.45f else 1f

    Card(
        modifier  = Modifier.fillMaxWidth().alpha(alpha).clickable(enabled = !isDisabled, onClick = onClick).border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp)),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(exam.emoji, fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(exam.name, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                if (note != null) {
                    Text(note, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                } else {
                    Text(exam.fullName, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (isMultiSelect) {
                Box(modifier = Modifier.size(20.dp).clip(if (isSelected) CircleShape else RoundedCornerShape(4.dp)).background(if (isSelected) BpscColors.Primary else Color.Transparent).border(1.5.dp, if (isSelected) BpscColors.Primary else BpscColors.TextHint, if (isSelected) CircleShape else RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            } else {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Transparent).border(1.5.dp, if (isSelected) BpscColors.Primary else BpscColors.TextHint, CircleShape), contentAlignment = Alignment.Center) {
                    if (isSelected) Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BpscColors.Primary))
                }
            }
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            Box(modifier = Modifier.size(if (i == current) 24.dp else 8.dp, 8.dp).clip(CircleShape).background(if (i == current) Color.White else Color.White.copy(0.35f)))
        }
    }
}
