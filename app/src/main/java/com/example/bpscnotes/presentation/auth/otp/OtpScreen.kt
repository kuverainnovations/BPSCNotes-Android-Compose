package com.example.bpscnotes.presentation.auth.otp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(navController: NavHostController, mobile: String) {
    val viewModel: OtpViewModel = hiltViewModel()
    val otpValues = remember { List(6) { mutableStateOf("") } }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val verifySuccess by viewModel.verifySuccess.observeAsState(false)
    val resendSuccess by viewModel.resendSuccess.observeAsState(false)


    // Countdown timer
    var secondsLeft by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }
    LaunchedEffect(resendSuccess) {
        secondsLeft = 30
        canResend = false
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        canResend = true
    }

    LaunchedEffect(verifySuccess) {
        if (verifySuccess) {
            navController.navigate(Screen.ExamSelection.route)
            { popUpTo(Screen.Login.route) { inclusive = true } }
        }
    }

    // Auto-submit when all 6 filled
    val fullOtp = otpValues.joinToString("") { it.value }
    LaunchedEffect(fullOtp) {
        if (fullOtp.length == 6) {
            viewModel.verifyOtp(mobile, fullOtp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BpscColors.Surface)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = BpscColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Illustration / icon
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(BpscColors.PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = null,
                tint = BpscColors.Primary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Verify Your Number",
            style = MaterialTheme.typography.headlineSmall,
            color = BpscColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "OTP sent to +91 $mobile",
            style = MaterialTheme.typography.bodyMedium,
            color = BpscColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 6 OTP boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            otpValues.forEachIndexed { index, state ->
                OtpBox(
                    value = state.value,
                    focusRequester = focusRequesters[index],
                    onValueChange = { newVal ->
                        if (newVal.isEmpty()) {
                            state.value = ""
                            if (index > 0) focusRequesters[index - 1].requestFocus()
                        } else {
                            // Handle paste (multi-char)
                            if (newVal.length > 1) {
                                val digits = newVal.filter(Char::isDigit).take(6)
                                digits.forEachIndexed { i, c ->
                                    if (index + i < 6) otpValues[index + i].value = c.toString()
                                }
                                val lastFocus = minOf(index + digits.length - 1, 5)
                                focusRequesters[lastFocus].requestFocus()
                            } else {
                                state.value = newVal.filter(Char::isDigit).take(1)
                                if (state.value.isNotEmpty() && index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            }
                        }
                    },
                    isError = error != null,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Error
        AnimatedVisibility(visible = error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Resend
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Didn't receive OTP? ",
                style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextSecondary
            )
            if (canResend) {
                Text(
                    text = "Resend",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { viewModel.resendOtp(mobile) }
                )
            } else {
                Text(
                    text = "Resend in ${secondsLeft}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextHint
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Verify button
        Button(
            onClick = { viewModel.verifyOtp(mobile, fullOtp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(54.dp),
            enabled = fullOtp.length == 6 && !isLoading,
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
                    "Verify & Continue",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.popBackStack() }) {
            Text(
                "Change Mobile Number",
                color = BpscColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
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
    val isFocused = remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused.value -> BpscColors.Primary
        value.isNotEmpty() -> BpscColors.Primary.copy(alpha = 0.5f)
        else -> BpscColors.Divider
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused.value = it.isFocused }
            .height(54.dp)
            .border(
                width = if (isFocused.value || value.isNotEmpty()) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (value.isNotEmpty()) BpscColors.PrimaryLight else Color.White,
                shape = RoundedCornerShape(12.dp)
            ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = BpscColors.Primary
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) { inner() }
        }
    )
}