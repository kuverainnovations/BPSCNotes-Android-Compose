package com.example.bpscnotes.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors

// ════════════════════════════════════════════════════════════
// EditProfileScreen — fully dynamic
// Loads current data from ProfileViewModel, calls PATCH /users/profile
// ════════════════════════════════════════════════════════════

@Composable
fun EditProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val user  = state.user

    // Pre-populate fields from live user data
    var name        by remember(user?.name)        { mutableStateOf(user?.name ?: "") }
    var email       by remember(user?.email)       { mutableStateOf(user?.email ?: "") }
    var bio         by remember(user?.bio)         { mutableStateOf(user?.bio ?: "") }
    var district    by remember(user?.district)    { mutableStateOf(user?.district ?: "") }
    var targetYear  by remember(user?.targetYear)  { mutableStateOf(user?.targetYear?.toString() ?: "") }
    var prepLevel   by remember(user?.prepLevel)   { mutableStateOf(user?.prepLevel ?: "") }

    val snackbarHost = remember { SnackbarHostState() }
    val prepLevels   = listOf(str.prepBeginner, str.prepIntermediate, str.prepAdvanced)
    val targetYears  = (2025..2030).map { it.toString() }

    // Navigate back on success
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
            navController.popBackStack()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost    = { SnackbarHost(snackbarHost) },
        containerColor  = BpscColors.Surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(BpscColors.Surface)) {

            // ── Header ─────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(
                    listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                    Offset(0f, 0f), Offset(500f, 300f)))
               // .statusBarsPadding()
                    ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .padding(top = 46.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color.White.copy(0.12f)).clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text(str.profileEdit, style = MaterialTheme.typography.titleLarge,
                        color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color.White.copy(0.12f))
                        .clickable(enabled = !state.isSaving) {
                            viewModel.updateProfile(
                                name       = name,
                                email      = email.ifEmpty { null },
                                bio        = bio.ifEmpty { null },
                                district   = district.ifEmpty { null },
                                targetYear = targetYear.toIntOrNull(),
                                prepLevel  = prepLevel.ifEmpty { null }
                            )
                        }, contentAlignment = Alignment.Center) {
                        if (state.isSaving)
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Avatar preview
                Box(modifier = Modifier.align(Alignment.CenterHorizontally)
                    .size(80.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2)))),
                    contentAlignment = Alignment.Center) {
                    Text(
                        name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "?" },
                        style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold
                    )
                }

                // ── Personal Info ──────────────────────────────
                SectionHeader(str.editPersonalInfo)
                ProfileField(value = name, label = str.editFullName, icon = Icons.Rounded.Person,
                    onValueChange = { name = it })
                ProfileField(value = email, label = str.editEmail, icon = Icons.Rounded.Email,
                    keyboardType = KeyboardType.Email, onValueChange = { email = it })
                ProfileField(value = bio, label = "Bio / About me", icon = Icons.Rounded.Info,
                    singleLine = false, onValueChange = { bio = it }, minLines = 3)
                ProfileField(value = district, label = str.editDistrict, icon = Icons.Rounded.LocationOn,
                    onValueChange = { district = it })

                // ── Exam Settings ──────────────────────────────
                SectionHeader(str.editExamSettings)

                // Prep level picker
              /*  Text(str.editPrepLevel, style = MaterialTheme.typography.labelMedium,
                    color = BpscColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    prepLevels.forEach { level ->
                        val selected = prepLevel == level
                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .background(if (selected) BpscColors.Primary else Color.White)
                            .border(1.dp, if (selected) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(12.dp))
                            .clickable { prepLevel = level }
                            .padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text(level, style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) Color.White else BpscColors.TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }*/

                // Target year picker
                Text(str.editTargetYear, style = MaterialTheme.typography.labelMedium,
                    color = BpscColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    targetYears.forEach { year ->
                        val selected = targetYear == year
                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .background(if (selected) BpscColors.CoinGold else Color.White)
                            .border(1.dp, if (selected) BpscColors.CoinGold else BpscColors.Divider, RoundedCornerShape(12.dp))
                            .clickable { targetYear = year }
                            .padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text(year, style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) Color.White else BpscColors.TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // ── Save button ────────────────────────────────
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            name = name, email = email.ifEmpty { null }, bio = bio.ifEmpty { null },
                            district = district.ifEmpty { null }, targetYear = targetYear.toIntOrNull(),
                            prepLevel = prepLevel.ifEmpty { null }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    enabled  = name.isNotBlank() && !state.isSaving,
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(str.editSaving, style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(str.editSaveChanges, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                // Mobile (read-only — cannot be changed)
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PhoneAndroid, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
                        Column {
                            Text(str.editMobile, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                            Text(user?.mobile ?: str.noData,
                                style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(str.editVerified, style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary,
        fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ProfileField(
    value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (String) -> Unit, singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text, minLines: Int = 1
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        leadingIcon   = { Icon(icon, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp)) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(14.dp),
        singleLine    = singleLine,
        minLines      = minLines,
        maxLines      = if (singleLine) 1 else 4,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = BpscColors.Primary,
            unfocusedBorderColor = BpscColors.Divider,
            focusedLabelColor    = BpscColors.Primary,
        )
    )
}
