package com.example.bpscnotes.presentation.auth.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen

private val BIHAR_DISTRICTS = listOf(
    "Patna", "Gaya", "Bhagalpur", "Muzaffarpur", "Darbhanga",
    "Araria", "Arwal", "Aurangabad", "Banka", "Begusarai",
    "Bhabua", "Buxar", "Gopalganj", "Jamui", "Jehanabad",
    "Kaimur", "Katihar", "Khagaria", "Kishanganj", "Lakhisarai",
    "Madhepura", "Madhubani", "Munger", "Nalanda", "Nawada",
    "Purnia", "Rohtas", "Saharsa", "Samastipur", "Saran",
    "Sheikhpura", "Sheohar", "Sitamarhi", "Siwan", "Supaul",
    "Vaishali", "West Champaran", "East Champaran"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavHostController,
    tempToken: String,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var showDistrictMenu by remember { mutableStateOf(false) }

    val isLoading       by viewModel.isLoading.observeAsState(false)
    val error           by viewModel.error.observeAsState()
    val registerSuccess by viewModel.registerSuccess.observeAsState(false)

    LaunchedEffect(registerSuccess) {
        if (registerSuccess) {
            viewModel.onNavigationConsumed()
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BpscColors.Surface)
            .imePadding()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(BpscColors.Primary, Color(0xFF1557C0)))
                )
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Create Your Profile",
                    style      = MaterialTheme.typography.headlineSmall,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Just a few details to personalise your experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.75f)
                )
            }
        }

        // Form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BpscColors.PrimaryLight)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Person,
                    null,
                    tint     = BpscColors.Primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Name — required
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Full Name *") },
                placeholder   = { Text("e.g. Rahul Kumar") },
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = BpscColors.Primary,
                    unfocusedBorderColor = BpscColors.Divider
                )
            )

            // Email — optional
            OutlinedTextField(
                value         = email,
                onValueChange = { email = it },
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Email (optional)") },
                placeholder   = { Text("e.g. rahul@gmail.com") },
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = BpscColors.Primary,
                    unfocusedBorderColor = BpscColors.Divider
                )
            )

            // District dropdown — optional
            ExposedDropdownMenuBox(
                expanded        = showDistrictMenu,
                onExpandedChange = { showDistrictMenu = it }
            ) {
                OutlinedTextField(
                    value         = district,
                    onValueChange = { district = it },
                    modifier      = Modifier.fillMaxWidth().menuAnchor(),
                    label         = { Text("District (optional)") },
                    placeholder   = { Text("Select your district") },
                    shape         = RoundedCornerShape(14.dp),
                    singleLine    = true,
                    readOnly      = true,
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(showDistrictMenu) },
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BpscColors.Primary,
                        unfocusedBorderColor = BpscColors.Divider
                    )
                )
                ExposedDropdownMenu(
                    expanded        = showDistrictMenu,
                    onDismissRequest = { showDistrictMenu = false }
                ) {
                    BIHAR_DISTRICTS.forEach { d ->
                        DropdownMenuItem(
                            text    = { Text(d) },
                            onClick = { district = d; showDistrictMenu = false }
                        )
                    }
                }
            }

            // Error
            AnimatedVisibility(visible = error != null) {
                Text(
                    text     = error ?: "",
                    color    = MaterialTheme.colorScheme.error,
                    style    = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            // Submit
            Button(
                onClick  = {
                    viewModel.register(
                        tempToken = tempToken,
                        name      = name,
                        email     = email.trim().takeIf { it.isNotEmpty() },
                        district  = district.trim().takeIf { it.isNotEmpty() }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled  = name.isNotBlank() && !isLoading,
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color     = Color.White,
                        modifier  = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Continue to Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            Text(
                "Your data is secure and never shared.",
                style    = MaterialTheme.typography.bodySmall,
                color    = BpscColors.TextHint,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
