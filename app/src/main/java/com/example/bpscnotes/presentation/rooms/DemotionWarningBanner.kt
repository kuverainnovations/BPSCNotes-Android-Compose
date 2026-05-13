package com.example.bpscnotes.presentation.rooms

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.bpscnotes.core.ui.t.BpscColors

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/DemotionWarningBanner.kt
//
// Shows a dismissible "At Risk" banner when the user's
// tier progress has dropped below the demotion threshold.
//
// Used in:
//   1. RoomsHubScreen — shown at top when getAtRiskStatus returns true
//   2. DashboardScreen — shown as a persistent card (future)
// ════════════════════════════════════════════════════════════

data class AtRiskState(
    val isAtRisk:    Boolean = false,
    val progress:    Float   = 0f,    // 0–100
    val threshold:   Float   = 50f,   // 0–100
    val tierKey:     String  = "",
    val tierName:    String  = "",
    val tierEmoji:   String  = "",
)

/**
 * Compact dismissible banner for "At Risk" demotion warning.
 * Shown only when isAtRisk = true.
 * Dismiss lasts only for the session (no persistence needed —
 * backend will stop sending the push after grace period starts).
 */
@Composable
fun DemotionWarningBanner(
    state:       AtRiskState,
    onDismiss:   () -> Unit,
    onStudyNow:  () -> Unit,
) {
    if (!state.isAtRisk) return

    val deficit = (state.threshold - state.progress).coerceAtLeast(0f)
    val animProg by animateFloatAsState(state.progress / 100f, tween(800), label = "demotion_prog")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        border   = BorderStroke(1.5.dp, Color(0xFFFF8F00))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("⚠️", fontSize = 20.sp)
                    Column {
                        Text(
                            "${state.tierEmoji} ${state.tierName} at risk",
                            style      = MaterialTheme.typography.titleMedium,
                            color      = Color(0xFFE65100),
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Your activity dropped to ${state.progress.toInt()}% — threshold is ${state.threshold.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBF360C)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = Color(0xFFBF360C), modifier = Modifier.size(16.dp))
                }
            }

            // Progress bar showing how close to demotion
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFFE0B2))) {
                    Box(
                        modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight().background(
                            if (state.progress < 30f) Color(0xFFE53935)
                            else if (state.progress < 50f) Color(0xFFFF8F00)
                            else Color(0xFF43A047),
                            RoundedCornerShape(4.dp)
                        )
                    )
                    // Threshold marker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.threshold / 100f)
                            .fillMaxHeight(3f)
                            .align(Alignment.CenterStart)
                            .padding(end = 0.dp)
                    ) {
                        Box(modifier = Modifier.size(2.dp, 8.dp).align(Alignment.CenterEnd).background(Color(0xFFE53935)))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${state.progress.toInt()}% activity", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBF360C), fontSize = 10.sp)
                    Text("Need ${state.threshold.toInt()}% to keep ${state.tierName}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBF360C), fontSize = 10.sp)
                }
            }

            // CTA
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = onStudyNow,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("Study Now 🔥", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.height(36.dp),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, Color(0xFFFF8F00)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100))
                ) {
                    Text("Dismiss", style = MaterialTheme.typography.labelSmall, fontSize = 12.sp)
                }
            }
        }
    }
}
