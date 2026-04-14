package com.example.bpscnotes.presentation.auth.examselection

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────
data class ExamOption(
    val id: String,
    val name: String,
    val fullName: String,
    val emoji: String,
    val color: Color,
    val bg: Color,
    val category: ExamCategory,
    val subjects: List<String>,
    val studentsCount: String,
    val difficulty: String,
    val avgPrepMonths: Int,
)

enum class ExamCategory(val label: String, val emoji: String) {
    BPSC      ("BPSC",            "🎯"),
    Bihar     ("Bihar State",     "🏛️"),
    Central   ("Central Govt",    "🇮🇳"),
    Defence   ("Defence",         "🛡️"),
    Teaching  ("Teaching",        "📚"),
}

enum class PrepLevel(val label: String, val emoji: String, val color: Color) {
    Beginner    ("Beginner",     "🌱", Color(0xFF2ECC71)),
    Intermediate("Intermediate", "📈", Color(0xFFE67E22)),
    Advanced    ("Advanced",     "🚀", Color(0xFFE74C3C)),
}

val allExams = listOf(
    ExamOption("e1",  "BPSC 70th CCE",       "Bihar Public Service Commission 70th CCE",
        "🎯", Color(0xFF1565C0), Color(0xFFE8F0FD), ExamCategory.BPSC,
        listOf("Polity", "History", "Geography", "Economy", "Bihar GK", "Science"),
        "2.8L+", "Hard", 12),
    ExamOption("e2",  "BPSC 71st CCE",       "Bihar Public Service Commission 71st CCE",
        "🎯", Color(0xFF1565C0), Color(0xFFE8F0FD), ExamCategory.BPSC,
        listOf("Polity", "History", "Geography", "Economy", "Bihar GK", "Science"),
        "2.5L+", "Hard", 12),
    ExamOption("e3",  "BPSC APO",            "BPSC Assistant Prosecution Officer",
        "⚖️", Color(0xFF9B59B6), Color(0xFFF3E8FD), ExamCategory.BPSC,
        listOf("Law", "Polity", "Current Affairs", "Bihar GK"),
        "45K+", "Medium", 8),
    ExamOption("e4",  "BPSC AE",             "BPSC Assistant Engineer",
        "📐", Color(0xFF1ABC9C), Color(0xFFE8FDF8), ExamCategory.BPSC,
        listOf("Engineering", "Maths", "General Studies"),
        "35K+", "Hard", 10),
    ExamOption("e5",  "Bihar Police SI",      "Bihar Police Sub-Inspector",
        "👮", Color(0xFF2ECC71), Color(0xFFE8FDF4), ExamCategory.Bihar,
        listOf("General Studies", "Bihar GK", "Reasoning", "Hindi"),
        "3.5L+", "Medium", 8),
    ExamOption("e6",  "Bihar Constable",      "Bihar Police Constable",
        "🚔", Color(0xFF27AE60), Color(0xFFE8FDF4), ExamCategory.Bihar,
        listOf("General Studies", "Bihar GK", "Maths"),
        "8L+", "Easy", 4),
    ExamOption("e7",  "Bihar SSC",            "Bihar Staff Selection Commission",
        "📋", Color(0xFFE67E22), Color(0xFFFFF0EA), ExamCategory.Bihar,
        listOf("General Studies", "Maths", "Reasoning", "Hindi"),
        "5L+", "Medium", 6),
    ExamOption("e8",  "BPSC Teacher",         "Bihar Teacher Eligibility (BTET/STET)",
        "🏫", Color(0xFFF39C12), Color(0xFFFFF8E1), ExamCategory.Teaching,
        listOf("Education", "Subject Knowledge", "Bihar GK"),
        "6L+", "Medium", 6),
    ExamOption("e9",  "Bihar Judiciary",      "Bihar Judicial Services",
        "⚖️", Color(0xFF8E44AD), Color(0xFFF3E8FD), ExamCategory.Bihar,
        listOf("Law", "Constitution", "Criminal Law", "Civil Law"),
        "20K+", "Hard", 18),
    ExamOption("e10", "Bihar Health Dept",    "Bihar Health Services Exam",
        "🏥", Color(0xFFE74C3C), Color(0xFFFEE8E8), ExamCategory.Bihar,
        listOf("Biology", "Medical", "General Studies"),
        "25K+", "Hard", 12),
    ExamOption("e11", "SSC CGL",              "Staff Selection Commission CGL",
        "🇮🇳", Color(0xFF2980B9), Color(0xFFE8F4FD), ExamCategory.Central,
        listOf("Reasoning", "Quantitative Aptitude", "English", "GK"),
        "25L+", "Hard", 12),
    ExamOption("e12", "SSC CHSL",             "Staff Selection Commission CHSL",
        "📝", Color(0xFF3498DB), Color(0xFFE8F4FD), ExamCategory.Central,
        listOf("Reasoning", "Maths", "English", "GK"),
        "18L+", "Medium", 8),
    ExamOption("e13", "Railway NTPC",         "Railway Recruitment Board NTPC",
        "🚂", Color(0xFFE74C3C), Color(0xFFFEE8E8), ExamCategory.Central,
        listOf("Maths", "GK & CA", "Reasoning"),
        "30L+", "Medium", 8),
    ExamOption("e14", "Railway Group D",      "Railway Recruitment Board Group D",
        "🛤️", Color(0xFFE74C3C), Color(0xFFFEE8E8), ExamCategory.Central,
        listOf("Maths", "Science", "Reasoning", "GK"),
        "20L+", "Easy", 6),
    ExamOption("e15", "UPSC CSE",             "Union Public Service Commission CSE",
        "🏆", Color(0xFFF39C12), Color(0xFFFFF8E1), ExamCategory.Central,
        listOf("History", "Polity", "Geography", "Economy", "Science", "CSAT"),
        "10L+", "Very Hard", 24),
    ExamOption("e16", "NDA",                  "National Defence Academy",
        "🛡️", Color(0xFF1A5276), Color(0xFFE8EAF6), ExamCategory.Defence,
        listOf("Maths", "English", "Physics", "Chemistry"),
        "5L+", "Hard", 12),
    ExamOption("e17", "CDS",                  "Combined Defence Services",
        "⚔️", Color(0xFF1A5276), Color(0xFFE8EAF6), ExamCategory.Defence,
        listOf("English", "GK", "Maths"),
        "3L+", "Hard", 10),
    ExamOption("e18", "Bihar Engineering",    "Bihar Engineering Services",
        "⚙️", Color(0xFF16A085), Color(0xFFE8FDF8), ExamCategory.Bihar,
        listOf("Engineering", "Technical", "General Studies"),
        "18K+", "Hard", 12),
)

// ─────────────────────────────────────────────────────────────
// EXAM SELECTION SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun ExamSelectionScreen(navController: NavHostController) {
    var primaryExam     by remember { mutableStateOf<ExamOption?>(null) }
    var secondaryExam   by remember { mutableStateOf<ExamOption?>(null) }
    var prepLevel       by remember { mutableStateOf<PrepLevel?>(null) }
    var targetYear      by remember { mutableStateOf("2026") }
    var selectedCategory by remember { mutableStateOf<ExamCategory?>(null) }
    var currentStep     by remember { mutableIntStateOf(1) } // 1=Primary, 2=Secondary, 3=Level
    var searchQuery     by remember { mutableStateOf("") }

    val filteredExams = allExams.filter { exam ->
        (selectedCategory == null || exam.category == selectedCategory) &&
                (searchQuery.isEmpty() || exam.name.contains(searchQuery, ignoreCase = true) ||
                        exam.fullName.contains(searchQuery, ignoreCase = true))
    }

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                        Offset(0f, 0f), Offset(400f, 500f)
                    ))
                    .statusBarsPadding()
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
                    drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
                    val dotSpacing = 28.dp.toPx()
                    var x = dotSpacing
                    while (x < size.width) {
                        var y = dotSpacing
                        while (y < size.height) {
                            drawCircle(Color.White.copy(0.05f), 1.dp.toPx(), Offset(x, y))
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Step indicator
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        listOf("Primary Exam", "Additional Exams", "Prep Level").forEachIndexed { index, label ->
                            val step      = index + 1
                            val isDone    = currentStep > step
                            val isCurrent = currentStep == step
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape)
                                        .background(when { isDone -> BpscColors.Success; isCurrent -> Color.White; else -> Color.White.copy(0.25f) }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    else Text("$step", style = MaterialTheme.typography.labelSmall, color = if (isCurrent) BpscColors.Primary else Color.White.copy(0.6f), fontWeight = FontWeight.ExtraBold)
                                }
                                if (index < 2) Box(modifier = Modifier.width(40.dp).height(2.dp).background(if (isDone) BpscColors.Success else Color.White.copy(0.25f)))
                            }
                        }
                    }

                    // Title
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            when (currentStep) {
                                1 -> "🎯 Choose Your Primary Exam"
                                2 -> "📚 Add More Exams (Optional)"
                                else -> "📊 Your Preparation Level"
                            },
                            style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                        )
                        Text(
                            when (currentStep) {
                                1 -> "We'll personalize content specifically for your exam"
                                2 -> "Prepare for multiple exams simultaneously"
                                else -> "This helps us recommend the right content"
                            },
                            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f), textAlign = TextAlign.Center
                        )
                    }

                    // Search (step 1 and 2)
                    if (currentStep <= 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(0.15f)).border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery, onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) Text("Search exam...", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.5f))
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp).clickable { searchQuery = "" })
                        }
                    }
                }
            }

            // ── Content by step ──────────────────────────────
            when (currentStep) {
                1 -> Step1PrimaryExam(
                    exams            = filteredExams,
                    primaryExam      = primaryExam,
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    onSelectExam     = { primaryExam = it; if (secondaryExam?.id == it.id) secondaryExam = null }
                )
                2 -> Step2SecondaryExams(
                    exams          = filteredExams,
                    primaryExam    = primaryExam,
                    secondaryExam  = secondaryExam,
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    onToggleSecondary = { exam ->
                        secondaryExam = if (secondaryExam?.id == exam.id) null else exam
                    }
                )
                3 -> Step3PrepLevel(
                    primaryExam  = primaryExam,
                    secondaryExam = secondaryExam,
                    prepLevel    = prepLevel,
                    targetYear   = targetYear,
                    onSelectLevel = { prepLevel = it },
                    onYearChange  = { targetYear = it }
                )
            }
        }

        // ── Bottom CTA ───────────────────────────────────────
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.White).padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Selected summary
                if (primaryExam != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(BpscColors.PrimaryLight).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(primaryExam!!.emoji, fontSize = 16.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(primaryExam!!.name, style = MaterialTheme.typography.bodyLarge, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                            if (secondaryExam != null) Text("+ ${secondaryExam!!.name}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        }
                        if (prepLevel != null) Text("${prepLevel!!.emoji} ${prepLevel!!.label}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Primary, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick  = { currentStep--; searchQuery = ""; selectedCategory = null },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            border   = BorderStroke(1.dp, BpscColors.Divider),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.TextSecondary)
                        ) {
                            Icon(Icons.Rounded.ArrowBack, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Back", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Button(
                        onClick = {
                            when (currentStep) {
                                1 -> { if (primaryExam != null) { currentStep = 2; searchQuery = ""; selectedCategory = null } }
                                2 -> { currentStep = 3 }
                                3 -> {
                                    if (prepLevel != null) {
                                        navController.navigate(Screen.Main.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(2f).height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        enabled  = when (currentStep) { 1 -> primaryExam != null; 3 -> prepLevel != null; else -> true },
                        colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                    ) {
                        Text(
                            when (currentStep) {
                                1 -> "Next →"
                                2 -> if (secondaryExam != null) "Continue with ${secondaryExam!!.name} →" else "Skip for now →"
                                else -> "Start Preparing 🚀"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 1 — PRIMARY EXAM SELECTION
// ─────────────────────────────────────────────────────────────
@Composable
private fun Step1PrimaryExam(
    exams: List<ExamOption>,
    primaryExam: ExamOption?,
    selectedCategory: ExamCategory?,
    onCategoryChange: (ExamCategory?) -> Unit,
    onSelectExam: (ExamOption) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Category filter
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                val sel = selectedCategory == null
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) BpscColors.Primary else Color.White)
                    .border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
                    .clickable { onCategoryChange(null) }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text("All", style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
            items(ExamCategory.values()) { cat ->
                val sel = selectedCategory == cat
                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) BpscColors.Primary else Color.White)
                    .border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
                    .clickable { onCategoryChange(if (sel) null else cat) }.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(cat.emoji, fontSize = 12.sp)
                    Text(cat.label, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(exams) { exam ->
                ExamCard(exam = exam, isSelected = primaryExam?.id == exam.id, isDisabled = false, badge = null, onClick = { onSelectExam(exam) })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 2 — SECONDARY EXAMS
// ─────────────────────────────────────────────────────────────
@Composable
private fun Step2SecondaryExams(
    exams: List<ExamOption>,
    primaryExam: ExamOption?,
    secondaryExam: ExamOption?,
    selectedCategory: ExamCategory?,
    onCategoryChange: (ExamCategory?) -> Unit,
    onToggleSecondary: (ExamOption) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Primary exam reminder
        if (primaryExam != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp)).background(BpscColors.PrimaryLight).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(primaryExam.emoji, fontSize = 18.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Primary: ${primaryExam.name}", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                    Text("Content will be prioritized for this exam", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
                Icon(Icons.Rounded.Star, null, tint = BpscColors.Primary, modifier = Modifier.size(16.dp))
            }
        }

        // Category filter
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                val sel = selectedCategory == null
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) BpscColors.Primary else Color.White)
                    .border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
                    .clickable { onCategoryChange(null) }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text("All", style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
            items(ExamCategory.values()) { cat ->
                val sel = selectedCategory == cat
                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) BpscColors.Primary else Color.White)
                    .border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
                    .clickable { onCategoryChange(if (sel) null else cat) }.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(cat.emoji, fontSize = 12.sp)
                    Text(cat.label, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(exams) { exam ->
                val isPrimary  = exam.id == primaryExam?.id
                val isSelected = exam.id == secondaryExam?.id
                ExamCard(
                    exam       = exam,
                    isSelected = isSelected,
                    isDisabled = isPrimary,
                    badge      = if (isPrimary) "Primary" else null,
                    onClick    = { if (!isPrimary) onToggleSecondary(exam) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 3 — PREP LEVEL + TARGET YEAR
// ─────────────────────────────────────────────────────────────
@Composable
private fun Step3PrepLevel(
    primaryExam: ExamOption?,
    secondaryExam: ExamOption?,
    prepLevel: PrepLevel?,
    targetYear: String,
    onSelectLevel: (PrepLevel) -> Unit,
    onYearChange: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Exam summary card
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📋 Your Exam Plan", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                    if (primaryExam != null) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(primaryExam.bg).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(primaryExam.emoji, fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(primaryExam.name, style = MaterialTheme.typography.titleMedium, color = primaryExam.color, fontWeight = FontWeight.ExtraBold)
                                    Text("PRIMARY", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 8.sp,
                                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(primaryExam.color).padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                                Text(primaryExam.fullName, style = MaterialTheme.typography.bodyMedium, color = primaryExam.color.copy(0.7f))
                            }
                        }
                    }
                    if (secondaryExam != null) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(secondaryExam.bg).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(secondaryExam.emoji, fontSize = 18.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(secondaryExam.name, style = MaterialTheme.typography.bodyLarge, color = secondaryExam.color, fontWeight = FontWeight.Bold)
                                    Text("SECONDARY", style = MaterialTheme.typography.labelSmall, color = secondaryExam.color, fontWeight = FontWeight.Bold, fontSize = 8.sp,
                                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(secondaryExam.bg).padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                    // Subjects preview
                    if (primaryExam != null) {
                        Text("📚 Subjects you'll study:", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        val allSubjects = (primaryExam.subjects + (secondaryExam?.subjects ?: emptyList())).distinct()
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            allSubjects.forEach { sub ->
                                Text(sub, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 10.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Prep level
        item {
            Text("📊 Your Preparation Level", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrepLevel.values().forEach { level ->
                    val isSelected = prepLevel == level
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectLevel(level) },
                        shape    = RoundedCornerShape(16.dp),
                        colors   = CardDefaults.cardColors(containerColor = if (isSelected) level.color.copy(0.08f) else Color.White),
                        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp),
                        border   = if (isSelected) BorderStroke(2.dp, level.color) else BorderStroke(1.dp, BpscColors.Divider)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(level.color.copy(0.1f)), contentAlignment = Alignment.Center) {
                                Text(level.emoji, fontSize = 24.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(level.label, style = MaterialTheme.typography.titleMedium, color = if (isSelected) level.color else BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text(when (level) {
                                    PrepLevel.Beginner     -> "Just starting out · Need structured guidance"
                                    PrepLevel.Intermediate -> "Have basics · Want to improve scores"
                                    PrepLevel.Advanced     -> "Already prepared · Need revision & tests"
                                }, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                            }
                            if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = level.color, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        // Target year
        item {
            Text("📅 Target Exam Year", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("2026", "2027", "2028").forEach { year ->
                    val isSel = targetYear == year
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (isSel) BpscColors.Primary else Color.White)
                            .border(1.dp, if (isSel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(14.dp))
                            .clickable { onYearChange(year) }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(year, style = MaterialTheme.typography.titleLarge, color = if (isSel) Color.White else BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                            Text(when (year) { "2026" -> "This year 🔥"; "2027" -> "Next year"; else -> "Long term" },
                                style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White.copy(0.8f) else BpscColors.TextHint, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        // Personalization preview
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = BpscColors.PrimaryLight), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✨ What we'll personalize for you", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                    listOf(
                        "📚 Courses recommended for your exam",
                        "🎯 Daily targets based on your syllabus",
                        "📰 Current affairs filtered for your exam",
                        "❓ Quizzes from your exam's previous papers",
                        "💼 Job alerts for your target exam category",
                        "🏆 Subscription plans with your exam content first",
                    ).forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item, style = MaterialTheme.typography.bodyMedium, color = BpscColors.Primary.copy(0.8f))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// EXAM CARD
// ─────────────────────────────────────────────────────────────
@Composable
private fun ExamCard(
    exam: ExamOption,
    isSelected: Boolean,
    isDisabled: Boolean,
    badge: String?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (isDisabled) 0.6f else 1f).clickable(enabled = !isDisabled, onClick = onClick),
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = if (isSelected) exam.bg else Color.White),
        elevation = CardDefaults.cardElevation(if (isSelected) 5.dp else 2.dp),
        border   = if (isSelected) BorderStroke(2.dp, exam.color) else BorderStroke(1.dp, BpscColors.Divider)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Icon
            Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(if (isSelected) exam.color.copy(0.15f) else exam.bg), contentAlignment = Alignment.Center) {
                Text(exam.emoji, fontSize = 24.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(exam.name, style = MaterialTheme.typography.titleMedium, color = if (isSelected) exam.color else BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                    if (badge != null) Text(badge, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(exam.color).padding(horizontal = 5.dp, vertical = 2.dp))
                }
                Text(exam.fullName, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(exam.category.emoji, fontSize = 10.sp)
                    Text(exam.category.label, style = MaterialTheme.typography.labelSmall, color = exam.color, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(exam.bg).padding(horizontal = 5.dp, vertical = 2.dp))
                    Text("·", color = BpscColors.TextHint)
                    Icon(Icons.Rounded.People, null, tint = BpscColors.TextHint, modifier = Modifier.size(10.dp))
                    Text("${exam.studentsCount} students", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
                    Text("·", color = BpscColors.TextHint)
                    Text("~${exam.avgPrepMonths}m prep", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
                }
            }
            // Difficulty badge + selection
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val diffColor = when (exam.difficulty) { "Easy" -> Color(0xFF2ECC71); "Medium" -> Color(0xFFE67E22); "Hard" -> Color(0xFFE74C3C); else -> Color(0xFF8E44AD) }
                Text(exam.difficulty, style = MaterialTheme.typography.labelSmall, color = diffColor, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(diffColor.copy(0.1f)).padding(horizontal = 6.dp, vertical = 2.dp))
                if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = exam.color, modifier = Modifier.size(22.dp))
                else Box(modifier = Modifier.size(22.dp).clip(CircleShape).border(2.dp, BpscColors.Divider, CircleShape))
            }
        }
    }
}