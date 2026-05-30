package com.example.bpscnotes.core.ads

import com.example.bpscnotes.core.language.LocalStrings
import android.app.Activity
import android.view.LayoutInflater
import android.widget.TextView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bpscnotes.core.ui.t.BpscColors
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

// ════════════════════════════════════════════════════════════
// AD COMPONENTS — Compose wrappers for all ad formats
// ════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────
// 1. BANNER AD — shown between content sections
//    Usage: BannerAdView(adUnitId = adManager.getBannerAdUnitId())
// ─────────────────────────────────────────────────────────────
@Composable
fun BannerAdView(adUnitId: String) {
    val str = LocalStrings.current
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 6.dp),
        factory  = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────
// 2. REWARDED AD BUTTON — for CoinWalletScreen "Earn" tab
//    Shows remaining daily watches and a pulsing "Watch Ad" CTA.
// ─────────────────────────────────────────────────────────────
@Composable
fun WatchAdForCoinsCard(
    adManager:         AdManager,
    onWatchAd:         () -> Unit,
    coinsPerAd:        Int     = 10,
    minAdsPerSession:  Int     = 2,
    adsRemainingToday: Int     = -1,   // -1 = unlimited (ignored)
    isAdReady:         Boolean,
    modifier:          Modifier = Modifier,
    watchedCount:      Int     = 0,    // passed from parent so it survives navigation
) {
    val str = LocalStrings.current
    val totalEarnable = coinsPerAd * watchedCount

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF7B1FA2), Color(0xFF1565C0))))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Title row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "📺 Watch & Earn",
                            style      = MaterialTheme.typography.titleLarge,
                            color      = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            str.adNoLimit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.75f),
                        )
                    }
                    Box(
                        modifier         = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🪙", fontSize = 22.sp)
                            Text(
                                "+$coinsPerAd",
                                style      = MaterialTheme.typography.titleMedium,
                                color      = Color(0xFFFFD700),
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }

                // Session earnings strip — shows after first watch
                if (watchedCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(0.12f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            "✅ $watchedCount ad${if (watchedCount == 1) "" else "s"} watched today",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "+$totalEarnable coins earned",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color(0xFFFFD700),
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }

                // CTA button
                Button(
                    onClick  = { onWatchAd() },
                    enabled  = isAdReady,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Color.White,
                        disabledContainerColor = Color.White.copy(0.3f),
                    ),
                ) {
                    if (!isAdReady) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            color       = Color(0xFF7B1FA2),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(str.adLoading, color = Color(0xFF7B1FA2), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Rounded.PlayCircle, null,
                            tint     = Color(0xFF7B1FA2),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Watch $minAdsPerSession Ads — Earn ${coinsPerAd * minAdsPerSession} Coins",
                            color      = Color(0xFF7B1FA2),
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }

                Text(
                    "📌 $minAdsPerSession ads per session · $coinsPerAd coins each · Watch unlimited times.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.6f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 3. NATIVE JOB AD — looks like a job card with str.adSponsored label
//    Used in JobVacanciesScreen between real job listings.
// ─────────────────────────────────────────────────────────────
@Composable
fun NativeSponsoredJobCard(
    sponsorName:    String = "BPSC Coaching Institute",
    headline:       String = "Crack BPSC 2026 — Free Trial Class",
    body:           String = "Join 10,000+ students. Expert faculty. Live classes daily.",
    ctaLabel:       String = "Register Free →",
    onCtaClick:     () -> Unit
) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    Card(
        modifier  = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF3E5F5)),
                        Alignment.Center
                    ) { Text("🎓", fontSize = 20.sp) }
                    Column {
                        Text(sponsorName, style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold, color = cs.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF3E5F5))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(str.adSponsored, style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF7B1FA2), fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.sp)
                            }
                        }
                    }
                }
                Icon(Icons.Rounded.OpenInNew, null,
                    tint     = BpscColors.TextHint,
                    modifier = Modifier.size(16.dp))
            }

            Text(headline, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold, color = cs.onSurface, lineHeight = 22.sp)
            Text(body, style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)

            Button(
                onClick  = onCtaClick,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
            ) {
                Text(ctaLabel, style = MaterialTheme.typography.titleSmall, color = Color.White,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 4. DASHBOARD AD STRIP — subtle between dashboard sections
//    Shown between "My Schedule" and next section.
//    NOT shown to Pro subscribers.
// ─────────────────────────────────────────────────────────────
@Composable
fun DashboardBannerStrip(adUnitId: String, isProUser: Boolean) {
    val str = LocalStrings.current
    if (isProUser) return  // Pro users see no ads
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                str.adAdvertisement,
                style    = MaterialTheme.typography.labelSmall,
                color    = BpscColors.TextHint,
                fontSize = 8.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        BannerAdView(adUnitId = adUnitId)
    }
}

// ─────────────────────────────────────────────────────────────
// 5. POST-SESSION REWARD PROMPT — after study session ends
//    "You studied Xh Xm. Want to earn bonus coins?"
// ─────────────────────────────────────────────────────────────
@Composable
fun PostSessionAdPrompt(
    studyMinutes:    Int,
    coinsEarned:     Int,
    adReady:         Boolean,
    adsRemainingToday: Int,
    onWatchAd:       () -> Unit,
    onSkip:          () -> Unit
) {
    val str = LocalStrings.current
    var dismissed by remember { mutableStateOf(false) }
    if (dismissed) return
    // Only show if session was meaningful (>= 5 min) and ad is available
    if (studyMinutes < 5) return  // show regardless of ad count — no limit

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF004D40), Color(0xFF00695C))))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(str.adGreatSession, style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text(
                    "You studied ${if (studyMinutes >= 60) "${studyMinutes/60}h ${studyMinutes%60}m" else "${studyMinutes}m"} and earned $coinsEarned coins.\nWatch a 30-second ad to earn ${AdManager.REWARDED_COINS} bonus coins!",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = Color.White.copy(0.85f),
                    lineHeight = 20.sp
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = { dismissed = true; onSkip() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, Color.White.copy(0.4f)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(0.7f))
                    ) { Text("Skip", style = MaterialTheme.typography.titleSmall) }

                    Button(
                        onClick  = { dismissed = true; onWatchAd() },
                        enabled  = adReady,
                        modifier = Modifier.weight(2f).height(46.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))
                    ) {
                        Text("🪙 Watch & Earn +${AdManager.REWARDED_COINS}",
                            color = Color(0xFF1A1A1A), fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}