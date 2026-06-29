package com.example.bpscnotes.presentation.studymaterials

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.BpscDropdown
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.core.ui.t.BpscPalette
import com.example.bpscnotes.data.remote.api.MaterialType
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

// ─────────────────────────────────────────────────────────────────────────────
// Type options shown in the card grid (visual groupings map to 3 backend types)
// ─────────────────────────────────────────────────────────────────────────────

private data class UploadTypeOption(
    val emoji:    String,
    val label:    String,
    val sublabel: String,
    val bgColor:  Color,
    val type:     MaterialType
)

private val UPLOAD_TYPES = listOf(
    UploadTypeOption("📋", "Notes &",      "Summaries",   Color(0xFFEEF3FD), MaterialType.PDF),
    UploadTypeOption("📄", "PDFs &",       "Guides",      Color(0xFFFFEBEE), MaterialType.PDF),
    UploadTypeOption("📝", "PYQs &",       "Papers",      Color(0xFFF3E5F5), MaterialType.PYQ),
    UploadTypeOption("📚", "Books &",      "References",  Color(0xFFE8F5E9), MaterialType.BOOK),
    UploadTypeOption("✏️", "Handwritten",  "Notes",       Color(0xFFFFF8E1), MaterialType.PDF),
)

// Step labels and icons for the 4-step progress indicator
private val STEP_LABELS = listOf("Upload", "Details", "Review", "Go Live")
private val STEP_ICONS: List<ImageVector> = listOf(
    Icons.Rounded.AttachFile,
    Icons.Rounded.Edit,
    Icons.Rounded.Visibility,
    Icons.Rounded.CheckCircle
)

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UploadMaterialScreen(
    navController: NavHostController,
    viewModel: StudyMaterialsViewModel = hiltViewModel()
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current

    var currentStep     by remember { mutableIntStateOf(1) }
    var policyAccepted  by remember { mutableStateOf(false) }
    var selectedTypeIdx by remember { mutableIntStateOf(0) }
    var fileUri         by remember { mutableStateOf<Uri?>(null) }
    var fileName        by remember { mutableStateOf("") }
    var fileSizeWarning by remember { mutableStateOf<String?>(null) }

    // Navigate back and signal StudyMaterialsScreen to switch to My Uploads tab
    LaunchedEffect(state.uploadSuccess) {
        if (state.uploadSuccess != null) {
            navController.previousBackStackEntry
                ?.savedStateHandle?.set("upload_success", true)
            navController.popBackStack()
        }
    }

    // File picker launcher
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val sizeBytes = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (c.moveToFirst() && idx >= 0) c.getLong(idx) else 0L
            } ?: 0L
        } catch (_: Exception) { 0L }

        fileSizeWarning = if (sizeBytes > 20 * 1024 * 1024)
            "File is ${sizeBytes / (1024 * 1024)}MB — large files take longer to upload."
        else null

        fileUri = uri
        var name = "document.pdf"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val ni = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && ni >= 0) name = cursor.getString(ni)
            }
        } catch (_: Exception) {}
        fileName = name
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> filePicker.launch("application/pdf") }

    fun launchFilePicker() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            filePicker.launch("application/pdf")
        } else {
            val perm = android.Manifest.permission.READ_EXTERNAL_STORAGE
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, perm) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) filePicker.launch("application/pdf")
            else permLauncher.launch(arrayOf(perm))
        }
    }

    // Cancel upload confirmation dialog (reuses existing VM logic)
    if (state.showUploadCancelDialog) {
        AlertDialog(
            onDismissRequest  = viewModel::dismissCancelDialog,
            title  = { Text("Upload in progress", fontWeight = FontWeight.Bold) },
            text   = { Text("Your file is still uploading. Do you want to cancel the upload?") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmCancelUpload) {
                    Text("Cancel Upload", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCancelDialog) {
                    Text("Continue Uploading", color = BpscColors.Primary)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            UploadTopBar(
                onBack = {
                    when {
                        state.isUploading -> viewModel.hideUpload()
                        currentStep > 1   -> currentStep--
                        else              -> navController.popBackStackSafe()
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                UploadStepIndicator(currentStep = currentStep)

                if (currentStep == 1) {
                    UploadStep1Content(
                        selectedTypeIdx = selectedTypeIdx,
                        onTypeIdxSelect = { idx ->
                            selectedTypeIdx = idx
                            viewModel.updateUploadForm(type = UPLOAD_TYPES[idx].type)
                        },
                        policyAccepted  = policyAccepted,
                        onPolicyChange  = { policyAccepted = it }
                    )
                } else {
                    UploadStep2Content(
                        fileUri         = fileUri,
                        fileName        = fileName,
                        fileSizeWarning = fileSizeWarning,
                        onPickFile      = ::launchFilePicker,
                        state           = state,
                        onFormChange    = { t, d, s, a, tg, ty, ip, fp, p, lang ->
                            viewModel.updateUploadForm(t, d, s, a, tg, ty, ip, fp, p, lang)
                        },
                        isUploading     = state.isUploading,
                        uploadProgress  = state.uploadProgress,
                        uploadError     = state.uploadError
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            UploadBottomBar(
                currentStep    = currentStep,
                policyAccepted = policyAccepted,
                fileUri        = fileUri,
                title          = state.uploadTitle,
                subject        = state.uploadSubject,
                isUploading    = state.isUploading,
                onContinue     = { currentStep = 2 },
                onSubmit       = {
                    fileUri?.let { uri ->
                        val tags  = state.uploadTags
                            .split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val fp    = state.uploadFreePages.toIntOrNull() ?: 3
                        val price = state.uploadPrice.toIntOrNull() ?: 0
                        viewModel.uploadMaterial(
                            uri, state.uploadTitle, state.uploadDescription,
                            state.uploadSubject, state.uploadType, state.uploadAuthor,
                            tags, 0, state.uploadIsPremium, fp, price, state.uploadLanguage
                        )
                    }
                }
            )

            UploadFooter()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar — blue gradient, back arrow, title, trust badge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UploadTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                    start  = Offset(0f, 0f),
                    end    = Offset(700f, 300f)
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button + title
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack, null,
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        "Upload Your Material",
                        style      = MaterialTheme.typography.titleMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Share. Help. Earn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            // Trust badge
            Row(
                modifier              = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Rounded.Security, null,
                    tint     = BpscPalette.Gold,
                    modifier = Modifier.size(14.dp)
                )
                Column {
                    Text(
                        "Trusted by",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.80f)
                    )
                    Text(
                        "10K+ Students",
                        style      = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4-step progress indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UploadStepIndicator(currentStep: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            STEP_LABELS.forEachIndexed { index, label ->
                val stepNum  = index + 1
                val isActive = stepNum == currentStep
                val isDone   = stepNum < currentStep

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDone   -> BpscPalette.Green500
                                    isActive -> BpscPalette.Blue500
                                    else     -> Color(0xFFE8EAF0)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Rounded.Check else STEP_ICONS[index],
                            contentDescription = null,
                            tint     = if (isActive || isDone) Color.White else Color(0xFF9E9E9E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        style      = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color      = when {
                            isDone   -> BpscPalette.Green500
                            isActive -> BpscPalette.Blue500
                            else     -> Color(0xFF9E9E9E)
                        },
                        fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal,
                        textAlign  = TextAlign.Center
                    )
                }

                // Connector line between steps — Column wrapper ensures vertical centering with the 34dp circle
                if (index < STEP_LABELS.size - 1) {
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(
                            color     = if (stepNum < currentStep) BpscPalette.Green500 else Color(0xFFD8DDE6),
                            thickness = 1.5.dp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — Landing page: hero, type cards, guidelines, policy
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UploadStep1Content(
    selectedTypeIdx: Int,
    onTypeIdxSelect: (Int) -> Unit,
    policyAccepted:  Boolean,
    onPolicyChange:  (Boolean) -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        HeroBannerCard()
        WhatYouCanUploadSection(selectedTypeIdx, onTypeIdxSelect)
        BeforeUploadSection()
        PolicyAndWarningSection(policyAccepted, onPolicyChange)
    }
}

@Composable
private fun HeroBannerCard() {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFE3EBFC)),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Row(
            modifier              = Modifier.padding(10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFD8E8FB)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎓", fontSize = 18.sp)
                    Text("📚", fontSize = 26.sp)

                }
            }

            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Share Quality Notes. Empower Aspirants.",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = Color(0xFF1A1D2E),
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.sp
                )
                Text(
                    "Upload your original study material and help thousands of BPSC aspirants succeed.",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = Color(0xFF6B7280),
                    lineHeight = 16.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("💰", fontSize = 11.sp)
                        Text(
                            "60% revenue share",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatYouCanUploadSection(selectedIdx: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "What you can upload",
            style      = MaterialTheme.typography.titleSmall,
            color      = Color(0xFF1A1D2E),
            fontWeight = FontWeight.ExtraBold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(UPLOAD_TYPES.size) { idx ->
                UploadTypeCard(UPLOAD_TYPES[idx], idx == selectedIdx) { onSelect(idx) }
            }
        }
    }
}

@Composable
private fun UploadTypeCard(opt: UploadTypeOption, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier            = Modifier
            .width(82.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) BpscPalette.Blue100 else opt.bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) BpscPalette.Blue500 else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(opt.emoji, fontSize = 24.sp)
        Text(
            opt.label,
            style      = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color      = if (isSelected) BpscPalette.Blue700 else Color(0xFF1A1D2E),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            textAlign  = TextAlign.Center,
            lineHeight = 13.sp
        )
        Text(
            opt.sublabel,
            style     = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color     = if (isSelected) BpscPalette.Blue500 else Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun BeforeUploadSection() {
    data class Guideline(val icon: ImageVector, val label: String, val color: Color)

    val guidelines = listOf(
        Guideline(Icons.Rounded.Verified,             "Original\ncontent only",  Color(0xFF2E7D32)),
        Guideline(Icons.Rounded.Security,             "Quality\nreviewed",        Color(0xFF6A1B9A)),
        Guideline(Icons.Rounded.People,               "Help 10K+\naspirants",     Color(0xFF1565C0)),
        Guideline(Icons.Rounded.AccountBalanceWallet, "Earn for\nyour work",      Color(0xFFF57F17)),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Before you upload",
            style      = MaterialTheme.typography.titleSmall,
            color      = Color(0xFF1A1D2E),
            fontWeight = FontWeight.ExtraBold
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            guidelines.forEach { g ->
                Column(
                    modifier            = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(g.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(g.icon, null, tint = g.color, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        g.label,
                        style     = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color     = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicyAndWarningSection(policyAccepted: Boolean, onPolicyChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Checkbox + policy text
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (policyAccepted) Color(0xFFEEF7FF) else Color(0xFFF8F9FB))
                .border(
                    1.dp,
                    if (policyAccepted) BpscPalette.Blue500.copy(alpha = 0.35f)
                    else Color(0xFFE2E5EC),
                    RoundedCornerShape(14.dp)
                )
                .clickable { onPolicyChange(!policyAccepted) }
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Checkbox(
                checked         = policyAccepted,
                onCheckedChange = onPolicyChange,
                colors          = CheckboxDefaults.colors(
                    checkedColor   = BpscPalette.Blue500,
                    uncheckedColor = Color(0xFFB0B7C3)
                ),
                modifier = Modifier.size(20.dp)
            )
            Text(
                buildAnnotatedString {
                    append("I confirm this content is original and I agree to the ")
                    withStyle(SpanStyle(color = BpscPalette.Blue500, fontWeight = FontWeight.SemiBold)) {
                        append("Creator Policy")
                    }
                    append(", ")
                    withStyle(SpanStyle(color = BpscPalette.Blue500, fontWeight = FontWeight.SemiBold)) {
                        append("Community Guidelines")
                    }
                    append(", ")
                    withStyle(SpanStyle(color = BpscPalette.Blue500, fontWeight = FontWeight.SemiBold)) {
                        append("Copyright Policy")
                    }
                    append(" and ")
                    withStyle(SpanStyle(color = BpscPalette.Blue500, fontWeight = FontWeight.SemiBold)) {
                        append("Terms of Use")
                    }
                    append(".")
                },
                style    = MaterialTheme.typography.bodySmall,
                color    = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        // Warning box
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF8E6))
                .border(1.dp, Color(0xFFFFD080), RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Icon(
                Icons.Rounded.Warning, null,
                tint     = Color(0xFFF57C00),
                modifier = Modifier.size(15.dp).padding(top = 1.dp)
            )
            Text(
                "Do not upload copyrighted books, paid course PDFs, or content you don't own. " +
                        "Violations may result in content removal and account suspension.",
                style      = MaterialTheme.typography.bodySmall,
                color      = Color(0xFF7B4F00),
                lineHeight = 16.sp
            )
        }

        Spacer(Modifier.height(4.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — Material details form
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UploadStep2Content(
    fileUri:         Uri?,
    fileName:        String,
    fileSizeWarning: String?,
    onPickFile:      () -> Unit,
    state:           StudyMaterialsUiState,
    onFormChange:    (String?, String?, String?, String?, String?, MaterialType?, Boolean?, String?, String?, String?) -> Unit,
    isUploading:     Boolean,
    uploadProgress:  Float,
    uploadError:     String?
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // File picker area
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (fileUri != null) Color(0xFFEEF7FF) else Color(0xFFF5F7FA))
                .border(
                    width = 2.dp,
                    color = if (fileUri != null) BpscPalette.Blue500 else Color(0xFFD0D4DC),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(enabled = !isUploading, onClick = onPickFile)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (fileUri != null) {
                    Icon(
                        Icons.Rounded.CheckCircle, null,
                        tint     = BpscPalette.Blue500,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        fileName,
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = Color(0xFF1A1D2E),
                        fontWeight = FontWeight.SemiBold,
                        textAlign  = TextAlign.Center,
                        maxLines   = 2
                    )
                    Text(
                        "Tap to change file",
                        style = MaterialTheme.typography.bodySmall,
                        color = BpscColors.TextSecondary
                    )
                } else {
                    Box(
                        modifier         = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(BpscPalette.Blue100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.AttachFile, null,
                            tint     = BpscPalette.Blue500,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        "Tap to select your PDF",
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = Color(0xFF1A1D2E),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "PDF files up to 50MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = BpscColors.TextSecondary
                    )
                }
            }
        }

        fileSizeWarning?.let { warning ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFF3CD))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Warning, null, tint = Color(0xFFF57C00), modifier = Modifier.size(13.dp))
                Text(warning, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7B4F00))
            }
        }

        if (isUploading) {
            UploadProgressCard(uploadProgress)
        }

        uploadError?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFFEE8E8))
            ) {
                Row(
                    modifier              = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Warning, null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                    Text(err, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE53935))
                }
            }
        }

        // Form fields
        OutlinedTextField(
            value         = state.uploadTitle,
            onValueChange = { onFormChange(it, null, null, null, null, null, null, null, null, null) },
            label         = { Text("Title *") },
            placeholder   = { Text("Give your material a clear title") },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            singleLine    = true,
            enabled       = !isUploading
        )

        val subjectOptions = state.subjects.filter { it != "All" }.ifEmpty {
            listOf(
                "Polity", "History", "Geography", "Economy",
                "Bihar GK", "Science", "Environment", "Current Affairs", "BPSC Specific"
            )
        }

        BpscDropdown(
            value       = state.uploadSubject,
            label       = "Subject *",
            options     = subjectOptions,
            onSelect    = { onFormChange(null, null, it, null, null, null, null, null, null, null) },
            placeholder = "Select a subject"
        )

        BpscDropdown(
            value       = state.uploadLanguage,
            label       = "Language",
            options     = listOf("English", "Hindi", "Hindi + English"),
            onSelect    = { onFormChange(null, null, null, null, null, null, null, null, null, it) },
            placeholder = "Select language"
        )

        OutlinedTextField(
            value         = state.uploadAuthor,
            onValueChange = { onFormChange(null, null, null, it, null, null, null, null, null, null) },
            label         = { Text("Author / Creator name") },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            singleLine    = true,
            enabled       = !isUploading
        )

        OutlinedTextField(
            value          = state.uploadTags,
            onValueChange  = { onFormChange(null, null, null, null, it, null, null, null, null, null) },
            label          = { Text("Tags") },
            placeholder    = { Text("e.g. BPSC, Polity, Constitution") },
            modifier       = Modifier.fillMaxWidth(),
            shape          = RoundedCornerShape(12.dp),
            singleLine     = true,
            supportingText = { Text("Comma separated — helps students find your material") },
            enabled        = !isUploading
        )

        OutlinedTextField(
            value         = state.uploadDescription,
            onValueChange = { onFormChange(null, it, null, null, null, null, null, null, null, null) },
            label         = { Text("Message to Reviewer") },
            placeholder   = { Text("Tell the reviewer about the content, source, year, exam relevance…") },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            minLines      = 3,
            maxLines      = 4,
            enabled       = !isUploading
        )

        PremiumCard(state, onFormChange, isUploading)

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun UploadProgressCard(progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFEEF7FF)),
        border   = BorderStroke(1.dp, BpscPalette.Blue500.copy(alpha = 0.25f))
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = BpscPalette.Blue500
                    )
                    Text(
                        "Uploading…",
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = BpscPalette.Blue500,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = BpscPalette.Blue500,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color      = BpscPalette.Blue500,
                trackColor = BpscPalette.Blue100
            )
        }
    }
}

@Composable
private fun PremiumCard(
    state:        StudyMaterialsUiState,
    onFormChange: (String?, String?, String?, String?, String?, MaterialType?, Boolean?, String?, String?, String?) -> Unit,
    isUploading:  Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (state.uploadIsPremium) Color(0xFFFFF8E1) else Color(0xFFF8F9FB)
        ),
        border = BorderStroke(
            1.dp,
            if (state.uploadIsPremium) Color(0xFFFFD080) else Color(0xFFE2E5EC)
        )
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("💰", fontSize = 16.sp)
                        Text(
                            "Premium Content",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        "Charge students to access the full material",
                        style = MaterialTheme.typography.bodySmall,
                        color = BpscColors.TextSecondary
                    )
                }
                Switch(
                    checked         = state.uploadIsPremium,
                    onCheckedChange = { onFormChange(null, null, null, null, null, null, it, null, null, null) },
                    enabled         = !isUploading,
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor = BpscPalette.Gold,
                        checkedTrackColor = BpscPalette.Gold.copy(alpha = 0.3f)
                    )
                )
            }

            AnimatedVisibility(
                visible = state.uploadIsPremium,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value           = state.uploadPrice,
                            onValueChange   = { onFormChange(null, null, null, null, null, null, null, null, it.filter(Char::isDigit), null) },
                            label           = { Text("₹ Price") },
                            modifier        = Modifier.weight(1f),
                            shape           = RoundedCornerShape(12.dp),
                            singleLine      = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled         = !isUploading
                        )
                        OutlinedTextField(
                            value           = state.uploadFreePages,
                            onValueChange   = { onFormChange(null, null, null, null, null, null, null, it.filter(Char::isDigit), null, null) },
                            label           = { Text("Free pages") },
                            modifier        = Modifier.weight(1f),
                            shape           = RoundedCornerShape(12.dp),
                            singleLine      = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled         = !isUploading
                        )
                    }
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF3CD))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 12.sp)
                        Text(
                            "Students see ${state.uploadFreePages.ifEmpty { "3" }} free pages. Full access unlocks after purchase.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7B4F00)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom action bar — "Continue" on step 1, "Submit for Review" on step 2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UploadBottomBar(
    currentStep:    Int,
    policyAccepted: Boolean,
    fileUri:        Uri?,
    title:          String,
    subject:        String,
    isUploading:    Boolean,
    onContinue:     () -> Unit,
    onSubmit:       () -> Unit
) {
    val canContinue = policyAccepted
    val canSubmit   = fileUri != null && title.isNotBlank() && subject.isNotBlank() && !isUploading

    Surface(
        tonalElevation  = 8.dp,
        shadowElevation = 8.dp,
        color           = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Button(
                onClick  = if (currentStep == 1) onContinue else onSubmit,
                enabled  = if (currentStep == 1) canContinue else canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = BpscPalette.Blue500,
                    disabledContainerColor = Color(0xFFD0D4DC),
                    contentColor           = Color.White,
                    disabledContentColor   = Color.White.copy(alpha = 0.6f)
                )
            ) {
                when {
                    isUploading      -> {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Uploading…", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    currentStep == 1 -> {
                        Text("Continue to Details", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                    else             -> {
                        Text("Submit for Review", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Send, null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            when {
                currentStep == 1 && !policyAccepted -> Text(
                    "Accept the policy above to continue",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = BpscColors.TextSecondary,
                    modifier  = Modifier.fillMaxWidth().padding(top = 6.dp),
                    textAlign = TextAlign.Center
                )
                currentStep == 2 && fileUri == null -> Text(
                    "Select a PDF file to enable submission",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = BpscColors.TextSecondary,
                    modifier  = Modifier.fillMaxWidth().padding(top = 6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Footer branding strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UploadFooter() {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F4F8))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Security, null, tint = BpscColors.TextSecondary, modifier = Modifier.size(11.dp))
            Text(
                "Secure · Fair · Student First",
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.TextSecondary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BPSC", style = MaterialTheme.typography.labelSmall, color = BpscPalette.Blue500, fontWeight = FontWeight.ExtraBold)
            Text("Notes", style = MaterialTheme.typography.labelSmall, color = BpscPalette.Orange500, fontWeight = FontWeight.ExtraBold)
            Text(" · Only What Matters", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
        }
    }
}
