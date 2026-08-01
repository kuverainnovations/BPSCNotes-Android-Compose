package com.example.bpscnotes.presentation.auth.register

import com.example.bpscnotes.core.language.LocalStrings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.BpscDropdown
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavHostController,
    tempToken: String,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val str     = LocalStrings.current
    val cs      = MaterialTheme.colorScheme
    val context = LocalContext.current
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("register") }

    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var showDistrictMenu by remember { mutableStateOf(false) }

    val isLoading       by viewModel.isLoading.observeAsState(false)
    val error           by viewModel.error.observeAsState()
    val registerSuccess by viewModel.registerSuccess.observeAsState(false)
    val districts       by viewModel.districts.collectAsState()
    val districtsLoading by viewModel.districtsLoading.collectAsState()
    val registrationsOpen by viewModel.registrationsOpen.collectAsState()

    LaunchedEffect(registerSuccess) {
        if (registerSuccess) {
            viewModel.onNavigationConsumed()
            // Mark setup as pending so SplashScreen can re-route if app is killed mid-setup
            TokenStore(context).saveOnboardingCompleted(false)
            // ExamSetupScreen replaced OnboardingWizard as the post-register
            // setup flow (2026-07-10) — its ViewModel sets onboardingCompleted
            // back to true on finish/skip.
            navController.navigate(Screen.ExamSetup.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .imePadding()
    ) {
        // ── Header ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(BpscColors.Primary, Color(0xFF1557C0)))
                )
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 28.dp)
            ) {
                // Back button — circular, clearly tappable, breathing room
                // from the screen edge and the title below it.
                IconButton(
                    onClick = { navController.popBackStackSafe() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.16f), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    str.registerCreateProfile,
                    style      = MaterialTheme.typography.headlineMedium,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    str.registerPersonalize,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.78f)
                )
            }
        }

        // ── Form ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Avatar — sits at the top of the form, centered.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cs.surface)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BpscColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint     = BpscColors.Primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Section label — gives the form a clear sense of structure
            Text(
                text = str.registerPersonalDetails,
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.TextHint,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Name — required
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    modifier      = Modifier.fillMaxWidth(),
                    leadingIcon   = { Icon(Icons.Rounded.Person, null, tint = cs.outline) },
                    label         = { Text(str.editFullName) },
                    placeholder   = { Text(str.registerNameHint) },
                    shape         = RoundedCornerShape(14.dp),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction      = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BpscColors.Primary,
                        unfocusedBorderColor = cs.outline
                    )
                )

                // Email — optional
                OutlinedTextField(
                    value         = email,
                    onValueChange = { email = it },
                    modifier      = Modifier.fillMaxWidth(),
                    leadingIcon   = { Icon(Icons.Rounded.Email, null, tint = cs.outline) },
                    label         = { Text("${str.editEmail} (Optional)") },
                    placeholder   = { Text(str.registerEmailHint) },
                    shape         = RoundedCornerShape(14.dp),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction    = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BpscColors.Primary,
                        unfocusedBorderColor = cs.outline
                    )
                )

                // District dropdown — optional, populated from backend
                BpscDropdown(
                    value       = district,
                    label       = "${str.editDistrict} (Optional)",
                    placeholder = if (districtsLoading) "Loading districts…" else str.editDistrict,
                    options     = districts.map { it.name },
                    onSelect    = { name -> district = name },
                    enabled     = districts.isNotEmpty() && !districtsLoading,
                    leadingIcon = { Icon(Icons.Rounded.LocationOn, null, tint = cs.outline) }
                )

                // Referral code — optional. Until now there was no way to enter
                // one at all, so no signup could ever be credited to a referrer
                // and refer-to-earn never paid out to anybody.
                OutlinedTextField(
                    value         = referralCode,
                    onValueChange = { referralCode = it.uppercase().filter { c -> !c.isWhitespace() } },
                    modifier      = Modifier.fillMaxWidth(),
                    leadingIcon   = { Icon(Icons.Rounded.CardGiftcard, null, tint = cs.outline) },
                    label         = { Text(str.registerReferralLabel) },
                    placeholder   = { Text(str.registerReferralHint) },
                    supportingText = { Text(str.registerReferralHelp, style = MaterialTheme.typography.labelSmall) },
                    shape         = RoundedCornerShape(14.dp),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction    = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BpscColors.Primary,
                        unfocusedBorderColor = cs.outline
                    )
                )
            }

            // Error
            AnimatedVisibility(visible = error != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text     = error ?: "",
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Admin "New Registrations" switch is off — say so rather than
            // letting the user fill the form and hit a server rejection.
            if (!registrationsOpen) {
                Surface(
                    color = cs.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        "New registrations are temporarily closed. Please try again later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Submit
            Button(
                onClick  = {
                    viewModel.register(
                        tempToken = tempToken,
                        name      = name,
                        email     = email.trim().takeIf { it.isNotEmpty() },
                        district  = district.trim().takeIf { it.isNotEmpty() },
                        referralCode = referralCode.trim().takeIf { it.isNotEmpty() }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled  = name.isNotBlank() && !isLoading && registrationsOpen,
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = BpscColors.Primary,
                    disabledContainerColor = BpscColors.Primary.copy(alpha = 0.4f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color     = Color.White,
                        modifier  = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        str.registerContinue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = BpscColors.TextHint,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    str.registerDataSecure,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = BpscColors.TextHint
                )
            }
        }
    }
}