package com.example.bpscnotes.presentation.rooms

import com.example.bpscnotes.core.language.LocalStrings
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.RoomTierDto
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/TierPromotionOverlay.kt
//
// Full-screen celebration shown when user is promoted to a
// higher tier. Triggered from RoomsHubScreen when
// state.myTierData.currentTier != previously cached tier.
// ════════════════════════════════════════════════════════════

@Composable
fun TierPromotionOverlay(
    newTier: RoomTierDto,
    onDismiss: () -> Unit
) {
    val str = LocalStrings.current
    val tierColor = try {
        Color(android.graphics.Color.parseColor(newTier.colorHex))
    } catch (e: Exception) { BpscColors.CoinGold }

    // Entrance animation
    val scale   by animateFloatAsState(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "scale")
    val alpha   by animateFloatAsState(1f, tween(400), label = "alpha")

    // Confetti particles
    val particles = remember { List(40) { ConfettiParticle() } }
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val confettiProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "confetti_progress"
    )

    Box(
        modifier  = Modifier.fillMaxSize().background(Color.Black.copy(0.88f)).alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        // Confetti layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val x = (size.width * p.x + confettiProgress * p.speedX * size.width) % size.width
                val y = (size.height * confettiProgress * p.speedY + p.y * 200) % size.height
                rotate(confettiProgress * p.rotation) {
                    drawRect(
                        color  = p.color,
                        topLeft = Offset(x, y),
                        size   = androidx.compose.ui.geometry.Size(p.size, p.size * 0.5f)
                    )
                }
            }
        }

        // Content card
        Column(
            modifier              = Modifier.scale(scale).padding(32.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(16.dp)
        ) {
            // Tier icon with ring
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 4.dp.toPx()
                    val inset  = stroke / 2
                    val sz     = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(tierColor.copy(0.3f), -90f, 360f, false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                        topLeft = Offset(inset, inset), size = sz)
                    drawArc(tierColor, -90f, 270f, false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset), size = sz)
                }
                Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(tierColor.copy(0.15f)).border(2.dp, tierColor, CircleShape), contentAlignment = Alignment.Center) {
                    Text(newTier.iconEmoji?:"", fontSize = 44.sp)
                }
            }

            Text(str.promotionCongrats, style = MaterialTheme.typography.titleMedium, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
            Text(str.promotionPromotedTo, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.8f))
            Text(newTier.name?:"", style = MaterialTheme.typography.displaySmall, color = tierColor, fontWeight = FontWeight.ExtraBold)
            Text("${newTier.coinMultiplier}× coins/hour · ${newTier.xpMultiplier}× XP", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))

            // Perks
            if (newTier.perks.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tierColor.copy(0.15f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(str.promotionPerks, style = MaterialTheme.typography.titleMedium, color = tierColor, fontWeight = FontWeight.Bold)
                        newTier.perks.take(3).forEach { perk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = tierColor, modifier = Modifier.size(14.dp))
                                Text(perk, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.9f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = tierColor)
            ) {
                Text("Enter ${newTier.name} 🚀", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}

// Confetti particle data
private class ConfettiParticle {
    val x         = Random.nextFloat()
    val y         = -Random.nextFloat() * 0.5f
    val speedX    = (Random.nextFloat() - 0.5f) * 0.3f
    val speedY    = 0.3f + Random.nextFloat() * 0.7f
    val rotation  = Random.nextFloat() * 720f
    val size      = 8f + Random.nextFloat() * 10f
    val color     = listOf(
        Color(0xFFFFD700), Color(0xFFFF6B35), Color(0xFF4CAF50),
        Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF9C27B0),
        Color(0xFF00BCD4), Color(0xFFFFEB3B),
    ).random()
}
