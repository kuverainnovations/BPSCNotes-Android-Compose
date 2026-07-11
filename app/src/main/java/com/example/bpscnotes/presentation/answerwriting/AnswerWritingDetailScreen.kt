package com.example.bpscnotes.presentation.answerwriting

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppErrorState
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────
// ANSWER WRITING — detail. Two modes:
//  · Not submitted: writing pad with live word count + elapsed timer
//  · Submitted:     my answer + model answer + score/feedback once graded
// ─────────────────────────────────────────────────────────────

private val HeroGradient = listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF3949AB))
private val Indigo       = Color(0xFF3949AB)
private val IndigoSoft   = Color(0xFFE8EAF6)

@Composable
fun AnswerWritingDetailScreen(
    navController: NavHostController,
    questionId: String,
    viewModel: AnswerWritingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(questionId) { viewModel.loadDetail(questionId) }

    // Coin toast after submit
    LaunchedEffect(state.justEarnedCoins) {
        state.justEarnedCoins?.let {
            snackbarHostState.showSnackbar("🪙 +$it coins earned!")
            viewModel.clearCoinsToast()
        }
    }
    // Submit error toast
    LaunchedEffect(state.submitError) {
        state.submitError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSubmitError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoadingDetail -> AppLoader()
            state.detailError != null -> AppErrorState(
                message = state.detailError!!,
                onRetry = { viewModel.loadDetail(questionId) },
                secondaryAction = {
                    OutlinedButton(onClick = { navController.popBackStackSafe() }) { Text(str.goBack) }
                }
            )
            state.question != null -> DetailContent(questionId, navController, viewModel)
            else -> AppLoader()
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

@Composable
private fun DetailContent(
    questionId: String,
    navController: NavHostController,
    viewModel: AnswerWritingViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val q = state.question ?: return
    val submission = state.submission
    val isWriting = submission == null

    // Elapsed writing timer — only ticks pre-submit
    var elapsedSecs by rememberSaveable(questionId) { mutableStateOf(0) }
    LaunchedEffect(isWriting) {
        while (isWriting) { delay(1000); elapsedSecs++ }
    }

    var showConfirm by remember { mutableStateOf(false) }
    val words = state.draftText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    val overLimit = words > q.wordLimit

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { if (!state.isSubmitting) showConfirm = false },
            icon = { Text("✍️", fontSize = 30.sp) },
            title = { Text(str.awConfirmTitle, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(str.awConfirmBody, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                    Text(
                        "$words / ${q.wordLimit} ${str.awWords}" + if (overLimit) " · ${str.awOverLimit}" else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (overLimit) cs.error else BpscColors.Success,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submit(questionId, elapsedSecs) },
                    enabled = !state.isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(str.awSubmitting, fontWeight = FontWeight.Bold)
                    } else Text(str.awSubmit, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }, enabled = !state.isSubmitting) { Text(str.cancel) }
            }
        )
    }
    // Close the dialog once the submission lands
    LaunchedEffect(submission) { if (submission != null) showConfirm = false }

    Column(modifier = Modifier.fillMaxSize().background(cs.background)) {

        // ── Hero header (pinned) ─────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(HeroGradient, Offset(0f, 0f), Offset(400f, 300f)))
                .statusBarsPadding()
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(0.05f), 140.dp.toPx(), Offset(size.width + 20.dp.toPx(), -30.dp.toPx()))
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(Color.White.copy(0.15f))
                            .clickable { navController.popBackStackSafe() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    if (isWriting) {
                        // Elapsed time pill
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Rounded.Timer, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text(
                                "%d:%02d".format(elapsedSecs / 60, elapsedSecs % 60),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    q.subject?.takeIf { it.isNotBlank() }?.let {
                        HeaderChip(it)
                    }
                    HeaderChip("🏅 ${q.marks} ${str.awMarks}")
                    HeaderChip("📝 ${q.wordLimit} ${str.awWords}")
                }
            }
        }

        // ── Scrollable body ──────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Question card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Q.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Indigo, fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        q.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface, fontWeight = FontWeight.Bold, lineHeight = 23.sp
                    )
                }
            }

            // Writing tips (pre-submit only)
            if (isWriting && !q.tips.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("💡", fontSize = 16.sp)
                        Column {
                            Text(str.awTips, style = MaterialTheme.typography.labelMedium, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(3.dp))
                            Text(q.tips!!, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A5B00), lineHeight = 18.sp)
                        }
                    }
                }
            }

            if (isWriting) {
                // ── Writing pad ─────────────────────────────────
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        BasicTextField(
                            value = state.draftText,
                            onValueChange = { viewModel.updateDraft(it) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = cs.onSurface, lineHeight = 22.sp),
                            decorationBox = { inner ->
                                if (state.draftText.isEmpty()) {
                                    Text(str.awWriteHint, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                                }
                                inner()
                            }
                        )
                        HorizontalDivider(color = cs.outline.copy(0.4f))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$words / ${q.wordLimit} ${str.awWords}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (overLimit) cs.error else Indigo,
                                fontWeight = FontWeight.Bold
                            )
                            if (overLimit) {
                                Text(
                                    "⚠️ ${str.awOverLimit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.error, fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        // word-limit progress
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (words.toFloat() / q.wordLimit).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (overLimit) cs.error else Indigo,
                            trackColor = IndigoSoft,
                        )
                    }
                }
            } else {
                // ── Submitted: status → score/feedback → model answer → my answer ──
                SubmittedStatusCard(submission!!)

                if (!q.modelAnswer.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = IndigoSoft.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🏆", fontSize = 15.sp)
                                Text(str.awModelAnswer, style = MaterialTheme.typography.titleSmall, color = Indigo, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                q.modelAnswer!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface, lineHeight = 22.sp
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(str.awYourAnswer, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "${submission.wordCount} ${str.awWords}",
                                style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            submission.answerText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant, lineHeight = 22.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }

        // ── Pinned submit button (writing mode only) ─────────────
        if (isWriting) {
            Box(modifier = Modifier.fillMaxWidth().background(cs.surface).padding(16.dp).navigationBarsPadding().imePadding()) {
                Button(
                    onClick = { showConfirm = true },
                    enabled = words > 0 && !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo)
                ) {
                    Icon(Icons.Rounded.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.awSubmit, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HeaderChip(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun SubmittedStatusCard(submission: com.example.bpscnotes.data.remote.api.AnswerSubmissionDto) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val reviewed = submission.status == "reviewed"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reviewed) Color(0xFFE8FDF4) else Color(0xFFFFF8E1)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (reviewed && submission.score != null) {
                    // Score circle
                    Box(
                        modifier = Modifier.size(58.dp).clip(CircleShape).background(BpscColors.Success),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${submission.score.toInt()}", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text("/${submission.marks}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f), fontSize = 9.sp)
                        }
                    }
                    Column {
                        Text(str.awStatusReviewed, style = MaterialTheme.typography.titleSmall, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold)
                        submission.reviewedAt?.let {
                            Text(it.take(10), style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                        }
                    }
                } else {
                    Text("⏳", fontSize = 28.sp)
                    Column {
                        Text(str.awStatusPending, style = MaterialTheme.typography.titleSmall, color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(2.dp))
                        Text(str.awPendingNote, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A5B00), lineHeight = 17.sp)
                    }
                }
            }

            // Examiner feedback
            if (reviewed && !submission.feedback.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.65f)).padding(12.dp)
                ) {
                    Text(str.awFeedback, style = MaterialTheme.typography.labelMedium, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        submission.feedback!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurface, lineHeight = 21.sp
                    )
                }
            }
        }
    }
}
