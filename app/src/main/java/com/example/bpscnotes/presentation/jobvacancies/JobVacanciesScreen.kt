package com.example.bpscnotes.presentation.jobvacancies

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.JobVacancyDto
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────
// Category mapping
// ─────────────────────────────────────────────────────────────
enum class JobCategory(val label: String, val emoji: String, val color: Color, val bg: Color) {
    BPSC        ("BPSC",         "🎯", Color(0xFF1565C0), Color(0xFFE8F0FD)),
    BiharGovt   ("Bihar Govt",   "🏛️", Color(0xFF2ECC71), Color(0xFFE8FDF4)),
    CentralGovt ("Central Govt", "🇮🇳", Color(0xFF9B59B6), Color(0xFFF3E8FD)),
    Railway     ("Railway",      "🚂", Color(0xFFE67E22), Color(0xFFFFF0EA)),
    Banking     ("Banking",      "🏦", Color(0xFF16A085), Color(0xFFE8FDF8)),
    SSC         ("SSC",          "📋", Color(0xFF8E44AD), Color(0xFFF5EEF8)),
    Defence     ("Defence",      "🛡️", Color(0xFF2C3E50), Color(0xFFEAECEE)),
    Private     ("Private",      "🏢", Color(0xFFE67E22), Color(0xFFFFF0EA)),
    Teaching    ("Teaching",     "📚", Color(0xFF27AE60), Color(0xFFE9F7EF)),
    Other       ("Other",        "📌", Color(0xFF7F8C8D), Color(0xFFF2F3F4)),
}

private fun String.toJobCategory(): JobCategory = when (this.trim()) {
    "BPSC"         -> JobCategory.BPSC
    "Bihar Govt"   -> JobCategory.BiharGovt
    "Central Govt" -> JobCategory.CentralGovt
    "Railway"      -> JobCategory.Railway
    "Banking"      -> JobCategory.Banking
    "SSC"          -> JobCategory.SSC
    "Defence"      -> JobCategory.Defence
    "Private"      -> JobCategory.Private
    "Teaching"     -> JobCategory.Teaching
    else           -> JobCategory.Other
}

// ─────────────────────────────────────────────────────────────
// Date helpers
// ─────────────────────────────────────────────────────────────
private val DATE_FORMATS = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd",
)

private fun String?.parseToMillis(): Long {
    if (this.isNullOrBlank()) return 0L
    for (fmt in DATE_FORMATS) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.parse(this)?.time ?: continue
        } catch (_: Exception) {}
    }
    return 0L
}

private fun String?.formatDisplay(): String {
    if (this.isNullOrBlank()) return "—"
    val millis = this.parseToMillis()
    if (millis == 0L) return this
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
}

private fun Long.daysUntil(): Long {
    if (this == 0L) return -1
    return TimeUnit.MILLISECONDS.toDays(this - System.currentTimeMillis())
}

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun JobVacanciesScreen(
    navController: NavHostController,
    viewModel: JobVacanciesViewModel = hiltViewModel()
) {
    val vmState by viewModel.uiState.collectAsState()

    var searchQuery      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<JobCategory?>(null) }
    var selectedJob      by remember { mutableStateOf<JobVacancyDto?>(null) }
    val savedJobs = remember(vmState.jobs) {
        vmState.jobs
            .filter { it.isSaved == true }
            .map { it.id }
            .toMutableStateList()
    }
    var showAlertSheet   by remember { mutableStateOf(false) }
    val focusManager     = LocalFocusManager.current

    val allJobs = vmState.jobs

    // Get all unique categories from live data
    val liveCategories = remember(allJobs) {
        allJobs.map { it.category?.toJobCategory() }.distinct().sortedBy { it?.label }
    }

    val filtered = remember(allJobs, searchQuery, selectedCategory) {
        allJobs.filter { job ->
            val matchCat    = selectedCategory == null || (job.category ?: "").toJobCategory() == selectedCategory
            val matchSearch = searchQuery.isEmpty() ||
                    job.title.contains(searchQuery, true) ||
                    job.department?.contains(searchQuery, true) == true ||
                    job.location?.contains(searchQuery, true) == true ||
                    job.qualification?.contains(searchQuery, true) == true
            matchCat && matchSearch
        }.sortedWith(compareBy { it.applyEndDate.parseToMillis() })
    }

    if (vmState.isLoading && allJobs.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = BpscColors.Primary)
        }
        return
    }

    if (vmState.error != null && allJobs.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚠️", fontSize = 40.sp)
                Text(vmState.error!!, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                Button(onClick = { viewModel.retry() }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text("Retry") }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── HERO HEADER ───────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                        Offset.Zero, Offset(400f, 400f)
                    ))
                    .statusBarsPadding()
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
                    drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStack() }, Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Job Vacancies", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("${filtered.size} active openings", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                            }
                        }
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(0.15f))
                                .clickable { showAlertSheet = true },
                            Alignment.Center
                        ) {
                            Icon(Icons.Rounded.NotificationAdd, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Search
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.15f))
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement =  Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) Text("Search jobs, departments, location…", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.5f))
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.7f),
                                modifier = Modifier.size(16.dp).clickable { searchQuery = ""; focusManager.clearFocus() })
                        }
                    }

                    // Stats strip
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.1f)).padding(horizontal = 4.dp, vertical = 10.dp),
                        Arrangement.SpaceEvenly
                    ) {
                        JobStatChip("📋", "${filtered.size}", "Total")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        JobStatChip("🆕", "${allJobs.count { it.isNew }}", "New")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        JobStatChip("🔴", "${filtered.count { (it.applyEndDate.parseToMillis().daysUntil()) in 0..7 }}", "Closing Soon")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        JobStatChip("🔖", "${savedJobs.size}", "Saved")
                    }
                }
            }

            // ── CATEGORY CHIPS ────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val sel = selectedCategory == null
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (sel) BpscColors.Primary else Color.White)
                            .border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = null }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("All", style = MaterialTheme.typography.bodyMedium,
                            color = if (sel) Color.White else BpscColors.TextSecondary,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                items(liveCategories) { cat ->
                    val sel = selectedCategory == cat
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (sel) cat?.color ?: BpscColors.Primary else Color.White)
                            .border(
                                1.dp,
                                if (sel) cat?.color ?: BpscColors.Primary else BpscColors.Divider,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedCategory = if (sel) null else cat
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        cat?.emoji?.let {
                            Text(it, fontSize = 13.sp)
                        }

                        cat?.label?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (sel) Color.White else BpscColors.TextSecondary,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── JOB LIST ──────────────────────────────────────
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔍", fontSize = 48.sp)
                        Text("No jobs found", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Try a different search or category", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                        if (vmState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BpscColors.Primary)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Featured
                    val featured = filtered.filter { it.isFeatured }
                    if (featured.isNotEmpty() && selectedCategory == null && searchQuery.isEmpty()) {
                        item { Text("⭐ Featured", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold) }
                        items(featured, key = { it.id }) { job ->
                            JobCard(job = job, isSaved = savedJobs.contains(job.id),
                                onSave = {
                                    viewModel.toggleSave(job.id)
                                },
                                        onClick  = { selectedJob = job })
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                        item { Text("All Jobs", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold) }
                    }
                    val rest = if (selectedCategory == null && searchQuery.isEmpty()) filtered.filter { !it.isFeatured } else filtered
                    items(rest, key = { it.id }) { job ->
                        JobCard(job = job, isSaved = savedJobs.contains(job.id),
                            onSave = {
                                viewModel.toggleSave(job.id)
                            },
                            onClick = { selectedJob = job })
                    }
                }
            }
        }

        // Job detail sheet
        selectedJob?.let { job ->
            JobDetailSheet(
                job       = job,
                isSaved   = savedJobs.contains(job.id),
                onSave    = { if (savedJobs.contains(job.id)) savedJobs.remove(job.id) else savedJobs.add(job.id) },
                onDismiss = { selectedJob = null }
            )
        }

        // Alert sheet
        if (showAlertSheet) {
            JobAlertSheet(
                categories = liveCategories,
                onDismiss  = { showAlertSheet = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// JOB CARD — all fields from DTO
// ─────────────────────────────────────────────────────────────
@Composable
private fun JobCard(
    job:     JobVacancyDto,
    isSaved: Boolean,
    onSave:  () -> Unit,
    onClick: () -> Unit,
) {
    val cat      = (job.category ?: "").toJobCategory()
    val endMs    = job.applyEndDate.parseToMillis()
    val daysLeft = endMs.daysUntil()
    val isUrgent  = daysLeft in 0..3
    val isClosing = daysLeft in 4..7
    val isPassed  = daysLeft < 0 && endMs > 0

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(if (job.isFeatured) 4.dp else 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Top: icon + title + save
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(cat.bg), Alignment.Center) {
                    Text(cat.emoji, fontSize = 22.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SmallBadge(cat.label, cat.color, cat.bg)
                        if (job.isNew)    SmallBadge("🆕 New",    BpscColors.Success,     Color(0xFFE8FDF4))
                        if (isUrgent)     SmallBadge("🔴 Urgent", Color(0xFFE74C3C),      Color(0xFFFEE8E8))
                        else if (isClosing) SmallBadge("⚡ Closing", Color(0xFFE67E22),   Color(0xFFFFF0EA))
                        if (isPassed)     SmallBadge("Closed",    BpscColors.TextHint,    BpscColors.Surface)
                    }
                    Text(job.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
                    if (!job.department.isNullOrBlank())
                        Text(job.department.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (isSaved) Color(0xFFFFF8E1) else BpscColors.Surface)
                    .clickable(onClick = onSave), Alignment.Center) {
                    Icon(if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        null, tint = if (isSaved) BpscColors.CoinGold else BpscColors.TextHint, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = BpscColors.Divider)

            // Info chips row — FIXED: shows all key fields
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if ((job.totalPosts ?: 0) > 0)
                    InfoChip(Icons.Rounded.Groups, "${job.totalPosts ?: 0} posts", Modifier.weight(1f, fill = false))
                if (!job.location.isNullOrBlank())
                    InfoChip(Icons.Rounded.LocationOn, job.location.orEmpty(), Modifier.weight(1f, fill = false))
                if (!job.qualification.isNullOrBlank())
                    InfoChip(Icons.Rounded.School, job.qualification.orEmpty(), Modifier.weight(1f, fill = false))
            }

            // Salary + age in second row if available
            if (!job.salaryRange.isNullOrBlank() || !job.ageLimit.isNullOrBlank()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!job.salaryRange.isNullOrBlank())
                        InfoChip(Icons.Rounded.CurrencyRupee, job.salaryRange.orEmpty(), Modifier.weight(1f, fill = false))
                    if (!job.ageLimit.isNullOrBlank())
                        InfoChip(Icons.Rounded.Cake, "Age: ${job.ageLimit.orEmpty()}", Modifier.weight(1f, fill = false))
                }
            }

            // Deadline countdown
            DeadlineBar(daysLeft = daysLeft, isPassed = isPassed, applyEndDate = job.applyEndDate)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// JOB DETAIL SHEET — fully wired to DTO with working Apply
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobDetailSheet(
    job:      JobVacancyDto,
    isSaved:  Boolean,
    onSave:   () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context    = LocalContext.current
    val cat        = (job.category ?: "").toJobCategory()
    val endMs      = job.applyEndDate.parseToMillis()
    val daysLeft   = endMs.daysUntil()
    val isPassed   = daysLeft < 0 && endMs > 0

    // BUG FIX: working URL launcher
    fun openLink(url: String) {
        if (url.isBlank()) return
        val uri = if (url.startsWith("http")) Uri.parse(url) else Uri.parse("https://$url")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {

            // ── Blue header ────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0)), Offset.Zero, Offset(400f, 200f)))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SmallBadge(cat.label, cat.color, cat.bg)
                        if (job.isNew) SmallBadge("🆕 New", BpscColors.Success, Color(0xFFE8FDF4))
                        if (isPassed)  SmallBadge("Application Closed", BpscColors.TextHint, Color.White.copy(0.2f))
                    }
                    Text(job.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                    if (!job.department.isNullOrBlank())
                        Text(job.department.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
                    DeadlineBar(daysLeft = daysLeft, isPassed = isPassed, applyEndDate = job.applyEndDate)
                }
            }

            // ── Scrollable body ────────────────────────────────
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                // Quick stats grid — FIXED: all 4 fields from DTO
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("👥", "${if ((job.totalPosts ?: 0) > 0) job.totalPosts else "—"}", "Posts",  Modifier.weight(1f))
                    StatBox("📍", job.location?.ifBlank { "—" } ?: "—",                          "Location", Modifier.weight(1f))
                    StatBox("💰", job.salaryRange?.ifBlank { "—" } ?: "—",                       "Salary",   Modifier.weight(1f))
                    StatBox("🎂", job.ageLimit?.ifBlank { "—" } ?: "—",                          "Age Limit",Modifier.weight(1f))
                }

                // Description — FIXED: now wired to DTO field
                if (!job.description.isNullOrBlank()) {
                    SectionCard(title = "About this Job") {
                        Text(job.description.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, lineHeight = 24.sp)
                    }
                }

                // Eligibility — FIXED: all fields rendered
                SectionCard(title = "Eligibility & Details", accentColor = BpscColors.Primary, accentBg = BpscColors.PrimaryLight) {
                    if (!job.qualification.isNullOrBlank()) DetailRow("🎓", "Qualification", job.qualification.orEmpty())
                    if (!job.ageLimit.isNullOrBlank())      DetailRow("🎂", "Age Limit",     job.ageLimit.orEmpty())
                    if (!job.location.isNullOrBlank())      DetailRow("📍", "Location",      job.location.orEmpty())
                    if (!job.salaryRange.isNullOrBlank())   DetailRow("💰", "Salary",        job.salaryRange.orEmpty())
                    if ((job.totalPosts ?: 0) > 0)             DetailRow("👥", "Total Posts",   "${job.totalPosts ?: 0}")
                    if (!job.nearbyDistricts.isNullOrEmpty()) DetailRow("📌", "Districts",   job.nearbyDistricts?.joinToString(", ").orEmpty().orEmpty())
                }

                // Important dates — FIXED: all date fields
                SectionCard(title = "Important Dates") {
                    val dates = buildList {
                        if (!job.notificationDate.isNullOrBlank())
                            add(Triple("📢", "Notification",  job.notificationDate.formatDisplay()))
                        if (!job.applyStartDate.isNullOrBlank())
                            add(Triple("▶️", "Apply Start",   job.applyStartDate.formatDisplay()))
                        add(Triple("🔴", "Last Date",      job.applyEndDate.formatDisplay()))
                        if (!job.examDate.isNullOrBlank())
                            add(Triple("📝", "Exam Date",    job.examDate.formatDisplay()))
                    }
                    TimelineView(dates)
                }

                // Exam tags
                if (!job.officialLink.isNullOrBlank()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BpscColors.Surface).padding(12.dp),
                        Arrangement.spacedBy(8.dp), Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Link, null, tint = BpscColors.Primary, modifier = Modifier.size(16.dp))
                        Text(job.officialLink, style = MaterialTheme.typography.bodySmall, color = BpscColors.Primary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── Bottom actions — FIXED: Apply button now works ──
            HorizontalDivider(color = BpscColors.Divider)
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick  = onSave,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, if (isSaved) BpscColors.CoinGold else BpscColors.Divider),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = if (isSaved) BpscColors.CoinGold else BpscColors.TextSecondary)
                ) {
                    Icon(if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSaved) "Saved" else "Save", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    // BUG FIX: actually opens the URL
                    onClick  = { openLink(job.officialLink.orEmpty()) },
                    enabled  = !job.officialLink.isNullOrBlank() && !isPassed,
                    modifier = Modifier.weight(2f).height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = if (isPassed) BpscColors.TextHint else BpscColors.Primary,
                        disabledContainerColor = BpscColors.Divider
                    )
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isPassed) "Application Closed"
                        else if (job.officialLink.isNullOrBlank()) "No Link Available"
                        else "Apply / Official Site",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// JOB ALERT SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobAlertSheet(
    categories: List<JobCategory?>,
    onDismiss:  () -> Unit,
) {
    val selected = remember { mutableStateListOf<String>() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Job Alerts", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                    Text("Get notified when new vacancies are posted", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
                Icon(Icons.Rounded.Notifications, null, tint = BpscColors.CoinGold, modifier = Modifier.size(26.dp))
            }
            HorizontalDivider(color = BpscColors.Divider)
            categories.forEach { cat ->
                val isOn = selected.contains(cat?.label)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isOn)
                                cat?.bg ?: BpscColors.Surface
                            else
                                BpscColors.Surface
                        )
                        .clickable {
                            if (isOn)
                                selected.remove(cat?.label)
                            else
                                cat?.label?.let { selected.add(it) }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    cat?.emoji?.let {
                        Text(it, fontSize = 20.sp)
                    }

                    cat?.label?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    cat?.let {
                        Switch(
                            checked = isOn,
                            onCheckedChange = {
                                if (isOn)
                                    selected.remove(cat.label)
                                else
                                    selected.add(cat.label)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = it.color
                            )
                        )
                    }
                }
            }
            if (selected.isNotEmpty()) {
                Text("✅ Alerts on: ${selected.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Success, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text("Done", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SMALL HELPERS
// ─────────────────────────────────────────────────────────────

@Composable
private fun SmallBadge(text: String, fg: Color, bg: Color) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.Bold, fontSize = 9.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp))
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(BpscColors.Surface).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = BpscColors.TextSecondary, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatBox(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(BpscColors.Surface).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(icon, fontSize = 16.sp)
        Text(value, style = MaterialTheme.typography.titleSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
    }
}

@Composable
private fun SectionCard(
    title:       String,
    accentColor: Color = BpscColors.TextPrimary,
    accentBg:    Color = Color.White,
    content:     @Composable ColumnScope.() -> Unit
) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CardDefaults.cardColors(containerColor = accentBg), CardDefaults.cardElevation(if (accentBg == Color.White) 1.dp else 0.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = accentColor, fontWeight = FontWeight.ExtraBold)
            content()
        }
    }
}

@Composable
private fun DetailRow(icon: String, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 14.sp, modifier = Modifier.padding(top = 1.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun TimelineView(steps: List<Triple<String, String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        steps.forEachIndexed { index, (icon, label, date) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(BpscColors.PrimaryLight), Alignment.Center) {
                        Text(icon, fontSize = 13.sp)
                    }
                    if (index < steps.size - 1) Box(Modifier.width(2.dp).height(24.dp).background(BpscColors.PrimaryLight))
                }
                Column(Modifier.padding(top = 5.dp, bottom = if (index < steps.size - 1) 10.dp else 0.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(date,  style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DeadlineBar(daysLeft: Long, isPassed: Boolean, applyEndDate: String?) {
    val (bg, fg, text) = when {
        isPassed     -> Triple(Color(0xFFF2F3F4), BpscColors.TextHint, "Application closed · ${applyEndDate.formatDisplay()}")
        daysLeft <= 3  -> Triple(Color(0xFFFEE8E8), Color(0xFFE74C3C), "🔴 ${daysLeft}d left — Apply now!")
        daysLeft <= 7  -> Triple(Color(0xFFFFF0EA), Color(0xFFE67E22), "⚡ ${daysLeft} days left")
        daysLeft <= 30 -> Triple(Color(0xFFFFF8E1), BpscColors.CoinGold, "📅 Last date: ${applyEndDate.formatDisplay()}")
        else           -> Triple(BpscColors.PrimaryLight, BpscColors.Primary, "📅 Last date: ${applyEndDate.formatDisplay()}")
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 12.dp, vertical = 7.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = fg, fontWeight = FontWeight.SemiBold)
        if (!isPassed) Text("View →", style = MaterialTheme.typography.bodyMedium, color = fg, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun JobStatChip(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(64.dp)) {
        Text(icon, fontSize = 13.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
    }
}