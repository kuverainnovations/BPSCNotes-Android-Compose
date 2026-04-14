package com.example.bpscnotes.presentation.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.bpscnotes.core.ui.t.BpscColors

@Composable
fun BpscBottomNav(
    navController: NavController,
    items: List<BottomNavItem>,
    onFabClick: () -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val leftItems  = items.take(2)
    val rightItems = items.drop(2)

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.18f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue  = 0.45f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier         = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = BpscColors.Divider,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            leftItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick  = { navigateTo(navController, currentRoute, item.route) },
                    icon     = {
                        BadgedBox(badge = {
                            if (item.badgeCount > 0) Badge { Text(item.badgeCount.toString()) }
                        }) {
                            Icon(item.icon, item.label, modifier = Modifier.size(22.dp))
                        }
                    },
                    label  = {
                        Text(
                            item.label,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navItemColors()
                )
            }

            NavigationBarItem(
                selected = false,
                onClick  = {},
                enabled  = false,
                icon     = { Spacer(Modifier.size(22.dp)) },
                label    = { Spacer(Modifier.height(12.dp)) },
                colors   = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )

            rightItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick  = { navigateTo(navController, currentRoute, item.route) },
                    icon     = {
                        BadgedBox(badge = {
                            if (item.badgeCount > 0) Badge { Text(item.badgeCount.toString()) }
                        }) {
                            Icon(item.icon, item.label, modifier = Modifier.size(22.dp))
                        }
                    },
                    label  = {
                        Text(
                            item.label,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navItemColors()
                )
            }
        }

        Box(
            modifier         = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-26).dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0xFF7C4DFF).copy(alpha = pulseAlpha))
            )
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF7C4DFF))
            )
            FloatingActionButton(
                onClick        = onFabClick,
                modifier       = Modifier.size(56.dp),
                shape          = CircleShape,
                containerColor = Color.Transparent,
                elevation      = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF9C6FFF), Color(0xFF5C4DFF))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = "AI Tutor",
                            tint     = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "AI",
                            color      = Color.White,
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun navigateTo(navController: NavController, currentRoute: String?, route: String) {
    if (currentRoute != route) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor   = BpscColors.Primary,
    selectedTextColor   = BpscColors.Primary,
    unselectedIconColor = BpscColors.TextSecondary,
    unselectedTextColor = BpscColors.TextSecondary,
    indicatorColor      = BpscColors.PrimaryLight,
)