package com.example.bpscnotes.presentation.shared

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bpscnotes.app.R
import com.example.bpscnotes.di.AppConfigEntryPoint
import com.example.bpscnotes.core.language.LocalStrings
import dagger.hilt.android.EntryPointAccessors

/**
 * "Connect with us" — the brand's social channels, rendered as a row of tappable
 * circles. Placed next to Logout in Settings and in the dashboard drawer.
 *
 * Every URL comes from /app-config (app_settings keys social_*, plus the existing
 * support_email), so marketing can add or change a channel without an app release.
 * A channel with a blank value is omitted rather than opening a dead link, which
 * is also why this ships safe before anyone has filled the values in — if nothing
 * is configured, the whole block renders nothing.
 */
@Composable
fun SocialLinksRow(
    modifier: Modifier = Modifier,
    /**
     * Drops the "Connect with us" caption and uses smaller circles. The drawer
     * footer is fixed height and eats directly into the scrollable menu, so it
     * pays for itself there; Settings has room for the full version.
     */
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val str     = LocalStrings.current
    val repo = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppConfigEntryPoint::class.java
        ).appConfigRepository()
    }
    val config by repo.config.collectAsState()

    // Brand colours, so the row reads as the real channels rather than grey chips.
    val channels = listOfNotNull(
        config.socialWhatsapp.trimOrNull()
            ?.let { Channel(R.drawable.ic_social_whatsapp, Color(0xFF25D366), it, "WhatsApp") },
        config.socialTelegram.trimOrNull()
            ?.let { Channel(R.drawable.ic_social_telegram, Color(0xFF229ED9), it, "Telegram") },
        config.socialInstagram.trimOrNull()
            ?.let { Channel(R.drawable.ic_social_instagram, Color(0xFFE1306C), it, "Instagram") },
        config.socialFacebook.trimOrNull()
            ?.let { Channel(R.drawable.ic_social_facebook, Color(0xFF1877F2), it, "Facebook") },
        config.supportEmail.trimOrNull()
            ?.let { Channel(R.drawable.ic_social_email, Color(0xFF5F6368), "mailto:$it", "Email") },
    )

    if (channels.isEmpty()) return

    val circle = if (compact) 34.dp else 42.dp
    val glyph  = if (compact) 17.dp else 20.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 10.dp)
    ) {
        if (!compact) {
            Text(
                str.socialConnect,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)) {
            channels.forEach { ch ->
                Box(
                    modifier = Modifier
                        .size(circle)
                        .clip(CircleShape)
                        .background(ch.color.copy(alpha = 0.12f))
                        .clickable { openExternal(context, ch.url) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(ch.iconRes),
                        contentDescription = ch.label,
                        tint = ch.color,
                        modifier = Modifier.size(glyph)
                    )
                }
            }
        }
    }
}

private data class Channel(
    val iconRes: Int,
    val color: Color,
    val url: String,
    val label: String,
)

private fun String.trimOrNull(): String? = trim().takeIf { it.isNotEmpty() }

/**
 * Hands the link to whatever app claims it — the native Instagram/Telegram client
 * when installed, the browser otherwise, the mail composer for mailto:. Swallows
 * ActivityNotFoundException: a device with nothing registered (no browser, no mail
 * account) must not crash the settings screen.
 */
private fun openExternal(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    } catch (_: ActivityNotFoundException) { /* nothing can handle it — ignore */ }
}
