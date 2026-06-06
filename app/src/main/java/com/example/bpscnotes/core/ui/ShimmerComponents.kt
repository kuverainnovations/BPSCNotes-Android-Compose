package com.example.bpscnotes.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Shimmer brush ─────────────────────────────────────────────
@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors     = shimmerColors,
        start      = Offset(translateAnim - 200f, 0f),
        end        = Offset(translateAnim, 0f)
    )
}

// ── Base shimmer box ──────────────────────────────────────────
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    Box(modifier.clip(shape).background(shimmerBrush()))
}

// ── Dashboard skeleton ────────────────────────────────────────
@Composable
fun DashboardSkeleton() {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(cs.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Header
        item {
            Box(
                Modifier.fillMaxWidth().height(160.dp)
                    .background(shimmerBrush())
            )
        }
        // Stats row
        item {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    ShimmerBox(
                        Modifier.weight(1f).height(70.dp),
                        RoundedCornerShape(12.dp)
                    )
                }
            }
        }
        // Quiz cards row
        item {
            ShimmerBox(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(14.dp),
                RoundedCornerShape(4.dp))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    ShimmerBox(Modifier.width(170.dp).height(100.dp), RoundedCornerShape(16.dp))
                }
            }
        }
        // Quick access grid
        item {
            Spacer(Modifier.height(20.dp))
            ShimmerBox(Modifier.padding(horizontal = 16.dp).width(100.dp).height(14.dp),
                RoundedCornerShape(4.dp))
            Spacer(Modifier.height(10.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShimmerBox(Modifier.weight(1f).height(90.dp), RoundedCornerShape(16.dp))
                    ShimmerBox(Modifier.weight(1f).height(90.dp), RoundedCornerShape(16.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) {
                        ShimmerBox(Modifier.weight(1f).height(80.dp), RoundedCornerShape(16.dp))
                    }
                }
            }
        }
        // Weekly consistency
        item {
            Spacer(Modifier.height(20.dp))
            ShimmerBox(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(130.dp),
                RoundedCornerShape(16.dp))
        }
        // Daily targets
        item {
            Spacer(Modifier.height(16.dp))
            ShimmerBox(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(100.dp),
                RoundedCornerShape(16.dp))
        }
    }
}

// ── Profile skeleton ──────────────────────────────────────────
@Composable
fun ProfileSkeleton() {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(cs.background)) {
        // Header
        Box(Modifier.fillMaxWidth().height(200.dp).background(shimmerBrush()))
        Spacer(Modifier.height(16.dp))
        // Stats row
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { ShimmerBox(Modifier.weight(1f).height(60.dp), RoundedCornerShape(12.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        // Heatmap
        ShimmerBox(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(100.dp), RoundedCornerShape(16.dp))
        Spacer(Modifier.height(12.dp))
        // Badges
        ShimmerBox(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(100.dp), RoundedCornerShape(16.dp))
        Spacer(Modifier.height(12.dp))
        // Wallet
        ShimmerBox(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(80.dp), RoundedCornerShape(16.dp))
    }
}

// ── List skeleton (quiz list, mock tests, CA, etc) ────────────
@Composable
fun ListScreenSkeleton(
    headerHeight: Dp = 140.dp,
    itemCount: Int = 6,
    itemHeight: Dp = 90.dp
) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize().background(cs.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Header
        item { Box(Modifier.fillMaxWidth().height(headerHeight).background(shimmerBrush())) }
        // Filter chips
        item {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    ShimmerBox(Modifier.width(70.dp).height(32.dp), RoundedCornerShape(20.dp))
                }
            }
        }
        // Items
        items(itemCount) {
            ShimmerBox(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth().height(itemHeight),
                RoundedCornerShape(16.dp)
            )
        }
    }
}

// ── My Learning skeleton ──────────────────────────────────────
@Composable
fun MyLearningSkeleton() {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(cs.background)) {
        // Gradient header matching real screen
        Box(
            Modifier.fillMaxWidth().height(150.dp).background(shimmerBrush())
        ) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Tab pills
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(0.15f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(2) {
                        Box(
                            Modifier.weight(1f).height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (it == 0) androidx.compose.ui.graphics.Color.White.copy(0.3f)
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Subject filter chips
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { ShimmerBox(Modifier.width(70.dp).height(30.dp), RoundedCornerShape(20.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        // Course cards
        repeat(5) {
            ShimmerBox(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth().height(100.dp),
                RoundedCornerShape(16.dp)
            )
        }
    }
}

// ── Active Recall skeleton ────────────────────────────────────
@Composable
fun ActiveRecallSkeleton() {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(cs.background)) {
        // Header
        Box(Modifier.fillMaxWidth().height(120.dp).background(shimmerBrush()))
        Spacer(Modifier.height(16.dp))
        // Mastery stats
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { ShimmerBox(Modifier.weight(1f).height(50.dp), RoundedCornerShape(12.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        // Start card
        ShimmerBox(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(220.dp),
            RoundedCornerShape(24.dp)
        )
    }
}

// ── Card skeleton (generic) ───────────────────────────────────
@Composable
fun CardSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 80.dp
) {
    ShimmerBox(modifier.fillMaxWidth().height(height), RoundedCornerShape(16.dp))
}