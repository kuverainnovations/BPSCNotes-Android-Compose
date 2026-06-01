package com.example.bpscnotes.presentation.auth.onboarding

import com.example.bpscnotes.core.language.LocalStrings
import com.kuvera.bpscnotes.R
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    @DrawableRes val illustration: Int,
    val backgroundColor: Color,
    val accentColor: Color,
)

@Composable
fun rememberOnboardingPages(): List<OnboardingPage> {
    val str = LocalStrings.current
    return remember(str) {
        listOf(
            OnboardingPage(
                title           = str.onboarding1Title,
                subtitle        = str.onboarding1Subtitle,
                description     = str.onboarding1Body,
                illustration    = R.drawable.ic_onboard_target,
                backgroundColor = Color(0xFFE8F0FD),
                accentColor     = BpscColors.Primary,
            ),
            OnboardingPage(
                title           = str.onboarding2Title,
                subtitle        = str.onboarding2Subtitle,
                description     = str.onboarding2Body,
                illustration    = R.drawable.ic_onboard_recall,
                backgroundColor = Color(0xFFE8FDF4),
                accentColor     = Color(0xFF1A9E75),
            ),
            OnboardingPage(
                title           = str.onboarding3Title,
                subtitle        = str.onboarding3Subtitle,
                description     = str.onboarding3Body,
                illustration    = R.drawable.ic_onboard_group,
                backgroundColor = Color(0xFFFFF4EC),
                accentColor     = BpscColors.Accent,
            ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavHostController) {
    val str   = LocalStrings.current
    val pages = rememberOnboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPage(page = pages[page])
        }

        // Skip button
        if (pagerState.currentPage < pages.size - 1) {
            TextButton(
                onClick = {
                    TokenStore(context).setOnboarded()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 52.dp, end = 16.dp)
            ) {
                Text(str.skip, color = BpscColors.TextSecondary)
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) pages[pagerState.currentPage].accentColor
                                else BpscColors.TextHint
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val isLastPage = pagerState.currentPage == pages.size - 1

            Button(
                onClick = {
                    if (isLastPage) {
                        TokenStore(context).setOnboarded()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].accentColor
                )
            ) {
                Text(
                    text = if (isLastPage) str.onboardingGetStarted else str.next,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    val slideIn = remember { Animatable(-60f) }
    val fadeIn = remember { Animatable(0f) }

    LaunchedEffect(page) {
        launch { slideIn.animateTo(0f, tween(500, easing = EaseOutCubic)) }
        launch { fadeIn.animateTo(1f, tween(600)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(page.backgroundColor)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(0.12f))

        Image(
            painter = painterResource(page.illustration),
            contentDescription = null,
            modifier = Modifier
                .size(260.dp)
                .offset(y = slideIn.value.dp)
                .alpha(fadeIn.value)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = page.subtitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = page.accentColor,
            letterSpacing = 2.sp,
            modifier = Modifier.alpha(fadeIn.value)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = BpscColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(fadeIn.value)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = BpscColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.alpha(fadeIn.value)
        )
    }
}