package com.example.bpscnotes.presentation.auth.mpin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.bpscnotes.core.auth.AppBiometricManager
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.local.TokenStore
import dagger.hilt.android.EntryPointAccessors
import com.example.bpscnotes.di.TokenStoreEntryPoint

@Composable
fun BiometricOptInDialog(
    onEnable: () -> Unit,
    onSkip:   () -> Unit
) {
    val context    = LocalContext.current
    val tokenStore = EntryPointAccessors.fromApplication(
        context.applicationContext,
        TokenStoreEntryPoint::class.java
    ).tokenStore()

    // Only show if biometric hardware is available
    if (!AppBiometricManager.isAvailable(context)) {
        onSkip()
        return
    }

    AlertDialog(
        onDismissRequest = onSkip,
        icon = {
            Icon(Icons.Rounded.Fingerprint, null,
                tint = BpscColors.Primary, modifier = Modifier.size(40.dp))
        },
        title = {
            Text("Enable Fingerprint Login?", fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
        },
        text = {
            Text(
                "Login faster next time using your fingerprint instead of entering your MPIN.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    tokenStore.setBiometricEnabled(true)
                    onEnable()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Text("Enable Fingerprint")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                tokenStore.setBiometricEnabled(false)
                onSkip()
            }) {
                Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
