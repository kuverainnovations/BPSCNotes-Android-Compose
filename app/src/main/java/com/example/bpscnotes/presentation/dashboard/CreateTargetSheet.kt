package com.example.bpscnotes.presentation.dashboard

import com.example.bpscnotes.core.language.LocalStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.bpscnotes.core.ui.BpscDropdown
import com.example.bpscnotes.core.ui.t.BpscColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTargetSheet(
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val str          = LocalStrings.current
    val cs           = MaterialTheme.colorScheme
    val state        by viewModel.uiState.collectAsState()
    val isLoading     = state.isCreatingTarget
    var inputText    by remember { mutableStateOf("") }
    val addedTitles   = remember { mutableStateListOf<String>() }
    val currentCount  = state.dailyTargets.size
    val remaining     = 5 - currentCount - addedTitles.size
    val canAdd        = inputText.trim().isNotEmpty() && remaining > 0

    // Subject dropdown state
    val predefinedSubjects = listOf(
        "Polity", "History", "Geography", "Economy", "Bihar GK",
        "Science & Tech", "General Studies", "Environment", "Maths", "English", "Current Affairs"
    )
    var selectedSubject    by remember { mutableStateOf(predefinedSubjects[0]) }
    var subjectExpanded    by remember { mutableStateOf(false) }

    // Estimated time chips

    val focusRequester      = remember { FocusRequester() }
    val keyboardController  = LocalSoftwareKeyboardController.current
    val sheetState          = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Dismiss when ViewModel signals success
    LaunchedEffect(state.targetSuccess) {
        if (state.targetSuccess != null) {
            viewModel.clearTargetSuccess()
            onDismiss()
        }
    }

    // Auto-focus text field when sheet opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun addCurrentInput() {
        val trimmed = inputText.trim()
        if (trimmed.isNotEmpty() && remaining > 0) {
            // Format: "Subject - Sub-topic"
            addedTitles.add("$selectedSubject - $trimmed")
            inputText = ""
            focusRequester.requestFocus()
        }
    }

    ModalBottomSheet(
        onDismissRequest   = onDismiss,
        sheetState         = sheetState,
        containerColor     = cs.surface,
        shape              = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle         = {
            Box(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                Alignment.Center
            ) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(cs.outline))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()           // <-- single imePadding here, sheet slides up correctly
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // ── Title ────────────────────────────────────────────
            Text(
                str.targetCreateSheet,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color      = cs.onSurface
            )
            Text(
                "Add up to 5 tasks · $remaining remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = if (remaining <= 1) MaterialTheme.colorScheme.error
                else cs.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // ── Subject dropdown ──────────────────────────────────
            BpscDropdown(
                value    = selectedSubject,
                label    = str.mockSubjectWise,
                options  = predefinedSubjects,
                onSelect = { selectedSubject = it }
            )

            Spacer(Modifier.height(12.dp))

            // ── Sub-topic input ───────────────────────────────────
            // Using OutlinedTextField so the label floats and user always sees
            // what they're typing, even with keyboard open
            OutlinedTextField(
                value         = inputText,
                onValueChange = { if (it.length <= 120) inputText = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label         = { Text(str.subTopic) },
                placeholder   = { Text(str.ctSubTopicHint, color = BpscColors.TextHint) },
                singleLine    = true,
                shape         = RoundedCornerShape(14.dp),
                trailingIcon  = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addCurrentInput() }),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = BpscColors.Primary,
                    focusedLabelColor    = BpscColors.Primary,
                    cursorColor          = BpscColors.Primary
                )
            )

            // Character count + Add button row
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    "${inputText.length}/120",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (inputText.length > 100) MaterialTheme.colorScheme.error
                    else BpscColors.TextHint
                )
                Button(
                    onClick  = { addCurrentInput() },
                    enabled  = canAdd,
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(str.ctAdd, style = MaterialTheme.typography.labelLarge)
                }
            }

            // ── Added targets list ─────────────────────────────────
            if (addedTitles.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "To add (${addedTitles.size})",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = BpscColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))

                // Fixed-height scrollable list so it never pushes button off-screen
                LazyColumn(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(addedTitles) { i, title ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(cs.background)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                Modifier.size(22.dp).clip(CircleShape).background(BpscColors.PrimaryLight),
                                Alignment.Center
                            ) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                            }
                            Text(title, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f), color = cs.onSurface)
                            IconButton(
                                onClick  = { addedTitles.removeAt(i) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Rounded.Close, null,
                                    tint = BpscColors.TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Error
            if (state.error != null) {
                Spacer(Modifier.height(6.dp))
                Text(state.error!!, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))

            // ── Submit button — always visible above keyboard ──────
            Button(
                onClick  = {
                    val trimmed = inputText.trim()
                    if (trimmed.isNotEmpty() && remaining > 0) addedTitles.add("$selectedSubject - $trimmed")
                    if (addedTitles.isNotEmpty() && !isLoading) {
                        keyboardController?.hide()
                        viewModel.createTargets(addedTitles.toList(), subject = selectedSubject)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = (addedTitles.isNotEmpty() || inputText.trim().isNotEmpty()) && !isLoading,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    val count = addedTitles.size + if (inputText.trim().isNotEmpty()) 1 else 0
                    Text(
                        if (count > 0) "Add $count Target${if (count != 1) "s" else ""}"
                        else "Add Targets",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}