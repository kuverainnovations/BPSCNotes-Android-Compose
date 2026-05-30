package com.example.bpscnotes.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bpscnotes.core.ui.t.BpscColors

// ─────────────────────────────────────────────────────────────
// AppLoader — uniform circular loader with optional message
// ─────────────────────────────────────────────────────────────
@Composable
fun AppLoader(
    message: String = "",
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                color       = BpscColors.Primary,
                strokeWidth = 3.dp,
                modifier    = Modifier.size(40.dp)
            )
            if (message.isNotBlank()) {
                Text(
                    message,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = BpscColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// AppFullScreenLoader — overlay card style loader
// ─────────────────────────────────────────────────────────────
@Composable
fun AppFullScreenLoader(message: String = "Loading…") {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp, 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color       = BpscColors.Primary,
                    strokeWidth = 3.dp,
                    modifier    = Modifier.size(36.dp)
                )
                Text(message, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// AppEmptyState — uniform empty state with emoji, title, body, action
// ─────────────────────────────────────────────────────────────
@Composable
fun AppEmptyState(
    emoji:       String  = "📭",
    title:       String,
    body:        String  = "",
    actionLabel: String  = "",
    onAction:    (() -> Unit)? = null,
    modifier:    Modifier = Modifier.fillMaxSize()
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text(emoji, fontSize = 52.sp)
            Text(
                title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = BpscColors.TextPrimary,
                textAlign  = TextAlign.Center
            )
            if (body.isNotBlank()) {
                Text(
                    body,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = BpscColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            if (actionLabel.isNotBlank() && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// AppQuitDialog — uniform quit confirmation for Quiz / MockTest / MCQ
// ─────────────────────────────────────────────────────────────
@Composable
fun AppQuitDialog(
    title:     String = "Quit?",
    body:      String = "Your progress will be lost.",
    quitLabel: String = "Yes, Quit",
    keepLabel: String = "Keep Going",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(20.dp),
        containerColor   = Color.White,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⚠️", fontSize = 26.sp)
                Text(title, fontWeight = FontWeight.ExtraBold, color = BpscColors.TextPrimary)
            }
        },
        text = {
            Text(body, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                shape   = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Rounded.ExitToApp, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(quitLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text(keepLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────
// AppErrorState — uniform error with retry button
// ─────────────────────────────────────────────────────────────
@Composable
fun AppErrorState(
    message:  String,
    onRetry:  (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("⚠️", fontSize = 44.sp)
            Text(
                "Something went wrong",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = BpscColors.TextPrimary,
                textAlign  = TextAlign.Center
            )
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Try Again", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
