package com.example.bpscnotes.core.language

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bpscnotes.core.ui.t.BpscColors

/**
 * Language picker row — used in Settings.
 * Uses LanguageManager.language (static companion StateFlow) so changes
 * are immediately reflected app-wide without any restart.
 */
@Composable
fun LanguagePickerRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Collect from the STATIC companion StateFlow — same source as MainActivity
    val current by LanguageManager.language.collectAsState()
    val str = LocalStrings.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF3E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Language, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(str.settingsLanguage, style = MaterialTheme.typography.bodyLarge,
                color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(current.displayName + " / " + current.nativeName,
                style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
        }

        // Toggle chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = current == lang
                val bg by animateColorAsState(
                    if (isSelected) BpscColors.Primary else Color(0xFFF5F5F5), tween(180), label = "bg"
                )
                val textColor by animateColorAsState(
                    if (isSelected) Color.White else BpscColors.TextSecondary, tween(180), label = "tc"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg)
                        .clickable {
                            // Use static setter — updates the shared StateFlow instantly
                            LanguageManager.setLanguageGlobal(lang, context)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(lang.flag + " " + lang.nativeName, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = textColor)
                }
            }
        }
    }
}

/**
 * Compact toggle in side drawer header.
 * One tap switches between EN ↔ HI instantly.
 */
@Composable
fun LanguageSwitchButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val current by LanguageManager.language.collectAsState()

    // Segmented toggle: [ 🇬🇧 EN | 🇮🇳 HI ]
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(0.12f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLanguage.entries.forEach { lang ->
            val isSelected = current == lang
            val bg by animateColorAsState(
                if (isSelected) Color.White else Color.Transparent, tween(200), label = "sw"
            )
            val textColor by animateColorAsState(
                if (isSelected) BpscColors.Primary else Color.White.copy(0.75f), tween(200), label = "tc"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(bg)
                    .clickable { LanguageManager.setLanguageGlobal(lang, context) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    lang.flag + " " + lang.code.uppercase(),
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = textColor
                )
            }
        }
    }
}