package com.example.bpscnotes.presentation.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.BpscDropdown
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.core.ui.t.LocalDarkMode
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state   by viewModel.uiState.collectAsState()
    val str     = LocalStrings.current
    val user    = state.user
    val isDark  = LocalDarkMode.current
    val cs      = MaterialTheme.colorScheme

    var name     by remember(user?.name)     { mutableStateOf(user?.name ?: "") }
    var email    by remember(user?.email)    { mutableStateOf(user?.email ?: "") }
    var bio      by remember(user?.bio)      { mutableStateOf(user?.bio ?: "") }
    var district by remember(user?.district) { mutableStateOf(user?.district ?: "") }
    var showDistrictMenu by remember { mutableStateOf(false) }
    val districts        by viewModel.districts.collectAsState()
    val districtsLoading by viewModel.districtsLoading.collectAsState()
    var selectedAvatarUri  by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog     by remember { mutableStateOf(false) }
    var imageUriToCrop     by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // Gallery picker — Android Photo Picker (no storage/media permission needed on
    // any API level; system provides it on 13+ natively and backports it to older
    // versions via Google Play system updates, falling back to the Storage Access
    // Framework otherwise). Opens crop dialog after selection.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            imageUriToCrop = it
            showCropDialog = true
        }
    }

    // Show crop dialog when image is selected
    if (showCropDialog && imageUriToCrop != null) {
        ImageCropDialog(
            imageUri    = imageUriToCrop!!,
            onCropped   = { croppedUri ->
                selectedAvatarUri = croppedUri
                viewModel.uploadAvatar(croppedUri)
                showCropDialog = false
                imageUriToCrop = null
            },
            onDismiss   = {
                showCropDialog = false
                imageUriToCrop = null
            }
        )
    }

    val snackbarHost = remember { SnackbarHostState() }

    // Navigate back on success (optimistic — no delay)
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            viewModel.clearMessages()
            navController.popBackStackSafe()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
        }
    }

    val doSave = {
        viewModel.updateProfile(
            name      = name,
            email     = email.ifEmpty { null },
            bio       = bio.ifEmpty { null },
            district  = district.ifEmpty { null },
            targetYear = null,
            prepLevel = null
        )
    }

    Scaffold(
        snackbarHost       = { SnackbarHost(snackbarHost) },
        containerColor     = cs.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(cs.background)) {

            // ── Gradient header ──────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(
                    listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                    Offset(0f, 0f), Offset(500f, 300f)))) {
                Row(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp).padding(top = 46.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {

                    // Back
                    Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(Color.White.copy(0.12f))
                        .clickable { navController.popBackStackSafe() },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    Text(str.profileEdit, style = MaterialTheme.typography.titleLarge,
                        color = Color.White, fontWeight = FontWeight.ExtraBold)

                    // Save tick button in header
                    Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(
                            if (name.isNotBlank() && !state.isSaving)
                                Color(0xFF4CAF50).copy(0.85f)
                            else Color.White.copy(0.12f)
                        )
                        .clickable(enabled = name.isNotBlank() && !state.isSaving) { doSave() },
                        contentAlignment = Alignment.Center) {
                        if (state.isSaving)
                            CircularProgressIndicator(color = Color.White,
                                modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── Avatar ────────────────────────────────────
                Box(modifier = Modifier.align(Alignment.CenterHorizontally)
                    .clickable { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Box(modifier = Modifier.size(90.dp).clip(CircleShape)
                        .background(Brush.linearGradient(
                            listOf(Color(0xFF1565C0), Color(0xFF0D47A1)))),
                        contentAlignment = Alignment.Center) {
                        if (selectedAvatarUri != null) {
                            // Show newly selected image
                            AsyncImage(
                                model = selectedAvatarUri,
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else if (!user?.avatarUrl.isNullOrBlank()) {
                            // Show existing avatar from server
                            AsyncImage(
                                model = user?.avatarUrl,
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            // Initials fallback
                            Text(
                                name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                                    .take(2).joinToString("").ifEmpty { "?" },
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White, fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    // Camera icon badge — tappable
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(Color(0xFF1565C0))
                        .border(2.dp, cs.surface, CircleShape)
                        .align(Alignment.BottomEnd)
                        .clickable { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center) {
                        if (state.isUploadingAvatar) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // ── Personal Info ──────────────────────────────
                EditSection(title = str.editPersonalInfo, isDark = isDark) {
                    EditField(value = name, label = str.editFullName,
                        icon = Icons.Rounded.Person, onValueChange = { name = it })
                    Spacer(Modifier.height(10.dp))
                    EditField(value = email, label = str.editEmail,
                        icon = Icons.Rounded.Email, keyboardType = KeyboardType.Email,
                        onValueChange = { email = it })
                    Spacer(Modifier.height(10.dp))
                    // District dropdown — same source as registration screen
                    BpscDropdown(
                        value       = district,
                        label       = str.editDistrict,
                        placeholder = if (districtsLoading) "Loading districts…" else str.editDistrict,
                        options     = districts.map { it.name },
                        onSelect    = { name -> district = name },
                        enabled     = districts.isNotEmpty() && !districtsLoading,
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp)) }
                    )
                    Spacer(Modifier.height(10.dp))
                    EditField(value = bio, label = str.editBio,
                        icon = Icons.Rounded.Info, singleLine = false,
                        minLines = 1, maxLines = 6, onValueChange = { bio = it })
                }

                // ── Mobile (read-only) ─────────────────────────
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    Row(modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(BpscColors.Primary.copy(0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PhoneAndroid, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(str.editMobile, style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant)
                            Text(user?.mobile ?: str.noData, style = MaterialTheme.typography.bodyMedium,
                                color = cs.onBackground, fontWeight = FontWeight.SemiBold)
                        }
                        // All users log in via OTP — mobile is always verified by definition.
                        // is_verified (admin manual flag) ≠ mobile_verified (set on every OTP login).
                        // Showing "Not Verified" for OTP-based login is misleading — always show verified.
                        Row(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E7D32).copy(0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                            Text(str.editVerified, style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Save Button ────────────────────────────────
                Button(
                    onClick  = { doSave() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(16.dp),
                    enabled  = name.isNotBlank() && !state.isSaving,
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = Color.White,
                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(str.editSaving, style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(str.editSaveChanges, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun EditSection(title: String, isDark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onBackground,
            fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 10.dp))
        Card(shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun EditField(
    value: String, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        leadingIcon   = { Icon(icon, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp)) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        singleLine    = singleLine,
        minLines      = minLines,
        maxLines      = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = BpscColors.Primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor    = BpscColors.Primary,
        )
    )
}


// ── Built-in Crop Dialog — resizable + draggable ────────────────────────────
@Composable
private fun ImageCropDialog(
    imageUri: Uri,
    onCropped: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val bitmap = remember(imageUri) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.decodeBitmap(
                    android.graphics.ImageDecoder.createSource(context.contentResolver, imageUri)
                ) { decoder, _, _ -> decoder.isMutableRequired = true }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }
        } catch (e: Exception) { null }
    }

    if (bitmap == null) { onDismiss(); return }

    // Crop box state
    var boxLeft   by remember { mutableFloatStateOf(0f) }
    var boxTop    by remember { mutableFloatStateOf(0f) }
    var boxSize   by remember { mutableFloatStateOf(0f) }
    var viewW     by remember { mutableFloatStateOf(0f) }
    var viewH     by remember { mutableFloatStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }

    val HANDLE = 44f  // touch target size for corners in px

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top bar ──────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White)
                    }
                    Text("Crop Photo", color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = {
                        if (viewW > 0 && boxSize > 0) {
                            val imgW = bitmap.width.toFloat()
                            val imgH = bitmap.height.toFloat()
                            // Scale factors from view coords → bitmap coords
                            val scaleX = imgW / viewW
                            val scaleY = imgH / viewH
                            val bx = (boxLeft * scaleX).toInt().coerceIn(0, bitmap.width - 1)
                            val by = (boxTop  * scaleY).toInt().coerceIn(0, bitmap.height - 1)
                            val bw = (boxSize * scaleX).toInt().coerceIn(1, bitmap.width - bx)
                            val bh = (boxSize * scaleY).toInt().coerceIn(1, bitmap.height - by)
                            val cropped = android.graphics.Bitmap.createBitmap(bitmap, bx, by, bw, bh)
                            val scaled  = android.graphics.Bitmap.createScaledBitmap(cropped, 512, 512, true)
                            val file    = java.io.File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
                            onCropped(Uri.fromFile(file))
                        }
                    }) {
                        Text("Use Photo", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold)
                    }
                }

                // ── Image + crop overlay ─────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onGloballyPositioned { coords ->
                            viewW = coords.size.width.toFloat()
                            viewH = coords.size.height.toFloat()
                            if (!initialized && viewW > 0 && viewH > 0) {
                                val initSize = minOf(viewW, viewH) * 0.75f
                                boxSize  = initSize
                                boxLeft  = (viewW - initSize) / 2f
                                boxTop   = (viewH - initSize) / 2f
                                initialized = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay canvas — handles drag (move) and corner resize
                    var dragMode by remember { mutableStateOf("none") } // "move","tl","tr","bl","br"

                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitPointerEvent()
                                        val pos  = down.changes.firstOrNull()?.position ?: continue

                                        // Determine drag mode based on where finger landed
                                        dragMode = when {
                                            // Corners — check 44px touch target
                                            (pos.x - boxLeft).let { it * it } + (pos.y - boxTop).let { it * it } < HANDLE * HANDLE -> "tl"
                                            (pos.x - (boxLeft + boxSize)).let { it * it } + (pos.y - boxTop).let { it * it } < HANDLE * HANDLE -> "tr"
                                            (pos.x - boxLeft).let { it * it } + (pos.y - (boxTop + boxSize)).let { it * it } < HANDLE * HANDLE -> "bl"
                                            (pos.x - (boxLeft + boxSize)).let { it * it } + (pos.y - (boxTop + boxSize)).let { it * it } < HANDLE * HANDLE -> "br"
                                            // Inside box — move
                                            pos.x > boxLeft && pos.x < boxLeft + boxSize &&
                                                    pos.y > boxTop  && pos.y < boxTop  + boxSize -> "move"
                                            else -> "none"
                                        }

                                        // Consume drag events
                                        do {
                                            val event = awaitPointerEvent()
                                            val drag  = event.changes.firstOrNull() ?: break
                                            if (!drag.pressed) break
                                            drag.consume()
                                            val dx = drag.position.x - drag.previousPosition.x
                                            val dy = drag.position.y - drag.previousPosition.y
                                            val minSize = 80f

                                            when (dragMode) {
                                                "move" -> {
                                                    boxLeft = (boxLeft + dx).coerceIn(0f, viewW - boxSize)
                                                    boxTop  = (boxTop  + dy).coerceIn(0f, viewH - boxSize)
                                                }
                                                "br" -> {
                                                    val newSize = (boxSize + dx).coerceIn(minSize, minOf(viewW - boxLeft, viewH - boxTop))
                                                    boxSize = newSize
                                                }
                                                "bl" -> {
                                                    val newSize = (boxSize - dx).coerceIn(minSize, minOf(boxLeft + boxSize, viewH - boxTop))
                                                    boxLeft = (boxLeft + dx).coerceIn(0f, viewW - minSize)
                                                    boxSize = newSize
                                                }
                                                "tr" -> {
                                                    val newSize = (boxSize - dy).coerceIn(minSize, minOf(viewW - boxLeft, boxTop + boxSize))
                                                    boxTop  = (boxTop + dy).coerceIn(0f, viewH - minSize)
                                                    boxSize = newSize
                                                }
                                                "tl" -> {
                                                    val newSize = (boxSize - dx).coerceIn(minSize, minOf(boxLeft + boxSize, boxTop + boxSize))
                                                    boxLeft = (boxLeft + dx).coerceIn(0f, viewW - minSize)
                                                    boxTop  = (boxTop  + dy).coerceIn(0f, viewH - minSize)
                                                    boxSize = newSize
                                                }
                                            }
                                        } while (true)
                                    }
                                }
                            }
                    ) {
                        if (boxSize > 0f) {
                            // Dark overlay outside crop
                            val path = androidx.compose.ui.graphics.Path().apply {
                                addRect(Rect(0f, 0f, size.width, size.height))
                                addRect(Rect(boxLeft, boxTop, boxLeft + boxSize, boxTop + boxSize))
                            }
                            drawPath(path, Color.Black.copy(0.65f), blendMode = BlendMode.SrcOver)

                            // Crop border
                            drawRect(Color.White, topLeft = Offset(boxLeft, boxTop),
                                size = Size(boxSize, boxSize), style = Stroke(2.dp.toPx()))

                            // Grid lines
                            val t = boxSize / 3f
                            repeat(2) { i ->
                                drawLine(Color.White.copy(0.35f),
                                    Offset(boxLeft + t * (i + 1), boxTop),
                                    Offset(boxLeft + t * (i + 1), boxTop + boxSize), 0.8.dp.toPx())
                                drawLine(Color.White.copy(0.35f),
                                    Offset(boxLeft, boxTop + t * (i + 1)),
                                    Offset(boxLeft + boxSize, boxTop + t * (i + 1)), 0.8.dp.toPx())
                            }

                            // Corner handles
                            val hl = 22.dp.toPx(); val hw = 3.dp.toPx()
                            listOf(
                                Triple(Offset(boxLeft, boxTop), 1f, 1f),
                                Triple(Offset(boxLeft + boxSize, boxTop), -1f, 1f),
                                Triple(Offset(boxLeft, boxTop + boxSize), 1f, -1f),
                                Triple(Offset(boxLeft + boxSize, boxTop + boxSize), -1f, -1f)
                            ).forEach { (c, sx, sy) ->
                                drawLine(Color.White, c, Offset(c.x + hl * sx, c.y), hw)
                                drawLine(Color.White, c, Offset(c.x, c.y + hl * sy), hw)
                            }
                        }
                    }
                }

                Text(
                    "Drag inside to move  •  Drag corners to resize",
                    color = Color.White.copy(0.55f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(10.dp)
                )
            }
        }
    }
}