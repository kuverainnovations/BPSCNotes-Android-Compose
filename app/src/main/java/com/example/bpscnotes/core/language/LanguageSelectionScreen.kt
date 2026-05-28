package com.example.bpscnotes.core.language

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    private val languageManager: LanguageManager,
    private val tokenStore: TokenStore,
) : ViewModel() {
    val current: AppLanguage get() = languageManager.current

    fun select(lang: AppLanguage) {
        languageManager.setLanguage(lang)  // uses static companion internally
    }
}

@Composable
fun LanguageSelectionScreen(
    navController: NavHostController,
    vm: LanguageSelectionViewModel = hiltViewModel()
) {
    var selected by remember { mutableStateOf(vm.current) }

    // Determine where to go after selecting language
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                    start  = Offset(0f, 0f),
                    end    = Offset(400f, 900f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Globe icon
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🌐", fontSize = 44.sp)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Choose Your Language",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "अपनी भाषा चुनें",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "You can change this anytime from Settings  •  इसे Settings से बदल सकते हैं",
                fontSize = 12.sp,
                color = Color.White.copy(0.55f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(40.dp))

            // Language cards
            AppLanguage.entries.forEach { lang ->
                LanguageCard(
                    language = lang,
                    isSelected = selected == lang,
                    onSelect = { selected = lang }
                )
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(32.dp))

            // Continue button
            Button(
                onClick = {
                    vm.select(selected)
                    val destination = when {
                        tokenStore.getToken().isNullOrEmpty() ->
                            if (tokenStore.isOnboarded()) Screen.Login.route
                            else Screen.Onboarding.route
                        !tokenStore.isExamSetupDone() -> Screen.ExamSetup.route
                        else -> Screen.Main.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(
                    text = if (selected == AppLanguage.HINDI) "आगे बढ़ें  •  Continue" else "Continue  •  आगे बढ़ें",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BpscColors.Primary
                )
            }
        }
    }
}

@Composable
private fun LanguageCard(
    language: AppLanguage,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.White.copy(0.2f),
        animationSpec = tween(200), label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(0.2f) else Color.White.copy(0.07f),
        animationSpec = tween(200), label = "bg"
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 0.dp,
        animationSpec = tween(200), label = "elevation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Flag + radio
        Text(language.flag, fontSize = 32.sp)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = language.nativeName,
                fontSize = 14.sp,
                color = Color.White.copy(0.7f)
            )
        }

        // Radio indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White else Color.Transparent)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(BpscColors.Primary)
                )
            }
        }
    }
}
