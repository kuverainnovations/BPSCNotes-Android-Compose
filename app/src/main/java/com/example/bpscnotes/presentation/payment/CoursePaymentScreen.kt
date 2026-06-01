package com.example.bpscnotes.presentation.payment

import com.example.bpscnotes.core.language.LocalStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

@Composable
fun CoursePaymentScreen(
    navController: NavHostController,
    courseId: String,
    courseTitle: String,
    price: Int,
    razorpayOrderId: String? = null,
    razorpayKeyId: String? = null,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Initialize with course details
    LaunchedEffect(courseId) {
        viewModel.initCoursePurchase(courseId, courseTitle, price, razorpayOrderId, razorpayKeyId)
    }

    // Launch Razorpay when order is ready
    LaunchedEffect(state.razorpayOrderId) {
        val orderId = state.razorpayOrderId ?: return@LaunchedEffect
        val keyId   = state.razorpayKeyId   ?: return@LaunchedEffect
        if (orderId.isBlank() || keyId.isBlank()) return@LaunchedEffect

        launchRazorpay(
            context     = context,
            orderId     = orderId,
            keyId       = keyId,
            amount      = price,
            description = "${str.courseEnrollTitle} $courseTitle",
            userName    = state.userName,
            userEmail   = state.userEmail,
            userPhone   = state.userPhone,
            onSuccess   = { paymentId, signature ->
                viewModel.confirmCoursePurchase(paymentId, orderId, signature)
            },
            onFailure   = { code, msg ->
                viewModel.handlePaymentFailure(code, msg)
            },
            str
        )
    }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), BpscColors.Surface)))) {
        when {
            state.isSuccess -> SuccessScreen(
                title   = str.courseUnlocked,
                message = "\"$courseTitle\" " + str.courseUnlockedBody,
                onDone  = { navController.popBackStackSafe() }
            )
            else -> Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                // Header
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color.White.copy(0.15f))
                        .clickable { navController.popBackStackSafe() },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text(str.coursePurchaseComplete, style = MaterialTheme.typography.titleLarge,
                        color = Color.White, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(Modifier.weight(1f))

                // Course detail card
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface)) {
                    Column(modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 48.sp)
                        Text(courseTitle, style = MaterialTheme.typography.titleLarge,
                            color = cs.onSurface, fontWeight = FontWeight.ExtraBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        HorizontalDivider(color = cs.outline)

                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(str.coursePrice, style = MaterialTheme.typography.bodyLarge,
                                color = cs.onSurfaceVariant)
                            Text("₹$price", style = MaterialTheme.typography.titleLarge,
                                color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                        }

                        state.error?.let { err ->
                            Row(modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEE8E8)).padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Warning, null, tint = Color(0xFFE74C3C),
                                    modifier = Modifier.size(16.dp))
                                Text(err, style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF721C24), modifier = Modifier.weight(1f))
                            }
                        }

                        // Benefits
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(str.courseLifetime, str.courseOffline, str.courseCertificate).forEach {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp)).background(Color(0xFFF0F4FF)).padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("🔒", fontSize = 14.sp)
                            Text(str.courseSecure,
                                style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                // Re-trigger Razorpay by setting the order ID in state
                                // (LaunchedEffect watches state.razorpayOrderId)
                                viewModel.triggerCoursePayment()
                            },
                            enabled  = !state.isConfirming && !razorpayOrderId.isNullOrBlank(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                        ) {
                            if (state.isConfirming) {
                                CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(str.courseProcessing)
                            } else {
                                Text("${str.coursePurchaseBtn} ₹$price", style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
