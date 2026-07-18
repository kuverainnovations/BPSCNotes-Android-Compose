package com.example.bpscnotes.presentation.auth.login

import com.bpscnotes.app.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import androidx.browser.customtabs.CustomTabsIntent
import com.example.bpscnotes.presentation.navigation.Routes.Screen

@Composable
fun LoginScreen(navController: NavHostController) {
    val viewModel: LoginViewModel = hiltViewModel()
    val str = LocalStrings.current
    val cs  = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("login") }
    var mobile by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val destination by viewModel.destination.observeAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Navigate based on MPIN check result
    LaunchedEffect(destination) {
        when (val dest = destination) {
            is com.example.bpscnotes.presentation.auth.login.LoginDestination.Otp -> {
                viewModel.onDestinationConsumed()
                navController.navigate(Screen.Otp.createRouteWithContext(dest.mobile, "registration"))
            }
            is com.example.bpscnotes.presentation.auth.login.LoginDestination.MpinLogin -> {
                viewModel.onDestinationConsumed()
                navController.navigate(Screen.MpinLogin.createRoute(dest.mobile))
            }
            null -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top branded section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(BpscColors.Primary, Color(0xFF1557C0))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_bpsc_logo),
                        contentDescription = "BPSCNotes Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "BPSCNotes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    str.loginSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.75f)
                )
            }
        }

        // Form card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = (-28).dp)
                .clip(RoundedCornerShape(24.dp))
                .background(cs.surface)
                .padding(24.dp)
        ) {
            Text(
                str.loginEnterMobile,
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                str.loginSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mobile input
            OutlinedTextField(
                value = mobile,
                onValueChange = {
                    if (it.length <= 10 && it.all(Char::isDigit)) {
                        mobile = it

                        if (it.length == 10) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text(str.loginMobileHint, color = BpscColors.TextHint) },
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            "🇮🇳 +91",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Divider(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp),
                            color = cs.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = BpscColors.Primary,
                    unfocusedBorderColor    = cs.outline,
                    focusedTextColor        = BpscColors.TextPrimary,
                    unfocusedTextColor      = BpscColors.TextPrimary,
                    cursorColor             = BpscColors.Primary,
                    focusedContainerColor   = BpscColors.CardBg,
                    unfocusedContainerColor = BpscColors.CardBg,
                )
            )

            // Error
            AnimatedVisibility(visible = error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Terms checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { agreed = !agreed }
            ) {
                Checkbox(
                    checked = agreed,
                    onCheckedChange = { agreed = it },
                    colors = CheckboxDefaults.colors(checkedColor = BpscColors.Primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    str.loginIAgree,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
                val context = androidx.compose.ui.platform.LocalContext.current
                Text(
                    str.loginTermsPrivacy,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.Primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        CustomTabsIntent.Builder().build()
                            .launchUrl(context, android.net.Uri.parse("https://bpscnotes.in/privacy-policy/"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.proceed(mobile) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = mobile.length == 10 && agreed && !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        str.loginSendOtp,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = str.loginAutoCreate,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}