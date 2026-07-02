package com.example.bpscnotes.core.language

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.bpscnotes.app.R

import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    private val languageManager: LanguageManager,
    private val tokenStore: TokenStore,
) : ViewModel() {
    val current: AppLanguage get() = languageManager.current
    fun select(lang: AppLanguage) { languageManager.setLanguage(lang) }
}

@Composable
fun LanguageSelectionScreen(
    navController: NavHostController,
    vm: LanguageSelectionViewModel = hiltViewModel()
) {
    var selected by remember { mutableStateOf(vm.current) }
    val context    = LocalContext.current
    val tokenStore = remember { TokenStore(context) }

    // Subtle floating animation for the emblem
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                    start  = Offset(0f, 0f),
                    end    = Offset(400f, 1000f)
                )
            )
    ) {
        // Decorative circles in background
        Box(
            Modifier.size(320.dp).offset(x = (-80).dp, y = (-80).dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.04f))
        )
        Box(
            Modifier.size(200.dp).align(Alignment.BottomEnd).offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.04f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // BPSC Emblem + App name
            Box(
                Modifier
                    .offset(y = floatY.dp)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF1976D2), Color(0xFF0D47A1))
                        )
                    )
                    .border(2.dp, Color.White.copy(0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
//                Text("📚", fontSize = 48.sp)
                androidx.compose.foundation.Image(
                    painter            = painterResource(id = R.drawable.ic_bpsc_logo),
                    contentDescription = "BPSC Logo",
                    modifier           = Modifier.size(86.dp).clip(CircleShape),
                    contentScale       = androidx.compose.ui.layout.ContentScale.Fit
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "BPSCNotes",
                fontSize   = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                "BPSC Aspirants का साथी",
                fontSize   = 14.sp,
                color      = Color(0xFF90CAF9),
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(40.dp))

            // Section label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(0.2f))
                Text(
                    "अपनी भाषा चुनें  •  Choose Language",
                    fontSize  = 12.sp,
                    color     = Color.White.copy(0.6f),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(0.2f))
            }

            Spacer(Modifier.height(20.dp))

            // Language option cards
            AppLanguage.entries.forEach { lang ->
                LanguageOptionCard(
                    language   = lang,
                    isSelected = selected == lang,
                    onSelect   = { selected = lang }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "⚙️ आप Settings से कभी भी बदल सकते हैं",
                fontSize  = 11.sp,
                color     = Color.White.copy(0.45f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Continue button
            Button(
                onClick = {
                    vm.select(selected)
                    val dest = when {
                        tokenStore.getToken().isNullOrEmpty() ->
                            if (tokenStore.isOnboarded()) Screen.Login.route
                            else Screen.Onboarding.route
                        !tokenStore.isExamSetupDone() -> Screen.ExamSetup.route
                        else -> Screen.Main.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFD5BF49))
            ) {
                Text(
                    if (selected == AppLanguage.HINDI) "आगे बढ़ें  →" else "Continue  →",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color(0xFF051D56)
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    language: AppLanguage,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.02f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "card_scale"
    )
    val bgBrush = if (isSelected)
        Brush.linearGradient(listOf(Color(0xFF1976D2).copy(0.5f), Color(0xFF0D47A1).copy(0.4f)))
    else
        Brush.linearGradient(listOf(Color.White.copy(0.07f), Color.White.copy(0.05f)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .then(
                if (isSelected)
                    Modifier.border(2.dp, Color(0xFF8D8247), RoundedCornerShape(20.dp))
                else
                    Modifier.border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(20.dp))
            )
            .clickable { onSelect() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Flag circle
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF8D8247).copy(0.15f)
                        else Color.White.copy(0.08f)
                    ),
                Alignment.Center
            ) {
                Text(language.flag, fontSize = 28.sp)
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    language.displayName,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
                Text(
                    language.nativeName,
                    fontSize = 13.sp,
                    color    = Color.White.copy(if (isSelected) 0.85f else 0.6f)
                )
            }

            // Selection indicator
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF8D8247) else Color.Transparent
                    )
                    .border(
                        2.dp,
                        if (isSelected) Color(0xFF8D8247) else Color.White.copy(0.4f),
                        CircleShape
                    ),
                Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF051D56))
                }
            }
        }
    }
}