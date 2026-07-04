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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.core.ads.BannerAdView
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.AppErrorState
import com.example.bpscnotes.core.ui.sheetFlickerFix
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.JobVacancyDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
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

internal fun String.toJobCategory(): JobCategory = when (this.trim()) {
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
    viewModel: JobVacanciesViewModel = hiltViewModel(),
    adManager: AdManager
) {
    val vmState by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("job_vacancies") }

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = vmState.isLoading && vmState.jobs.isNotEmpty(),
        onRefresh = { viewModel.load() }
    ) {    var searchQuery      by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf<JobCategory?>(null) }
        // QA issue 14: saved jobs had no page to view them — the 🔖 Saved
        // chip below filters the list to bookmarked jobs.
        var showSavedOnly    by remember { mutableStateOf(false) }
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

        val filtered = remember(allJobs, searchQuery, selectedCategory, showSavedOnly) {
            allJobs.filter { job ->
                val matchCat    = selectedCategory == null || (job.category ?: "").toJobCategory() == selectedCategory
                val matchSaved  = !showSavedOnly || job.isSaved
                val matchSearch = searchQuery.isEmpty() ||
                        job.title.contains(searchQuery, true) ||
                        job.department?.contains(searchQuery, true) == true ||
                        job.location?.contains(searchQuery, true) == true ||
                        job.qualification?.contains(searchQuery, true) == true
                matchCat && matchSaved && matchSearch
            }.sortedWith(compareBy { it.applyEndDate.parseToMillis() })
        }

        if (vmState.isLoading && allJobs.isEmpty()) {
            AppLoader()
            return@PullToRefreshBox
        }

        if (vmState.error != null && allJobs.isEmpty()) {
            AppErrorState(message = vmState.error!!, onRetry = { viewModel.retry() })
            return@PullToRefreshBox
        }

        Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
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
                                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStackSafe() }, Alignment.Center) {
                                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text(str.jobsTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    Text("${allJobs.size} ${str.jobsOpeningsCountLabel}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
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
                                    if (searchQuery.isEmpty()) Text(str.jobsSearchHint, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.5f))
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
                            JobStatChip("📋", "${allJobs.size}", "Total")
                            Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                            JobStatChip("🆕", "${allJobs.count { it.isNew }}", "New")
                            Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                            JobStatChip("🔴", "${allJobs.count { (it.applyEndDate.parseToMillis().daysUntil()) in 0..7 }}", "Closing Soon")
                            Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                            JobStatChip("🔖", "${savedJobs.size}", str.jobsSaved)
                        }
                    }
                }

                // ── CATEGORY CHIPS ────────────────────────────────
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val sel = selectedCategory == null && !showSavedOnly
                        Box(
                            Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (sel) BpscColors.Primary else Color.White)
                                .border(1.dp, if (sel) BpscColors.Primary else cs.outline, RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = null; showSavedOnly = false }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(str.filterAll, style = MaterialTheme.typography.bodyMedium,
                                color = if (sel) Color.White else BpscColors.TextSecondary,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    item {
                        val sel = showSavedOnly
                        Row(
                            Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (sel) BpscColors.Primary else Color.White)
                                .border(1.dp, if (sel) BpscColors.Primary else cs.outline, RoundedCornerShape(20.dp))
                                .clickable { showSavedOnly = !showSavedOnly }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("🔖", fontSize = 13.sp)
                            Text(str.jobsSaved, style = MaterialTheme.typography.bodyMedium,
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
                                    if (sel) cat?.color ?: BpscColors.Primary else cs.outline,
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
                            Text(str.jobsNoJobs, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                            Text(str.jobsTryFilter, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                            if (vmState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BpscColors.Primary)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Source disclaimer — Play policy: govt info must cite official sources
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF8E1),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "ℹ️ Job details are sourced from official government websites (bpsc.bihar.gov.in, bssc.bihar.gov.in, etc.). " +
                                    "BPSCNotes is not a government app. Verify on the official site before applying.",
                                    fontSize = 11.sp, color = Color(0xFF6D4C41), lineHeight = 16.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                        }
                        // Featured
                        val featured = filtered.filter { it.isFeatured }
                        if (featured.isNotEmpty() && selectedCategory == null && searchQuery.isEmpty()) {
                            item { Text(str.jobsFeatured, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold) }
                            items(featured, key = { it.id }) { job ->
                                JobCard(job = job, isSaved = savedJobs.contains(job.id),
                                    onSave = {
                                        viewModel.toggleSave(job.id)
                                    },
                                    onClick  = { navController.navigate(Screen.JobDetail.createRoute(job.id)) })
                            }
                            item { Spacer(Modifier.height(4.dp)) }
                            item { Text(str.jobsAllJobs, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold) }
                        }
                        val rest = if (selectedCategory == null && searchQuery.isEmpty()) filtered.filter { !it.isFeatured } else filtered
                        itemsIndexed(rest, key = { _, it -> it.id }) { index, job ->
                            // Banner ad every 5 job cards — unobtrusive, between items
                            if (index > 0 && index % 5 == 0) {
                                BannerAdView(adUnitId = adManager.getBannerAdUnitId())
                            }
                            JobCard(job = job, isSaved = savedJobs.contains(job.id),
                                onSave  = { viewModel.toggleSave(job.id) },
                                onClick = { navController.navigate(Screen.JobDetail.createRoute(job.id)) })
                        }
                    }
                }
            }

            // Alert sheet
            if (showAlertSheet) {
                JobAlertSheet(
                    categories      = liveCategories,
                    selectedLabels  = vmState.alertCategories,
                    onToggle        = { viewModel.toggleAlertCategory(it) },
                    onDismiss       = { showAlertSheet = false }
                )
            }
        }
    } // end PullToRefreshBox
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val cat      = (job.category ?: "").toJobCategory()
    val endMs    = job.applyEndDate.parseToMillis()
    val daysLeft = endMs.daysUntil()
    val isUrgent  = daysLeft in 0..3
    val isClosing = daysLeft in 4..7
    val isPassed  = daysLeft < 0 && endMs > 0

    // Card border turns red when deadline is urgent (≤3 days)
    val cardBorderColor = when {
        isUrgent  -> Color(0xFFE74C3C)
        isClosing -> Color(0xFFE67E22)
        else      -> Color.Transparent
    }
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .then(if (isUrgent || isClosing)
                Modifier.border(1.5.dp, cardBorderColor, RoundedCornerShape(18.dp))
            else Modifier),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = if (isUrgent) Color(0xFFFFF5F5) else cs.surface),
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
                        if (job.isNew)    SmallBadge(str.jobsNew,    BpscColors.Success,     Color(0xFFE8FDF4))
                        if (isUrgent)     SmallBadge("🔴 Urgent", Color(0xFFE74C3C),      Color(0xFFFEE8E8))
                        else if (isClosing) SmallBadge("⚡ Closing", Color(0xFFE67E22),   Color(0xFFFFF0EA))
                        if (isPassed)     SmallBadge("Closed",    BpscColors.TextHint,    BpscColors.Surface)
                    }
                    Text(job.title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
                    if (!job.department.isNullOrBlank())
                        Text(job.department.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (isSaved) Color(0xFFFFF8E1) else BpscColors.Surface)
                    .clickable(onClick = onSave), Alignment.Center) {
                    Icon(if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        null, tint = if (isSaved) BpscColors.CoinGold else BpscColors.TextHint, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = cs.outline)

            // Info chips row — FIXED: shows all key fields
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if ((job.totalPosts ?: 0) > 0)
                    InfoChip(Icons.Rounded.Groups, "${job.totalPosts ?: 0} posts", Modifier.weight(1f, fill = false))
                if (!job.location.isNullOrBlank())
                    InfoChip(Icons.Rounded.LocationOn, job.location.orEmpty(), Modifier.weight(1f, fill = false))
                if (!job.qualification.isNullOrBlank())
                    InfoChip(Icons.Rounded.School, job.qualification.orEmpty(), Modifier.weight(1f, fill = false))
            }

            // Salary + age + experience in second row if available
            val expCard = job.experienceRequired?.trim()
            if (!job.salaryRange.isNullOrBlank() || !job.ageLimit.isNullOrBlank() || (!expCard.isNullOrBlank() && expCard != "Any")) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!job.salaryRange.isNullOrBlank())
                        InfoChip(Icons.Rounded.CurrencyRupee, job.salaryRange.orEmpty(), Modifier.weight(1f, fill = false))
                    if (!job.ageLimit.isNullOrBlank())
                        InfoChip(Icons.Rounded.Cake, "Age: ${job.ageLimit.orEmpty()}", Modifier.weight(1f, fill = false))
                    if (!expCard.isNullOrBlank() && expCard != "Any")
                        InfoChip(Icons.Rounded.WorkHistory, expCard, Modifier.weight(1f, fill = false))
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
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
                        if (job.isNew) SmallBadge(str.jobsNew, BpscColors.Success, Color(0xFFE8FDF4))
                        if (isPassed)  SmallBadge(str.jobsApplicationClosed, BpscColors.TextHint, Color.White.copy(0.2f))
                    }
                    Text(job.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                    if (!job.department.isNullOrBlank())
                        Text(job.department.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
                    DeadlineBar(daysLeft = daysLeft, isPassed = isPassed, applyEndDate = job.applyEndDate)
                }
            }

            // ── Scrollable body ────────────────────────────────
            val jobSheetScroll = rememberScrollState()
            Column(
                Modifier.weight(1f, fill = false)
                    .sheetFlickerFix(jobSheetScroll)
                    .verticalScroll(jobSheetScroll).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                // Quick stats: 4-column grid
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("👥", "${if ((job.totalPosts ?: 0) > 0) job.totalPosts else "—"}", str.jobsPosts,  Modifier.weight(1f))
                    StatBox("📍", job.location?.ifBlank { "—" } ?: "—",                          "Location", Modifier.weight(1f))
                    StatBox("💰", job.salaryRange?.ifBlank { "—" } ?: "—",                       "Salary",   Modifier.weight(1f))
                    StatBox("🎂", job.ageLimit?.ifBlank { "—" } ?: "—",                          "Age Limit",Modifier.weight(1f))
                }

                // Experience Required — prominent display when set
                val exp = job.experienceRequired?.trim()
                if (!exp.isNullOrBlank() && exp != "Any") {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F4FF))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.WorkHistory, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Experience Required", style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.TextHint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(exp, style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // Description
                if (!job.description.isNullOrBlank()) {
                    SectionCard(title = str.jobsAboutJob) {
                        Text(job.description.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, lineHeight = 24.sp)
                    }
                }
                if (!job.briefDescription.isNullOrBlank()) {
                    SectionCard(title = "Brief Description") {
                        Text(job.briefDescription.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, lineHeight = 24.sp)
                    }
                }

                // Advertisement PDF — top-level download button
                if (!job.advertPdfUrl.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEE8E8))
                            .clickable { openLink(job.advertPdfUrl.orEmpty()) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Advertisement PDF", style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE74C3C), fontWeight = FontWeight.ExtraBold)
                            Text("Tap to view or download the official advertisement",
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.Download, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(20.dp))
                    }
                }

                // Notification PDF
                if (!job.pdfUrl.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEE8E8))
                            .clickable { openLink(job.pdfUrl.orEmpty()) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Official Notification PDF", style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE74C3C), fontWeight = FontWeight.SemiBold)
                            Text("Tap to download / view PDF", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.Download, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(18.dp))
                    }
                }

                // Apply link
                if (!job.officialLink.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BpscColors.PrimaryLight)
                            .clickable { openLink(job.officialLink.orEmpty()) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Apply Online", style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.Primary, fontWeight = FontWeight.SemiBold)
                            Text(job.officialLink.orEmpty(), style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Rounded.KeyboardArrowRight, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                    }
                }

                // Eligibility
                SectionCard(title = str.jobsEligibility, accentColor = BpscColors.Primary, accentBg = BpscColors.PrimaryLight) {
                    if (!job.qualification.isNullOrBlank()) DetailRow("🎓", "Qualification", job.qualification.orEmpty())
                    if (!exp.isNullOrBlank() && exp != "Any") DetailRow("⏱️", "Experience", exp)
                    if (!job.ageLimit.isNullOrBlank())      DetailRow("🎂", "Age Limit",     job.ageLimit.orEmpty())
                    if (!job.location.isNullOrBlank())      DetailRow("📍", "Location",      job.location.orEmpty())
                    if (!job.salaryRange.isNullOrBlank())   DetailRow("💰", "Salary",        job.salaryRange.orEmpty())
                    if ((job.totalPosts ?: 0) > 0)          DetailRow("👥", "Total Posts",   "${job.totalPosts ?: 0}")
                    if (!job.nearbyDistricts.isNullOrEmpty()) DetailRow("📌", "Districts",   job.nearbyDistricts?.joinToString(", ").orEmpty())
                }

                // Important dates
                SectionCard(title = str.jobsImportantDates) {
                    val dates = buildList {
                        if (!job.notificationDate.isNullOrBlank()) add(Triple("📢", "Notification", job.notificationDate.formatDisplay()))
                        if (!job.applyStartDate.isNullOrBlank())   add(Triple("▶️", str.jobsApplyStart, job.applyStartDate.formatDisplay()))
                        add(Triple("🔴", str.jobsLastDate, job.applyEndDate.formatDisplay()))
                        if (!job.examDate.isNullOrBlank())         add(Triple("📝", "Exam Date", job.examDate.formatDisplay()))
                    }
                    TimelineView(dates)
                }
            }

            // ── Bottom actions — FIXED: Apply button now works ──
            HorizontalDivider(color = cs.outline)
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick  = onSave,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, if (isSaved) BpscColors.CoinGold else cs.outline),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = if (isSaved) BpscColors.CoinGold else BpscColors.TextSecondary)
                ) {
                    Icon(if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSaved) str.jobsSaved else str.jobsSave, style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    // BUG FIX: actually opens the URL
                    onClick  = { openLink(job.officialLink.orEmpty()) },
                    enabled  = !job.officialLink.isNullOrBlank() && !isPassed,
                    modifier = Modifier.weight(2f).height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = if (isPassed) BpscColors.TextHint else BpscColors.Primary,
                        disabledContainerColor = cs.outline
                    )
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isPassed) str.jobsApplicationClosed
                        else if (job.officialLink.isNullOrBlank()) str.jobsNoLink
                        else str.jobsApplyOfficialSite,
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
    categories:     List<JobCategory?>,
    selectedLabels: Set<String>,
    onToggle:       (String) -> Unit,
    onDismiss:      () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
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
                    Text(str.jobsAlerts, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                    Text(str.jobsAlertsSubtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
                // Alert icon removed — will be dynamic when job alert API is ready
            }
            HorizontalDivider(color = cs.outline)
            categories.forEach { cat ->
                val isOn = cat?.label != null && selectedLabels.contains(cat.label)
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
                            cat?.label?.let { onToggle(it) }
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
                            color = cs.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    cat?.let {
                        Switch(
                            checked = isOn,
                            onCheckedChange = {
                                cat.label?.let { label -> onToggle(label) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = it.color
                            )
                        )
                    }
                }
            }
            if (selectedLabels.isNotEmpty()) {
                Text("✅ Alerts on: ${selectedLabels.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Success, fontWeight = FontWeight.SemiBold)
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
    val cs = MaterialTheme.colorScheme
    Text(text, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.Bold, fontSize = 9.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp))
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(cs.background).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = BpscColors.TextSecondary, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatBox(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(cs.background).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(icon, fontSize = 16.sp)
        Text(value, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold,
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CardDefaults.cardColors(containerColor = accentBg), CardDefaults.cardElevation(if (accentBg == Color.White) 1.dp else 0.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = accentColor, fontWeight = FontWeight.ExtraBold)
            content()
        }
    }
}

@Composable
private fun DetailRow(icon: String, label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 14.sp, modifier = Modifier.padding(top = 1.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun TimelineView(steps: List<Triple<String, String, String>>) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
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
                    Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(date,  style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DeadlineBar(daysLeft: Long, isPassed: Boolean, applyEndDate: String?) {
    val str = LocalStrings.current
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
        if (!isPassed) Text("${str.materialsView} →", style = MaterialTheme.typography.bodyMedium, color = fg, fontWeight = FontWeight.ExtraBold)
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