package com.example.bpscnotes.presentation.auth.otp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    navController: NavHostController,
    mobile: String,
    otpContext: String = "registration",   // "registration" | "forgot_mpin"
    viewModel: OtpViewModel = hiltViewModel()
) {
    val str = LocalStrings.current
    val cs  = MaterialTheme.colorScheme
    val otpValues       = remember { List(6) { mutableStateOf("") } }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val isLoading       by viewModel.isLoading.observeAsState(false)
    val error           by viewModel.error.observeAsState()
    val result          by viewModel.result.observeAsState()
    val resendSuccess   by viewModel.resendSuccess.observeAsState(false)

    // For forgot_mpin: send OTP on screen entry (LoginViewModel doesn't send it)
    LaunchedEffect(Unit) {
        if (otpContext == "forgot_mpin") {
            viewModel.sendForgotMpinOtp(mobile)
        }
    }

    // Countdown timer — resets on each successful send/resend
    var secondsLeft by remember { mutableIntStateOf(30) }
    var canResend   by remember { mutableStateOf(false) }
    LaunchedEffect(resendSuccess) {
        secondsLeft = 30; canResend = false
        while (secondsLeft > 0) { delay(1000L); secondsLeft-- }
        canResend = true
        viewModel.onResendConsumed()
    }

    // Navigate based on OTP verification result
    LaunchedEffect(result) {
        when (val r = result) {
            is OtpResult.NavigateToMain -> {
                viewModel.onResultConsumed()
                navController.navigate(Screen.Main.route) { popUpTo(0) { inclusive = true } }
            }
            is OtpResult.NavigateToRegister -> {
                viewModel.onResultConsumed()
                navController.navigate(Screen.Register.createRoute(r.tempToken))
            }
            is OtpResult.NavigateToCreateMpin -> {
                viewModel.onResultConsumed()
                navController.navigate(Screen.CreateMpin.route) { popUpTo(0) { inclusive = true } }
            }
            is OtpResult.NavigateToResetMpin -> {
                viewModel.onResultConsumed()
                navController.navigate(Screen.ResetMpin.createRoute(r.mobile, r.otp)) {
                    popUpTo(Screen.Login.route) { inclusive = false }
                }
            }
            null -> {}
        }
    }

    val fullOtp = otpValues.joinToString("") { it.value }

    Column(
        modifier            = Modifier.fillMaxSize().background(cs.background).statusBarsPadding().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Otp.createRoute(mobile)) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = str.back, tint = BpscColors.TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // WhatsApp icon badge
        Box(
            modifier         = Modifier.size(90.dp).clip(CircleShape).background(BpscColors.PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Forum, null, tint = BpscColors.Primary, modifier = Modifier.size(44.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            str.otpTitle,
            style      = MaterialTheme.typography.headlineSmall,
            color      = BpscColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            str.otpSentTo + mobile,
            style     = MaterialTheme.typography.bodyMedium,
            color     = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // WhatsApp delivery hint
        Text(
            "Check your WhatsApp for the 6-digit code",
            style      = MaterialTheme.typography.labelMedium,
            color      = BpscColors.Primary.copy(alpha = 0.7f),
            textAlign  = TextAlign.Center,
            modifier   = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 6 OTP input boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.padding(horizontal = 24.dp)
        ) {
            otpValues.forEachIndexed { index, state ->
                OtpBox(
                    value          = state.value,
                    focusRequester = focusRequesters[index],
                    onValueChange  = { newVal ->
                        if (newVal.isEmpty()) {
                            state.value = ""
                            if (index > 0) focusRequesters[index - 1].requestFocus()
                        } else {
                            if (newVal.length > 1) {
                                // Paste handling — distribute digits across boxes
                                val digits = newVal.filter(Char::isDigit).take(6)
                                digits.forEachIndexed { i, c ->
                                    if (index + i < 6) otpValues[index + i].value = c.toString()
                                }
                                focusRequesters[minOf(index + digits.length - 1, 5)].requestFocus()
                            } else {
                                state.value = newVal.filter(Char::isDigit).take(1)
                                if (state.value.isNotEmpty() && index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            }
                        }
                    },
                    isError  = error != null,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        AnimatedVisibility(visible = error != null) {
            Text(
                text      = error ?: "",
                color     = MaterialTheme.colorScheme.error,
                style     = MaterialTheme.typography.bodyMedium,
                modifier  = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                str.otpDidntReceive + " ",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
            if (canResend) {
                Text(
                    str.otpResend,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = BpscColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable { viewModel.resendOtp(mobile) }
                )
            } else {
                Text(
                    "Resend in ${secondsLeft}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextHint
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick  = { viewModel.verifyOtp(mobile, fullOtp, otpContext) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
            enabled  = fullOtp.length == 6 && !isLoading,
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(str.otpVerify, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Otp.createRoute(mobile)) { inclusive = true }
                    launchSingleTop = true
                }
            }
        ) {
            Text(str.otpChangeNumber, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OtpBox(
    value: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val isFocused = remember { mutableStateOf(false) }
    val borderColor = when {
        isError            -> MaterialTheme.colorScheme.error
        isFocused.value    -> BpscColors.Primary
        value.isNotEmpty() -> BpscColors.Primary.copy(alpha = 0.5f)
        else               -> cs.outline
    }
    BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused.value = it.isFocused }
            .height(54.dp)
            .border(
                width = if (isFocused.value || value.isNotEmpty()) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (value.isNotEmpty()) BpscColors.PrimaryLight else BpscColors.CardBg,
                shape = RoundedCornerShape(12.dp)
            ),
        textStyle     = TextStyle(
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            color      = BpscColors.Primary
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction    = ImeAction.Next
        ),
        singleLine    = true,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) { inner() }
        }
    )
}