package com.example.bpscnotes.presentation.dashboard

import com.example.bpscnotes.core.language.LocalStrings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bpscnotes.core.ui.t.BpscColors

/**
 * Bottom sheet for creating daily targets.
 *
 * Calls [viewModel.createTargets()] with real API on submit.
 * The ViewModel returns to [DashboardUiState.targetSuccess] which
 * the parent screen observes to show a toast and dismiss.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateTargetSheet(
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val str = LocalStrings.current
    val cs  = MaterialTheme.colorScheme
    val state       by viewModel.uiState.collectAsState()
    val isLoading    = state.isCreatingTarget
    var inputText   by remember { mutableStateOf("") }
    val addedTitles  = remember { mutableStateListOf<String>() }
    val currentCount = state.dailyTargets.size

    val imeVisible = WindowInsets.isImeVisible

    // Dismiss automatically when ViewModel signals success
    LaunchedEffect(state.targetSuccess) {
        if (state.targetSuccess != null) {
            viewModel.clearTargetSuccess()
            onDismiss()
        }
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false, onClick = {}),
            shape    = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors   = CardDefaults.cardColors(containerColor = cs.surface)
        ) {

            val sheetHeight = when {
                addedTitles.isEmpty() -> 0.55f
                addedTitles.size <= 2 -> 0.65f
                addedTitles.size <= 5 -> 0.75f
                else -> 0.85f
            }
            Column(
                modifier = Modifier
                    .then(
                        if (imeVisible) {
                            Modifier.wrapContentHeight()
                        } else {
                            Modifier.fillMaxHeight(sheetHeight)
                        }
                    )
                    .padding(24.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(cs.outline)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    str.targetCreateSheet,
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Add up to 5 study tasks for today (${5 - currentCount} remaining).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                // Input row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.background)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value         = inputText,
                        onValueChange = { inputText = it },
                        modifier      = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        textStyle     = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                        singleLine    = true,
                        decorationBox = { inner ->
                            if (inputText.isEmpty()) {
                                Text(str.targetPlaceholder, color = BpscColors.TextHint)
                            }
                            inner()
                        }
                    )
                    IconButton(
                        onClick  = {
                            val trimmed = inputText.trim()
                            if (trimmed.isNotEmpty() && addedTitles.size + currentCount < 5) {
                                addedTitles.add(trimmed)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(BpscColors.Primary)
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "${currentCount + addedTitles.size} / 5 targets · ${addedTitles.size} to add",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentCount + addedTitles.size >= 5) MaterialTheme.colorScheme.error else BpscColors.TextSecondary
                )

                // Error display
                AnimatedVisibility(visible = state.error != null) {
                    Text(
                        state.error ?: "",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Added items list
                /*if (addedTitles.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    addedTitles.forEachIndexed { i, title ->
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier         = Modifier.size(24.dp).clip(CircleShape).background(BpscColors.PrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${i + 1}",
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = BpscColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                title,
                                style    = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick  = { addedTitles.removeAt(i) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    null,
                                    tint     = BpscColors.TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }*/

                // Scrollable targets area
                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .align(Alignment.TopCenter),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    itemsIndexed(addedTitles) { i, title ->

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(BpscColors.PrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${i + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { addedTitles.removeAt(i) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    null,
                                    tint = BpscColors.TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                    }

                Spacer(Modifier.height(16.dp))

              //  Spacer(Modifier.height(20.dp))

                // Submit button
                Button(
                    onClick  = {
                        if (addedTitles.isNotEmpty() && !isLoading) {
                            viewModel.createTargets(addedTitles.toList())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled  = addedTitles.isNotEmpty() && !isLoading,
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Add ${addedTitles.size} Target${if (addedTitles.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.height(80.dp))

        }
    }


}
