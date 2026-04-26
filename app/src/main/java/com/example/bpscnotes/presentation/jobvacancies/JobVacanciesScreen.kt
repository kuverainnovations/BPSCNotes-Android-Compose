package com.example.bpscnotes.presentation.jobvacancies

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.JobVacancyDto
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────
enum class JobCategory(val label: String, val emoji: String, val color: Color, val bg: Color) {
    BPSC       ("BPSC",          "🎯", Color(0xFF1565C0), Color(0xFFE8F0FD)),
    BiharGovt  ("Bihar Govt",    "🏛️", Color(0xFF2ECC71), Color(0xFFE8FDF4)),
    CentralGovt("Central Govt",  "🇮🇳", Color(0xFF9B59B6), Color(0xFFF3E8FD)),
    Private    ("Private",       "🏢", Color(0xFFE67E22), Color(0xFFFFF0EA)),
    PartTime   ("Part-time",     "⏰", Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
}

data class JobTimeline(
    val notificationDate: String?="",
    val applyStart: String?="",
    val applyEnd: String?="",
    val examDate: String?="",
)

data class JobVacancy(
    val id: String,
    val title: String,
    val department: String?="",
    val category: JobCategory,
    val totalPosts: Int,
    val location: String,
    val salaryRange: String,
    val qualification: String,
    val ageLimit: String,
    val applyEndDate: Long,        // epoch millis
    val timeline: JobTimeline,
    val officialLink: String,
    val isNew: Boolean = false,
    val isFeatured: Boolean = false,
    val isPartTime: Boolean = false,
    val workingHours: String? = null,   // for part-time
    val nearbyDistricts: List<String>? = emptyList(),
    val description: String,
)

// epoch helper — days from now
private fun daysFromNow(days: Int): Long {
    return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
}

/*
val mockJobs = listOf(
    JobVacancy("j1", "BPSC 70th Combined Competitive Exam",
        "Bihar Public Service Commission", JobCategory.BPSC, 1929,
        "Bihar (All Districts)", "₹56,100 – ₹2,08,700/month",
        "Graduation in any discipline", "21–37 years",
        daysFromNow(12),
        JobTimeline("15 Jan 2026", "01 Feb 2026", "28 Feb 2026", "June 2026"),
        "https://bpsc.bih.nic.in", isNew = true, isFeatured = true,
        description = "BPSC 70th CCE is one of the most prestigious state civil services exams. Successful candidates are appointed as Deputy Collector, DSP, Block Development Officer and other Group A & B posts."),

    JobVacancy("j2", "Bihar Police Sub-Inspector (SI) Recruitment",
        "Bihar Police", JobCategory.BiharGovt, 2446,
        "Bihar (All Districts)", "₹35,400 – ₹1,12,400/month",
        "Graduation + Physical Standards", "21–37 years",
        daysFromNow(3),
        JobTimeline("10 Jan 2026", "15 Jan 2026", "17 Mar 2026", "July 2026"),
        "https://csbc.bih.nic.in", isNew = true, isFeatured = true,
        description = "Bihar Police SI recruitment for the post of Sub-Inspector in Bihar Police. Candidates must clear written exam, physical test and medical examination."),

    JobVacancy("j3", "SSC CGL 2026 Notification",
        "Staff Selection Commission", JobCategory.CentralGovt, 17727,
        "All India", "₹25,500 – ₹1,51,100/month",
        "Graduation in any discipline", "18–32 years",
        daysFromNow(25),
        JobTimeline("20 Jan 2026", "01 Feb 2026", "25 Mar 2026", "Aug-Sep 2026"),
        "https://ssc.nic.in", isNew = true,
        description = "SSC CGL recruitment for various Group B and Group C posts across central government ministries, departments and organizations."),

    JobVacancy("j4", "Bihar STET 2026 – Secondary Teacher",
        "Bihar School Examination Board", JobCategory.BiharGovt, 7279,
        "Bihar (All Districts)", "₹28,900 – ₹91,300/month",
        "Graduation + B.Ed", "21–40 years",
        daysFromNow(18),
        JobTimeline("05 Feb 2026", "10 Feb 2026", "05 Mar 2026", "May 2026"),
        "https://bseb.ac.in",
        description = "Bihar State Teacher Eligibility Test for recruitment of Secondary Teachers (Class 9-10) in government schools across Bihar."),

    JobVacancy("j5", "RRB NTPC 2026 – Railway Jobs",
        "Railway Recruitment Board", JobCategory.CentralGovt, 11558,
        "All India (Zone-wise)", "₹19,900 – ₹92,300/month",
        "12th Pass / Graduation (Post-wise)", "18–33 years",
        daysFromNow(45),
        JobTimeline("01 Mar 2026", "01 Mar 2026", "30 Apr 2026", "Oct 2026"),
        "https://rrbapply.gov.in",
        description = "RRB NTPC recruitment for various Non-Technical Popular Category posts including Junior Clerk, Accounts Clerk, Station Master, Goods Guard etc."),

    JobVacancy("j6", "Tata Consultancy Services – Process Associate",
        "TCS BPS, Patna", JobCategory.Private, 150,
        "Patna, Bihar", "₹15,000 – ₹22,000/month",
        "Graduation (Any), Basic Computer Skills", "18–28 years",
        daysFromNow(8),
        JobTimeline("01 Feb 2026", "01 Feb 2026", "22 Feb 2026", null),
        "https://tcs.com/careers", isNew = true,
        description = "TCS BPS is hiring Process Associates for its Patna delivery center. Role involves data entry, document processing and basic back-office operations."),

    JobVacancy("j7", "Part-time Library Assistant – Near You",
        "District Public Library, Patna", JobCategory.PartTime, 12,
        "Patna, Muzaffarpur, Gaya", "₹8,000 – ₹12,000/month",
        "12th Pass, Reading Interest", "18–35 years",
        daysFromNow(5),
        JobTimeline("10 Feb 2026", "10 Feb 2026", "28 Feb 2026", null),
        "https://biharlib.gov.in", isNew = true, isPartTime = true,
        workingHours = "4 hrs/day · Flexible timings",
        nearbyDistricts = listOf("Patna", "Muzaffarpur", "Gaya", "Bhagalpur"),
        description = "Part-time Library Assistant roles at District Public Libraries. Ideal for students preparing for competitive exams. Flexible 4-hour shifts with access to library resources."),

    JobVacancy("j8", "Part-time Data Entry Operator – Remote",
        "Bihar e-Governance Society", JobCategory.PartTime, 200,
        "Work from Home (Bihar)", "₹10,000 – ₹15,000/month",
        "12th Pass, Typing Speed 25 WPM", "18–35 years",
        daysFromNow(15),
        JobTimeline("15 Feb 2026", "15 Feb 2026", "07 Mar 2026", null),
        "https://bih.nic.in", isNew = true, isPartTime = true,
        workingHours = "3-4 hrs/day · Work from home",
        nearbyDistricts = listOf("All Districts"),
        description = "Remote data entry positions for Bihar e-Governance Society. Perfect for exam aspirants who need flexible income. Work from home with your own device."),

    JobVacancy("j9", "Bihar Vidhan Sabha Secretariat Recruitment",
        "Bihar Legislative Assembly", JobCategory.BiharGovt, 65,
        "Patna, Bihar", "₹44,900 – ₹1,42,400/month",
        "Graduation + Hindi/English Typing", "21–37 years",
        daysFromNow(30),
        JobTimeline("20 Feb 2026", "01 Mar 2026", "31 Mar 2026", "June 2026"),
        "https://vidhansabha.bih.nic.in",
        description = "Bihar Vidhan Sabha Secretariat recruitment for various posts including PA, Stenographer, Assistant, and Security Personnel."),
)
*/

val jobAlertCategories = listOf("BPSC", "Bihar Govt", "Central Govt", "Private", "Part-time")


private fun JobVacancyDto.toUiModel(): JobVacancy {
    val cat = when (category) {
        "BPSC"         -> JobCategory.BPSC
        "Bihar Govt"   -> JobCategory.BiharGovt
        "Central Govt" -> JobCategory.CentralGovt
        "Private"      -> JobCategory.Private
        "Part-time"    -> JobCategory.PartTime
        else           -> JobCategory.BiharGovt
    }

    val endMillis = try {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .parse(applyEndDate)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    return JobVacancy(
        id = id,
        title = title,
        department = department,
        category = cat,
        totalPosts = totalPosts?: 0,
        location = location ?: "Unknown",
        salaryRange = salaryRange?: "",
        qualification = qualification?: "",
        ageLimit = ageLimit?: "",
        applyEndDate = endMillis ?: 0L,
        timeline = JobTimeline(
            notificationDate = notificationDate,
            applyStart = applyStartDate,
            applyEnd = applyEndDate,
            examDate = examDate
        ),
        officialLink = officialLink?: "",
        isNew = isNew,
        isFeatured = isFeatured,
        nearbyDistricts = nearbyDistricts,
        description = ""
    )
}


// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun JobVacanciesScreen(navController: NavHostController ,
                       viewModel: JobVacanciesViewModel = hiltViewModel()
) {

    val vmState by viewModel.uiState.collectAsState()




    var searchQuery      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<JobCategory?>(null) }
    var selectedJob      by remember { mutableStateOf<JobVacancy?>(null) }
    val savedJobs        = remember { mutableStateListOf<String>() }
    val alertCategories  = remember { mutableStateListOf<String>() }
    var showAlertSheet   by remember { mutableStateOf(false) }
    val focusManager     = LocalFocusManager.current

    val allJobs = remember(vmState.jobs) {
        vmState.jobs.map { it.toUiModel() }
    }

    val filtered = allJobs.filter { job ->
        val matchesCat = selectedCategory == null || job.category == selectedCategory
        val matchesSearch = searchQuery.isEmpty() ||
                job.title.contains(searchQuery, true) ||
                job.department?.contains(searchQuery, true) == true ||
                job.location.contains(searchQuery, true)

        matchesCat && matchesSearch
    }.sortedBy { it.applyEndDate }

    if (vmState.isLoading && allJobs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (vmState.error != null && allJobs.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(vmState.error!!)
            Button(onClick = { viewModel.retry() }) {
                Text("Retry")
            }
        }
        return
    }


    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                        Offset(0f, 0f), Offset(400f, 400f)
                    ))
                    .statusBarsPadding()
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
                    drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    // Top row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                                    .clickable { navController.popBackStack() },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                            Column {
                                Text("Job Vacancies", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Latest govt & private jobs", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                        // Alert bell
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (alertCategories.isNotEmpty()) Color(0xFFFFF8E1) else Color.White.copy(0.15f))
                                .clickable { showAlertSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (alertCategories.isNotEmpty()) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationAdd,
                                null,
                                tint = if (alertCategories.isNotEmpty()) BpscColors.CoinGold else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            if (alertCategories.isNotEmpty()) {
                                Box(
                                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                                        .size(14.dp).clip(CircleShape).background(Color(0xFFE74C3C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${alertCategories.size}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Search bar
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.15f))
                            .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) Text("Search jobs, departments...", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.5f))
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.7f),
                                modifier = Modifier.size(16.dp).clickable { searchQuery = ""; focusManager.clearFocus() })
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Stats strip
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.1f))
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        JobStatChip("📋", "${filtered.size}", "Jobs")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        JobStatChip("🆕", "${filtered.count { it.isNew }}", "New")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        JobStatChip("🔴", "${filtered.count { daysUntil(it.applyEndDate) <= 7 }}", "Closing Soon")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        JobStatChip("🔖", "${savedJobs.size}", "Saved")
                    }
                }
            }

            // ── Category filter chips ─────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val sel = selectedCategory == null
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
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
                items(JobCategory.values()) { cat ->
                    val sel = selectedCategory == cat
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (sel) cat.color else Color.White)
                            .border(1.dp, if (sel) cat.color else BpscColors.Divider, RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = if (sel) null else cat }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(cat.emoji, fontSize = 13.sp)
                        Text(cat.label, style = MaterialTheme.typography.bodyMedium,
                            color = if (sel) Color.White else BpscColors.TextSecondary,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            // ── Job list ──────────────────────────────────────
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔍", fontSize = 48.sp)
                        Text("No jobs found", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Try a different search or filter", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Featured section
                    val featured = filtered.filter { it.isFeatured }
                    if (featured.isNotEmpty() && selectedCategory == null && searchQuery.isEmpty()) {
                        item {
                            Text("⭐ Featured", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                        }
                        items(featured) { job ->
                            JobCard(job = job, isSaved = savedJobs.contains(job.id),
                                onSave = { if (savedJobs.contains(job.id)) savedJobs.remove(job.id) else savedJobs.add(job.id) },
                                onViewDetail = { selectedJob = job })
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                        item { Text("All Jobs", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold) }
                    }

                    items(if (selectedCategory == null && searchQuery.isEmpty()) filtered.filter { !it.isFeatured } else filtered) { job ->
                        JobCard(job = job, isSaved = savedJobs.contains(job.id),
                            onSave = { if (savedJobs.contains(job.id)) savedJobs.remove(job.id) else savedJobs.add(job.id) },
                            onViewDetail = { selectedJob = job })
                    }
                }
            }
        }

        // Job detail bottom sheet
        selectedJob?.let { job ->
            JobDetailSheet(
                job      = job,
                isSaved  = savedJobs.contains(job.id),
                onSave   = { if (savedJobs.contains(job.id)) savedJobs.remove(job.id) else savedJobs.add(job.id) },
                onDismiss = { selectedJob = null }
            )
        }

        // Alert settings sheet
        if (showAlertSheet) {
            JobAlertSheet(
                alertCategories = alertCategories,
                onToggle = { cat -> if (alertCategories.contains(cat)) alertCategories.remove(cat) else alertCategories.add(cat) },
                onDismiss = { showAlertSheet = false },
                allJobs=allJobs
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// JOB CARD
// ─────────────────────────────────────────────────────────────
@Composable
private fun JobCard(
    job: JobVacancy,
    isSaved: Boolean,
    onSave: () -> Unit,
    onViewDetail: () -> Unit,
) {
    val daysLeft   = daysUntil(job.applyEndDate)
    val isUrgent   = daysLeft <= 3
    val isClosing  = daysLeft in 4..7

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onViewDetail),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(if (job.isFeatured) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category icon box
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(job.category.bg),
                    contentAlignment = Alignment.Center
                ) { Text(job.category.emoji, fontSize = 22.sp) }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Badges row
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(job.category.label, style = MaterialTheme.typography.labelSmall, color = job.category.color, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(job.category.bg).padding(horizontal = 7.dp, vertical = 2.dp))
                        if (job.isNew) Text("🆕 New", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE8FDF4)).padding(horizontal = 7.dp, vertical = 2.dp))
                        if (isUrgent) Text("🔴 Urgent", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE74C3C), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFEE8E8)).padding(horizontal = 7.dp, vertical = 2.dp))
                        else if (isClosing) Text("⚡ Closing", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE67E22), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF0EA)).padding(horizontal = 7.dp, vertical = 2.dp))
                        if (job.isPartTime) Text("⏰ Part-time", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1ABC9C), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE8FDF8)).padding(horizontal = 7.dp, vertical = 2.dp))
                    }
                    Text(job.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
                    Text(job.department?:"", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }

                // Bookmark
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (isSaved) Color(0xFFFFF8E1) else BpscColors.Surface)
                        .clickable(onClick = onSave),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        null, tint = if (isSaved) BpscColors.CoinGold else BpscColors.TextHint, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = BpscColors.Divider)

            // Quick info row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                JobInfoChip(Icons.Rounded.Groups, "${job.totalPosts} Posts")
                JobInfoChip(Icons.Rounded.LocationOn, job.location.split(",").first().trim())
                JobInfoChip(Icons.Rounded.CurrencyRupee, job.salaryRange.split("–").first().trim())
            }

            // Part-time extras
            if (job.isPartTime && job.workingHours != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE8FDF8)).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⏰", fontSize = 12.sp)
                    Text(job.workingHours, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1ABC9C), fontWeight = FontWeight.SemiBold)
                    if (job.nearbyDistricts?.isNotEmpty() == true) {
                        Text("·", color = BpscColors.TextHint)
                        Text("📍 ${job.nearbyDistricts.take(2).joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                    }
                }
            }

            // Deadline countdown
            DeadlineCountdown(daysLeft = daysLeft)
        }
    }
}

@Composable
private fun JobInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BpscColors.Surface).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = BpscColors.TextSecondary, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DeadlineCountdown(daysLeft: Long) {
    val (bg, fg, text) = when {
        daysLeft <= 0  -> Triple(Color(0xFFFEE8E8), Color(0xFFE74C3C), "Deadline passed")
        daysLeft <= 3  -> Triple(Color(0xFFFEE8E8), Color(0xFFE74C3C), "🔴 ${daysLeft}d left — Apply now!")
        daysLeft <= 7  -> Triple(Color(0xFFFFF0EA), Color(0xFFE67E22), "⚡ ${daysLeft} days left")
        daysLeft <= 30 -> Triple(Color(0xFFFFF8E1), BpscColors.CoinGold, "📅 ${daysLeft} days remaining")
        else           -> Triple(BpscColors.PrimaryLight, BpscColors.Primary, "📅 ${daysLeft} days remaining")
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = fg, fontWeight = FontWeight.SemiBold)
        Text("Apply →", style = MaterialTheme.typography.bodyMedium, color = fg, fontWeight = FontWeight.ExtraBold)
    }
}

// ─────────────────────────────────────────────────────────────
// JOB DETAIL BOTTOM SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobDetailSheet(
    job: JobVacancy,
    isSaved: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val daysLeft   = daysUntil(job.applyEndDate)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {

            // Blue header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0)),
                        Offset(0f, 0f), Offset(400f, 200f)))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(job.category.label, style = MaterialTheme.typography.labelSmall, color = job.category.color, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(job.category.bg).padding(horizontal = 8.dp, vertical = 3.dp))
                        if (job.isNew) Text("🆕 New", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE8FDF4)).padding(horizontal = 7.dp, vertical = 2.dp))
                        if (job.isPartTime) Text("⏰ Part-time", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1ABC9C), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE8FDF8)).padding(horizontal = 7.dp, vertical = 2.dp))
                    }
                    Text(job.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                    Text(job.department?:"", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
                    DeadlineCountdown(daysLeft = daysLeft)
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick stats grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("👥", "Posts",    "${job.totalPosts}"),
                        Triple("📍", "Location", job.location.split(",").first()),
                        Triple("💰", "Salary",   job.salaryRange.split("–").first().trim()),
                        Triple("🎂", "Age",      job.ageLimit.split("–").first().trim() + "+"),
                    ).forEach { (icon, label, value) ->
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(BpscColors.Surface).padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(icon, fontSize = 16.sp)
                            Text(value, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
                        }
                    }
                }

                // Description
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("About this Job", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text(job.description, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, lineHeight = 24.sp)
                }

                // Eligibility
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BpscColors.PrimaryLight), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Eligibility", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                        DetailRow("🎓", "Qualification", job.qualification)
                        DetailRow("🎂", "Age Limit", job.ageLimit)
                        DetailRow("📍", "Location", job.location)
                        DetailRow("💰", "Salary", job.salaryRange)
                        if (job.isPartTime && job.workingHours != null) DetailRow("⏰", "Working Hours", job.workingHours)
                        if (job.isPartTime && job.nearbyDistricts?.isNotEmpty() == true) DetailRow("📌", "Available Districts", job.nearbyDistricts.joinToString(", "))
                    }
                }

                // Timeline
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Important Dates", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    TimelineView(job.timeline)
                }
            }

            // Bottom actions
            HorizontalDivider(color = BpscColors.Divider)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick  = onSave,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, if (isSaved) BpscColors.CoinGold else BpscColors.Divider),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = if (isSaved) BpscColors.CoinGold else BpscColors.TextSecondary)
                ) {
                    Icon(if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSaved) "Saved" else "Save", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick  = { /* Open official link */ },
                    modifier = Modifier.weight(2f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apply / Official Site", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: String, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun TimelineView(timeline: JobTimeline) {
    val steps = buildList {
        add(Triple("📢", "Notification", timeline.notificationDate))
        add(Triple("▶️", "Apply Start", timeline.applyStart))
        add(Triple("🔴", "Apply End", timeline.applyEnd))
        if (timeline.examDate != null) add(Triple("📝", "Exam Date", timeline.examDate))
    }
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        steps.forEachIndexed { index, (icon, label, date) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Timeline line
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                        Text(icon, fontSize = 14.sp)
                    }
                    if (index < steps.size - 1) {
                        Box(modifier = Modifier.width(2.dp).height(28.dp).background(BpscColors.PrimaryLight))
                    }
                }
                Column(modifier = Modifier.padding(top = 6.dp, bottom = if (index < steps.size - 1) 14.dp else 0.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(date?:"--", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
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
    alertCategories: List<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    allJobs: List<JobVacancy>,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Job Alerts", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                    Text("Get notified for new vacancies", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
                Icon(Icons.Rounded.Notifications, null, tint = BpscColors.CoinGold, modifier = Modifier.size(24.dp))
            }

            HorizontalDivider(color = BpscColors.Divider)

            Text("Select categories to get alerts:", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)

            jobAlertCategories.forEach { cat ->
                val isOn = alertCategories.contains(cat)
                val jobCat = JobCategory.values().firstOrNull { it.label == cat }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (isOn) jobCat?.bg ?: BpscColors.PrimaryLight else BpscColors.Surface)
                        .clickable { onToggle(cat) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(jobCat?.emoji ?: "📋", fontSize = 20.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("${allJobs.count { it.category.label == cat }} active jobs", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                    }
                    Switch(
                        checked = isOn, onCheckedChange = { onToggle(cat) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = jobCat?.color ?: BpscColors.Primary)
                    )
                }
            }

            if (alertCategories.isNotEmpty()) {
                Text(
                    "✅ Alerts active for: ${alertCategories.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.Success,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
private fun daysUntil(epochMillis: Long): Long {
    val diff = epochMillis - System.currentTimeMillis()
    return TimeUnit.MILLISECONDS.toDays(diff)
}

@Composable
private fun JobStatChip(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(64.dp)) {
        Text(icon, fontSize = 13.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
    }
}