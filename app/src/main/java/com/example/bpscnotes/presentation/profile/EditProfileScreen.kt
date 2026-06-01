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
import com.example.bpscnotes.core.ui.t.LocalDarkMode
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state   by viewModel.uiState.collectAsState()
    val str     = LocalStrings.current
    val user    = state.user
    val isDark  = LocalDarkMode.current
    val cs      = MaterialTheme.colorScheme

    var name     by remember(user?.name)     { mutableStateOf(user?.name ?: "") }
    var email    by remember(user?.email)    { mutableStateOf(user?.email ?: "") }
    var bio      by remember(user?.bio)      { mutableStateOf(user?.bio ?: "") }
    var district by remember(user?.district) { mutableStateOf(user?.district ?: "") }

    val snackbarHost = remember { SnackbarHostState() }

    // Navigate back on success (optimistic — no delay)
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            viewModel.clearMessages()
            navController.popBackStackSafe()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
        }
    }

    val doSave = {
        viewModel.updateProfile(
            name      = name,
            email     = email.ifEmpty { null },
            bio       = bio.ifEmpty { null },
            district  = district.ifEmpty { null },
            targetYear = null,
            prepLevel = null
        )
    }

    Scaffold(
        snackbarHost       = { SnackbarHost(snackbarHost) },
        containerColor     = cs.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(cs.background)) {

            // ── Gradient header ──────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(
                    listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                    Offset(0f, 0f), Offset(500f, 300f)))) {
                Row(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp).padding(top = 46.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {

                    // Back
                    Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(Color.White.copy(0.12f))
                        .clickable { navController.popBackStackSafe() },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    Text(str.profileEdit, style = MaterialTheme.typography.titleLarge,
                        color = Color.White, fontWeight = FontWeight.ExtraBold)

                    // Save tick button in header
                    Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(
                            if (name.isNotBlank() && !state.isSaving)
                                Color(0xFF4CAF50).copy(0.85f)
                            else Color.White.copy(0.12f)
                        )
                        .clickable(enabled = name.isNotBlank() && !state.isSaving) { doSave() },
                        contentAlignment = Alignment.Center) {
                        if (state.isSaving)
                            CircularProgressIndicator(color = Color.White,
                                modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── Avatar ────────────────────────────────────
                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Box(modifier = Modifier.size(90.dp).clip(CircleShape)
                        .background(Brush.linearGradient(
                            listOf(Color(0xFF1565C0), Color(0xFF0D47A1)))),
                        contentAlignment = Alignment.Center) {
                        Text(
                            name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                                .take(2).joinToString("").ifEmpty { "?" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White, fontWeight = FontWeight.ExtraBold
                        )
                    }
                    // Camera icon badge
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(Color(0xFF1565C0))
                        .border(2.dp, cs.surface, CircleShape)
                        .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                // ── Personal Info ──────────────────────────────
                EditSection(title = str.editPersonalInfo, isDark = isDark) {
                    EditField(value = name, label = str.editFullName,
                        icon = Icons.Rounded.Person, onValueChange = { name = it })
                    Spacer(Modifier.height(10.dp))
                    EditField(value = email, label = str.editEmail,
                        icon = Icons.Rounded.Email, keyboardType = KeyboardType.Email,
                        onValueChange = { email = it })
                    Spacer(Modifier.height(10.dp))
                    EditField(value = district, label = str.editDistrict,
                        icon = Icons.Rounded.LocationOn, onValueChange = { district = it })
                    Spacer(Modifier.height(10.dp))
                    EditField(value = bio, label = str.editBio,
                        icon = Icons.Rounded.Info, singleLine = false,
                        minLines = 4, maxLines = 6, onValueChange = { bio = it })
                }

                // ── Mobile (read-only) ─────────────────────────
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    Row(modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(BpscColors.Primary.copy(0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PhoneAndroid, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(str.editMobile, style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant)
                            Text(user?.mobile ?: str.noData, style = MaterialTheme.typography.bodyMedium,
                                color = cs.onBackground, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E7D32).copy(0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                            Text(str.editVerified, style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Save Button ────────────────────────────────
                Button(
                    onClick  = { doSave() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(16.dp),
                    enabled  = name.isNotBlank() && !state.isSaving,
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = Color.White,
                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(str.editSaving, style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(str.editSaveChanges, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun EditSection(title: String, isDark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onBackground,
            fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 10.dp))
        Card(shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun EditField(
    value: String, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        leadingIcon   = { Icon(icon, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp)) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        singleLine    = singleLine,
        minLines      = minLines,
        maxLines      = if (singleLine) 1 else 5,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = BpscColors.Primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor    = BpscColors.Primary,
        )
    )
}