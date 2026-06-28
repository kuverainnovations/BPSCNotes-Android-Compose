package com.example.bpscnotes.presentation.jobvacancies

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.AppErrorState
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.JobVacancyDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import java.text.SimpleDateFormat
import java.util.*

private fun openUrl(context: android.content.Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun formatDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return try {
        val input  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val output = SimpleDateFormat("d MMM yyyy", Locale.US)
        output.format(input.parse(raw) ?: return raw)
    } catch (_: Exception) { raw }
}

@Composable
fun JobDetailScreen(
    navController: NavHostController,
    vm: JobDetailViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            JobDetailTopBar(
                job       = state.job,
                onBack    = { navController.popBackStackSafe() },
                onSave    = { vm.toggleSave() },
            )
        },
        bottomBar = {
            if (state.job != null) {
                JobDetailBottomBar(job = state.job!!, context = context)
            }
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { AppLoader() }
            state.error != null -> AppErrorState(message = state.error!!, onRetry = { vm.load() })
            state.job != null   -> JobDetailContent(job = state.job!!, modifier = Modifier.padding(padding), context = context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobDetailTopBar(
    job:   JobVacancyDto?,
    onBack:  () -> Unit,
    onSave:  () -> Unit,
) {
    val cat = job?.category?.toJobCategory() ?: JobCategory.Other
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(cat.color, cat.color.copy(alpha = 0.75f)),
                    start = Offset.Zero,
                    end   = Offset(Float.MAX_VALUE, 0f),
                )
            )
    ) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                // Save toggle
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onSave) {
                        Icon(
                            if (job?.isSaved == true) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = job?.title ?: "Job Detail",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = job?.department ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(0.85f)),
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun JobDetailContent(job: JobVacancyDto, modifier: Modifier, context: android.content.Context) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Key badges row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (job.isNew)      JobBadge("NEW",      Color(0xFF2ECC71))
            if (job.isFeatured) JobBadge("FEATURED", Color(0xFFE67E22))
            if (job.isUrgent)   JobBadge("URGENT",   Color(0xFFE74C3C))
        }

        // Overview card
        InfoCard(title = "Overview") {
            InfoRow(Icons.Rounded.Business, "Organization", job.department ?: "—")
            InfoRow(Icons.Rounded.Category, "Category",     job.category  ?: "—")
            InfoRow(Icons.Rounded.People,   "Total Posts",  job.totalPosts?.toString() ?: "—")
            InfoRow(Icons.Rounded.LocationOn, "Location",
                listOfNotNull(job.jobCity, job.jobDistrict, job.jobState).joinToString(", ").ifBlank { "—" })
            if (job.salaryRange != null) InfoRow(Icons.Rounded.Payments, "Salary", job.salaryRange)
        }

        // Eligibility card
        InfoCard(title = "Eligibility") {
            if (!job.qualification.isNullOrBlank())
                InfoRow(Icons.Rounded.School, "Qualification", job.qualification)
            if (!job.ageLimit.isNullOrBlank())
                InfoRow(Icons.Rounded.Cake, "Age Limit", job.ageLimit)
            if (!job.experienceRequired.isNullOrBlank() && job.experienceRequired != "Any")
                InfoRow(Icons.Rounded.WorkHistory, "Experience", job.experienceRequired)
        }

        // Important dates card
        InfoCard(title = "Important Dates") {
            if (!job.notificationDate.isNullOrBlank())
                InfoRow(Icons.Rounded.NotificationsActive, "Notification Date", formatDate(job.notificationDate))
            if (!job.applyStartDate.isNullOrBlank())
                InfoRow(Icons.Rounded.PlayCircleOutline,   "Apply From",        formatDate(job.applyStartDate))
            InfoRow(Icons.Rounded.EventBusy, "Last Date to Apply", formatDate(job.applyEndDate), highlight = true)
            if (!job.examDate.isNullOrBlank())
                InfoRow(Icons.Rounded.Event, "Exam Date", formatDate(job.examDate))
        }

        // Description
        if (!job.description.isNullOrBlank()) {
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("About This Job", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A1A2E))
                    Spacer(Modifier.height(8.dp))
                    Text(job.description, fontSize = 13.sp, color = Color(0xFF444466), lineHeight = 20.sp)
                }
            }
        }

        // Links card
        val hasLinks = !job.officialLink.isNullOrBlank() || !job.notificationUrl.isNullOrBlank() || !job.pdfUrl.isNullOrBlank()
        if (hasLinks) {
            InfoCard(title = "Links") {
                if (!job.notificationUrl.isNullOrBlank()) {
                    LinkRow(
                        icon  = Icons.Rounded.OpenInBrowser,
                        label = "Official Notification",
                        color = Color(0xFF1565C0),
                        onClick = { openUrl(context, job.notificationUrl) },
                    )
                }
                if (!job.officialLink.isNullOrBlank()) {
                    LinkRow(
                        icon  = Icons.Rounded.Language,
                        label = "Apply Online",
                        color = Color(0xFF2ECC71),
                        onClick = { openUrl(context, job.officialLink) },
                    )
                }
                if (!job.pdfUrl.isNullOrBlank()) {
                    LinkRow(
                        icon  = Icons.Rounded.PictureAsPdf,
                        label = "Download Notification PDF",
                        color = Color(0xFFE74C3C),
                        onClick = { openUrl(context, job.pdfUrl) },
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun JobDetailBottomBar(job: JobVacancyDto, context: android.content.Context) {
    val hasApplyUrl = !job.officialLink.isNullOrBlank()
    val hasNotifUrl = !job.notificationUrl.isNullOrBlank()
    if (!hasApplyUrl && !hasNotifUrl) return

    Surface(
        tonalElevation = 4.dp,
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (hasNotifUrl) {
                OutlinedButton(
                    onClick = { openUrl(context, job.notificationUrl!!) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Notification", fontSize = 13.sp)
                }
            }
            if (hasApplyUrl) {
                Button(
                    onClick = { openUrl(context, job.officialLink!!) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                ) {
                    Icon(Icons.Rounded.ExitToApp, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apply Now", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Reusable card & row components ───────────────────────────

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A1A2E))
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(
    icon:      androidx.compose.ui.graphics.vector.ImageVector,
    label:     String,
    value:     String,
    highlight: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, modifier = Modifier.size(15.dp), tint = Color(0xFF8888AA))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = Color(0xFF8888AA), modifier = Modifier.width(110.dp))
        Text(
            value,
            fontSize   = 13.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color      = if (highlight) Color(0xFFE74C3C) else Color(0xFF1A1A2E),
            modifier   = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LinkRow(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    label:   String,
    color:   Color,
    onClick: () -> Unit,
) {
    TextButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = color.copy(0.6f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun JobBadge(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
