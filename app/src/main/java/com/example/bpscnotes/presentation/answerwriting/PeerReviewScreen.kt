package com.example.bpscnotes.presentation.answerwriting

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppErrorState
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.ReviewAssignmentDto
import com.example.bpscnotes.data.remote.api.ReviewQuestionDto
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

// ─────────────────────────────────────────────────────────────
// PEER REVIEW — matches the client mockup: anonymous student answer
// (handwritten photos or text) + a 4-question structured review form.
// Submitting earns a review credit and auto-loads the next answer.
// ─────────────────────────────────────────────────────────────

private val HeroGradient = listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF3949AB))
private val Indigo       = Color(0xFF3949AB)
private val IndigoSoft   = Color(0xFFE8EAF6)

@Composable
fun PeerReviewScreen(
    navController: NavHostController,
    questionId: String? = null,
    viewModel: PeerReviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("peer_review") }
    // Deep link from the locked card on the answer detail screen — open the
    // answers for that question directly, since those are the ones that
    // unlock the student's own feedback.
    LaunchedEffect(questionId) {
        if (!questionId.isNullOrBlank()) viewModel.openQuestion(questionId)
    }
    LaunchedEffect(state.justSubmittedMessage, state.submitError) {
        state.justSubmittedMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearToasts() }
        state.submitError?.let { snackbarHostState.showSnackbar(it); viewModel.clearToasts() }
    }

    // Back steps down the stack: form → answers → questions → leave.
    fun onBack() { if (!viewModel.back()) navController.popBackStackSafe() }
    BackHandler(enabled = state.step != PeerReviewStep.QUESTIONS) { viewModel.back() }

    // A review just unlocked this student's own feedback — offer to open it.
    state.unlockedQuestionId?.let { unlockedQ ->
        AlertDialog(
            onDismissRequest = { viewModel.clearUnlockPrompt() },
            icon  = { Text("🔓", fontSize = 30.sp) },
            title = { Text(str.awUnlockedTitle, fontWeight = FontWeight.ExtraBold) },
            text  = { Text(str.awUnlockedBody, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearUnlockPrompt()
                        navController.navigate(Screen.AnswerWritingDetail.createRoute(unlockedQ))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo)
                ) { Text(str.awViewNow, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearUnlockPrompt() }) { Text(str.awKeepReviewing) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(cs.background)) {

            // ── Header ───────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(HeroGradient, Offset(0f, 0f), Offset(400f, 300f)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // weight(1f) so the title column yields space to the credit
                    // pill instead of squeezing it into a letter-per-line strip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color.White.copy(0.15f))
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                str.awPeerReview, style = MaterialTheme.typography.titleLarge,
                                color = Color.White, fontWeight = FontWeight.ExtraBold,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                // On the question list, the header carries the
                                // total pending count (client screen 1).
                                if (state.step == PeerReviewStep.QUESTIONS && state.totalPending > 0)
                                    "${str.awPendingReviewsTitle} (${state.totalPending})"
                                else str.awHelpFellow,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(0.7f),
                                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    // +1 Credit pill — never wraps
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF57F17).copy(0.3f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("⭐", fontSize = 12.sp)
                        Text(
                            str.prCredit, style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold,
                            maxLines = 1, softWrap = false
                        )
                    }
                }
            }

            // ── Body ─────────────────────────────────────────────
            when {
                state.isLoading -> AppLoader()
                state.loadError != null -> AppErrorState(
                    message = state.loadError!!,
                    onRetry = { viewModel.loadQuestions() },
                    secondaryAction = {
                        OutlinedButton(onClick = { navController.popBackStackSafe() }) { Text(str.goBack) }
                    }
                )
                state.step == PeerReviewStep.FORM && state.assignment != null ->
                    ReviewBody(state.assignment!!, viewModel)
                state.step == PeerReviewStep.ANSWERS ->
                    ReviewPoolList(state.pool, state.question, state.unlocksMyReviews) {
                        viewModel.selectAssignment(it)
                    }
                state.noneAvailable ->
                    DoneState(state.reviewsDoneThisSession) { navController.popBackStackSafe() }
                else -> ReviewQuestionList(state.questions) { viewModel.selectQuestion(it) }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
@Composable
private fun ReviewBody(assignment: ReviewAssignmentDto, viewModel: PeerReviewViewModel) {
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Guidance banner
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFF8E1)).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (assignment.reviewedByMe) "📖" else "🤝", fontSize = 18.sp)
            Column {
                Text(
                    if (assignment.reviewedByMe) str.awAlreadyReviewedTitle else str.awReviewBannerTitle,
                    style = MaterialTheme.typography.labelLarge, color = Color(0xFF7A5B00), fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (assignment.reviewedByMe) str.awAlreadyReviewedBody else str.awReviewBannerBody,
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF9C7A1A)
                )
            }
        }

        // Question card
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(IndigoSoft.copy(alpha = 0.55f)).padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                assignment.subject?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it, style = MaterialTheme.typography.labelSmall,
                        color = Indigo, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.8f)).padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                } ?: Spacer(Modifier.width(1.dp))
                Text(
                    "📄 ${assignment.wordLimit} ${str.awWords}",
                    style = MaterialTheme.typography.labelSmall, color = Indigo, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.8f)).padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "“${assignment.questionText}”",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface, fontWeight = FontWeight.ExtraBold, lineHeight = 23.sp
            )
        }

        // Student's answer (anonymous)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape).background(IndigoSoft),
                            contentAlignment = Alignment.Center
                        ) { Text("👤", fontSize = 14.sp) }
                        Text(str.awStudentAnswer, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                    }
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8FDF4))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.VerifiedUser, null, tint = BpscColors.Success, modifier = Modifier.size(11.dp))
                        Text(str.awAnonymous, style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))

                // PDF, handwritten photos, or typed text
                var pdfToView by remember(assignment.id) { mutableStateOf<String?>(null) }
                pdfToView?.let { AnswerPdfDialog(url = it, onDismiss = { pdfToView = null }) }
                val images = assignment.answerImages.orEmpty()
                val pdf = assignment.answerPdf
                if (!pdf.isNullOrBlank()) {
                    OpenAnswerPdfButton(onClick = { pdfToView = pdf }, modifier = Modifier.fillMaxWidth())
                } else if (images.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        images.forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, cs.outline.copy(0.4f), RoundedCornerShape(12.dp))
                            )
                        }
                    }
                } else {
                    // Selectable so reviewers can long-press to copy quotes
                    // from the answer into their suggestion.
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            assignment.answerText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant, lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        if (assignment.reviewedByMe) {
            // Already reviewed → read-only. The answer above stays open so the
            // student can re-read it and learn (client, 26 Jul); the review
            // they gave is shown instead of the form.
            ReviewGivenCard(assignment)
        } else {

        // ── Your Review form ─────────────────────────────────────
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(IndigoSoft),
                        contentAlignment = Alignment.Center
                    ) { Text("✍️", fontSize = 14.sp) }
                    Text(str.awYourReview, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                }

                // Q1 — question demand (Yes / Partly / No)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(str.awReviewQ1, style = MaterialTheme.typography.labelLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VerdictChip(str.yes, "yes", state.verdict, Modifier.weight(1f)) { viewModel.setVerdict(it) }
                        VerdictChip(str.awPartly, "partly", state.verdict, Modifier.weight(1f)) { viewModel.setVerdict(it) }
                        VerdictChip(str.no, "no", state.verdict, Modifier.weight(1f)) { viewModel.setVerdict(it) }
                    }
                }

                // Q2 — star rating
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(str.awReviewQ2, style = MaterialTheme.typography.labelLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            Text(
                                if (star <= state.rating) "⭐" else "☆",
                                fontSize = 30.sp,
                                color = if (star <= state.rating) Color.Unspecified else BpscColors.TextHint,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setRating(star) }
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                // Q3 — "top three weaknesses": up to 3 of the 6 areas
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(str.awReviewQ3, style = MaterialTheme.typography.labelLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                        Text(str.awUpTo3, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    }
                    val areas = listOf(
                        "introduction" to str.awAreaIntro, "structure" to str.awAreaStructure, "content" to str.awAreaContent,
                        "value_addition" to str.awAreaValueAdd, "analysis" to str.awAreaAnalysis, "conclusion" to str.awAreaConclusion,
                    )
                    areas.chunked(3).forEach { rowAreas ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowAreas.forEach { (key, label) ->
                                AreaChip(label, key in state.improvementAreas, Modifier.weight(1f)) { viewModel.toggleImprovementArea(key) }
                            }
                            repeat(3 - rowAreas.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                // Q4 — one suggestion (optional, 200 chars)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(str.awReviewQ4, style = MaterialTheme.typography.labelLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                        Text(str.optionalParen, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .border(1.dp, cs.outline.copy(0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = state.suggestion,
                            onValueChange = { viewModel.setSuggestion(it) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = cs.onSurface, lineHeight = 20.sp),
                            decorationBox = { inner ->
                                if (state.suggestion.isEmpty()) {
                                    Text(str.awSuggestionHint, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextHint, lineHeight = 18.sp)
                                }
                                inner()
                            }
                        )
                        Text(
                            "${state.suggestion.length}/200",
                            style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        // Submit
        Button(
            onClick = { viewModel.submit() },
            enabled = state.verdict != null && state.rating in 1..5 && !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Rounded.Send, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(str.awSubmitReview, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
        }  // end else — form + submit shown only when not yet reviewed
        Spacer(Modifier.height(24.dp))
    }
}

// Read-only summary of the review the user already gave on this answer.
@Composable
private fun ReviewGivenCard(a: ReviewAssignmentDto) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val areas = a.myImprovementAreas?.takeIf { it.isNotEmpty() } ?: listOfNotNull(a.myImprovementArea)
    val (vLabel, vColor) = when (a.myVerdict) {
        "yes"    -> str.yes to BpscColors.Success
        "partly" -> str.awPartly to Color(0xFFB45309)
        else     -> str.no to Color(0xFFE74C3C)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(30.dp).clip(CircleShape).background(IndigoSoft),
                    contentAlignment = Alignment.Center
                ) { Text("✍️", fontSize = 14.sp) }
                Text(str.awYourReview, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
            }
            // Q1 — verdict
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(str.awReviewQ1, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Text(
                    vLabel, style = MaterialTheme.typography.labelLarge, color = vColor, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(vColor.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            // Q2 — rating
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(str.awReviewQ2, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Text(
                    "⭐".repeat(a.myRating.coerceIn(0, 5)) + "☆".repeat((5 - a.myRating).coerceIn(0, 5)),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            // Q3 — improvement areas
            if (areas.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(str.awReviewQ3, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    areas.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                Text(
                                    areaLabel(key), style = MaterialTheme.typography.labelSmall,
                                    color = Indigo, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(IndigoSoft)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            // Q4 — suggestion
            if (!a.mySuggestion.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(str.awReviewQ4, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Text("“${a.mySuggestion}”", style = MaterialTheme.typography.bodyMedium, color = cs.onSurface, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun VerdictChip(label: String, key: String, selected: String?, modifier: Modifier, onSelect: (String) -> Unit) {
    val isSel = selected == key
    val (bg, fg, border) = when {
        isSel && key == "yes" -> Triple(Color(0xFFE8FDF4), BpscColors.Success, BpscColors.Success)
        isSel && key == "no"  -> Triple(Color(0xFFFEE8E8), Color(0xFFE74C3C), Color(0xFFE74C3C))
        isSel                 -> Triple(Color(0xFFFFF8E1), Color(0xFFB45309), Color(0xFFB45309))
        else                  -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outline.copy(0.5f))
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(bg).border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { onSelect(key) }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isSel) Icon(Icons.Rounded.CheckCircle, null, tint = fg, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AreaChip(label: String, isSel: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(if (isSel) IndigoSoft else cs.surface)
            .border(1.dp, if (isSel) Indigo else cs.outline.copy(0.5f), RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (isSel) Icon(Icons.Rounded.CheckCircle, null, tint = Indigo, modifier = Modifier.size(12.dp))
            Text(
                label, style = MaterialTheme.typography.labelMedium,
                color = if (isSel) Indigo else cs.onSurfaceVariant,
                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1, textAlign = TextAlign.Center, fontSize = 11.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Screen 1 — questions I attempted that have answers waiting for
// my review, neediest first. Questions where a single review would
// unlock my own feedback are flagged, since that is the strongest
// reason to start there.
// ─────────────────────────────────────────────────────────────
@Composable
private fun ReviewQuestionList(
    questions: List<ReviewQuestionDto>,
    onSelect: (ReviewQuestionDto) -> Unit,
) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                str.awPickQuestion,
                style = MaterialTheme.typography.titleSmall,
                color = BpscColors.TextSecondary, fontWeight = FontWeight.Bold,
            )
        }
        items(questions, key = { it.id }) { q ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(q) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (q.unlocksMyReviews) Color(0xFFFFFBF0) else cs.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                border = if (q.unlocksMyReviews)
                    BorderStroke(1.dp, Color(0xFFB45309).copy(alpha = 0.35f)) else null,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            q.subject?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it, style = MaterialTheme.typography.labelSmall,
                                    color = Indigo, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(IndigoSoft)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                            if (q.unlocksMyReviews) {
                                Text(
                                    "🔓 ${str.awUnlocksYours}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFF8E1))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        q.questionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurface, fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp, maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "📄 ${q.pendingCount} ${str.awAnswersToReview}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Indigo, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        )
                        if (q.myReviewsHere > 0) {
                            Text(
                                "✓ ${q.myReviewsHere}", style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────
// Screen 2 — the anonymous answers under one question. Cards only:
// the answer itself opens on the next screen, so scanning is fast.
//
// Note there is no average rating on these cards by design — showing
// one anchors the reviewer to the existing score before they've read
// the answer. The review count is shown instead: it points reviewers
// at the answers that still need help.
// ─────────────────────────────────────────────────────────────
@Composable
private fun ReviewPoolList(
    pool: List<ReviewAssignmentDto>,
    question: ReviewQuestionDto?,
    unlocksMyReviews: Boolean,
    onSelect: (ReviewAssignmentDto) -> Unit,
) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        question?.let { q ->
            item(key = "question") {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(IndigoSoft.copy(alpha = 0.55f)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${q.marks} ${str.awMarks} · ${q.wordLimit} ${str.awWords}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Indigo, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    )
                    Text(
                        q.questionText,
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurface, fontWeight = FontWeight.ExtraBold, lineHeight = 21.sp,
                    )
                }
            }
        }
        if (unlocksMyReviews) {
            item(key = "unlock_banner") {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF8E1)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔓", fontSize = 18.sp)
                    Text(
                        str.awUnlockBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A5B00), lineHeight = 18.sp,
                    )
                }
            }
        }
        if (pool.isEmpty()) {
            item {
                Text(
                    str.awNoAnswersHere,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            item {
                Text(
                    str.awPickAnswer,
                    style = MaterialTheme.typography.titleSmall,
                    color = BpscColors.TextSecondary, fontWeight = FontWeight.Bold,
                )
            }
        }
        itemsIndexed(pool, key = { _, a -> a.id }) { i, a ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(a) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(IndigoSoft),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (a.isSeed) "📘" else "👤", fontSize = 18.sp) }

                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                // Samples are labelled, never passed off as a peer's work
                                if (a.isSeed) str.awSampleAnswer else "${str.awStudentAnswer} #${i + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface, fontWeight = FontWeight.Bold,
                            )
                            if (!a.isSeed) {
                                Text(
                                    str.awAnonymous, style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE8FDF4)).padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            if (a.reviewedByMe) {
                                Text(
                                    "✓ ${str.awReviewedByYou}", style = MaterialTheme.typography.labelSmall,
                                    color = Indigo, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(IndigoSoft).padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val kind = when {
                                !a.answerPdf.isNullOrBlank()    -> str.awPdfMode
                                !a.answerImages.isNullOrEmpty() -> str.awPhotosLabel
                                else                            -> "${a.wordCount} ${str.awWordsLower}"
                            }
                            Text("✍️ $kind", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                            // Review count, not average rating — see the note above
                            Text(
                                "🤝 ${a.peerReviewCount} ${str.awReviewsLower}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (a.peerReviewCount == 0) Color(0xFFB45309) else BpscColors.TextHint,
                                fontWeight = if (a.peerReviewCount == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DoneState(reviewsDone: Int, onBack: () -> Unit) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 52.sp)
        Spacer(Modifier.height(14.dp))
        Text(str.awNoMoreReviews, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            str.awNoMoreReviewsBody,
            style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        if (reviewsDone > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                "⭐ +$reviewsDone ${str.awReviewCredits}",
                style = MaterialTheme.typography.titleSmall, color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF8E1))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo)
        ) { Text(str.goBack, fontWeight = FontWeight.Bold) }
    }
}
