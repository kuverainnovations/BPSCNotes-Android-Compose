package com.example.bpscnotes.presentation.moderation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bpscnotes.data.remote.api.ReportReason

// ════════════════════════════════════════════════════════════
// ReportSheet — the single report/block surface, reused on every
// screen where a user can see another user's content.
//
// Play's UGC policy wants reporting and blocking reachable from the
// content itself, not buried in Settings, so this is designed to hang off
// a per-item overflow icon. It renders as one sheet with two actions
// because they answer different problems: report is "this content is
// wrong", block is "I don't want to see this person". Offering only one
// pushes people into using it for both.
// ════════════════════════════════════════════════════════════

private val REASON_LABELS: List<Pair<String, String>> = listOf(
    ReportReason.SPAM           to "Spam or advertising",
    ReportReason.HARASSMENT     to "Harassment or bullying",
    ReportReason.HATE_SPEECH    to "Hate speech",
    ReportReason.SEXUAL_CONTENT to "Sexual or explicit content",
    ReportReason.VIOLENCE       to "Violence or threats",
    ReportReason.COPYRIGHT      to "Copyright infringement",
    ReportReason.MISINFORMATION to "False or misleading information",
    ReportReason.PERSONAL_INFO  to "Shares personal information",
    ReportReason.OTHER          to "Something else",
)

/**
 * @param contentType      one of [com.example.bpscnotes.data.remote.api.ReportContentType]
 * @param contentId        id of the reported row
 * @param reportedUserId   author, when known — enables the Block action
 * @param reportedUserName shown in the block confirmation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(
    contentType: String,
    contentId: String,
    reportedUserId: String? = null,
    reportedUserName: String? = null,
    onDismiss: () -> Unit,
    vm: ModerationViewModel = hiltViewModel(),
) {
    val state = vm.state.collectAsState().value
    var reason  by remember { mutableStateOf(ReportReason.SPAM) }
    var details by remember { mutableStateOf("") }
    var confirmBlock by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Flag, contentDescription = null, tint = Color(0xFFE74C3C))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Report this content",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                "Tell us what's wrong. Our team reviews every report within 24 hours " +
                    "and removes content that breaks our rules.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            REASON_LABELS.forEach { (key, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .selectable(selected = reason == key, onClick = { reason = key })
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(selected = reason == key, onClick = { reason = key })
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedTextField(
                value = details,
                onValueChange = { if (it.length <= 1000) details = it },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                placeholder = { Text("Add any details (optional)") },
                shape = RoundedCornerShape(12.dp),
            )

            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { vm.report(contentType, contentId, reason, details) { onDismiss() } },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Submit report", fontWeight = FontWeight.Bold)
                }
            }

            // Blocking is a separate decision from reporting, and is offered
            // only when the author is known — a report on content whose author
            // has since been deleted still makes sense, a block does not.
            if (reportedUserId != null) {
                HorizontalDivider()
                TextButton(
                    onClick = { confirmBlock = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = null, tint = Color(0xFFE74C3C))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Block ${reportedUserName ?: "this user"}",
                        color = Color(0xFFE74C3C),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (confirmBlock && reportedUserId != null) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false },
            icon  = { Icon(Icons.Rounded.Block, contentDescription = null) },
            title = { Text("Block ${reportedUserName ?: "this user"}?") },
            text  = {
                Text(
                    "You won't see their messages, reviews or uploads anywhere in the app. " +
                        "They are not told you blocked them. You can undo this in " +
                        "Settings → Blocked users."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmBlock = false
                    vm.block(reportedUserId, reportedUserName) { onDismiss() }
                }) { Text("Block", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmBlock = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * Overflow action that opens [ReportSheet] for one item.
 *
 * Wrapping the state in here keeps call sites to a single line, so adding
 * reporting to a new UGC surface stays cheap enough that it actually gets
 * done — the reason the app shipped without it on three surfaces.
 */
@Composable
fun ReportIconButton(
    contentType: String,
    contentId: String,
    reportedUserId: String? = null,
    reportedUserName: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(32.dp)) {
        Icon(
            Icons.Rounded.Flag,
            contentDescription = "Report or block",
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
    if (open) {
        ReportSheet(
            contentType      = contentType,
            contentId        = contentId,
            reportedUserId   = reportedUserId,
            reportedUserName = reportedUserName,
            onDismiss        = { open = false },
        )
    }
}
