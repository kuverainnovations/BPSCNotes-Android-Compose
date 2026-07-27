package com.example.bpscnotes.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.bpscnotes.core.ui.t.BpscColors

// The Answer Writing accent — same indigo as the feature's own header.
private val FabGradient = listOf(Color(0xFF1A237E), Color(0xFF3949AB))

/**
 * Bottom bar with a raised, gradient centre button (Answer Writing) flanked by
 * two tabs on each side — five slots, the middle one a docked FAB that stands
 * out from the rest.
 *
 * [items] must be the four side tabs (left two, then right two). The centre
 * action is separate: it isn't a NavHost tab, it opens the Answer Writing
 * screen full-screen like a FAB.
 */
@Composable
fun BpscBottomNav(
    navController: NavController,
    items: List<BottomNavItem>,
    centerLabel: String,
    onCenterClick: () -> Unit,
    centerIcon: ImageVector = Icons.Rounded.EditNote,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun go(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState    = true
            }
        }
    }

    val left  = items.take(2)
    val right = items.drop(2)

    // The FAB rises this far above the bar; the outer Box reserves exactly that
    // much space on top so nothing is clipped by the bottomBar slot.
    val fabOverhang = 24.dp

    Box(Modifier.fillMaxWidth()) {
        // ── The bar ──────────────────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(top = fabOverhang),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(62.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                left.forEach  { NavCell(it, it.route == currentRoute) { go(it.route) } }
                Spacer(Modifier.weight(1.15f))   // gap under the FAB
                right.forEach { NavCell(it, it.route == currentRoute) { go(it.route) } }
            }
        }

        // ── Centre FAB (Answer Writing) ──────────────────────────
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(FabGradient))
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCenterClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(centerIcon, centerLabel, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Text(
                centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun RowScope.NavCell(item: BottomNavItem, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) BpscColors.Primary else BpscColors.TextSecondary
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BadgedBox(
            badge = { if (item.badgeCount > 0) Badge { Text(item.badgeCount.toString()) } }
        ) {
            Icon(item.icon, item.label, tint = color, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp,
        )
    }
}
