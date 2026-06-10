package com.example.bpscnotes.presentation.payment

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.AppStrings
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.SubscriptionPlanDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

// ─────────────────────────────────────────────────────────────
// SUBSCRIPTION PAYMENT SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun SubscriptionPaymentScreen(
    navController: NavHostController,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Launch Razorpay when order is ready.
    // Consume orderId immediately so rotation/recompose doesn't re-trigger.
    LaunchedEffect(state.razorpayOrderId) {
        val orderId = state.razorpayOrderId ?: return@LaunchedEffect
        val keyId   = state.razorpayKeyId   ?: return@LaunchedEffect
        viewModel.consumeRazorpayOrderId()
        launchRazorpay(
            context     = context,
            orderId     = orderId,
            keyId       = keyId,
            amount      = state.finalAmount,
            description = "BPSCNotes ${state.selectedPlan?.name ?: "Premium"} Subscription",
            userName    = state.userName,
            userEmail   = state.userEmail,
            userPhone   = state.userPhone,
            onSuccess   = { paymentId, signature ->
                viewModel.confirmSubscription(paymentId, orderId, signature)
            },
            onFailure   = { code, msg ->
                viewModel.handlePaymentFailure(code, msg)
            },
            str = str
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
        when {
            state.isSuccess -> SuccessScreen(
                title   = str.paymentActivated,
                message = str.paymentWelcome,
                bonusCoins = state.bonusCoins,
                onDone  = { navController.popBackStackSafe() }
            )
            else -> Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                    .statusBarsPadding().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(Color.White.copy(0.15f))
                            .clickable { navController.popBackStackSafe() },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(str.paymentGetPro, style = MaterialTheme.typography.headlineSmall,
                                color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text(str.paymentUnlockAll, style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(0.7f))
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Error
                    state.error?.let { err ->
                        Card(shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE8E8))) {
                            Row(modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(18.dp))
                                Text(err, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF721C24), modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.Close, null, tint = Color(0xFFE74C3C),
                                    modifier = Modifier.size(16.dp).clickable { viewModel.clearError() })
                            }
                        }
                    }

                    // Features card
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surface),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(str.paymentWhatsIncluded, style = MaterialTheme.typography.titleMedium,
                                color = cs.onSurface, fontWeight = FontWeight.Bold)
                            val features = listOf(
                                str.paymentBenefit1,
                                str.paymentBenefit2,
                                str.paymentBenefit3,
                                str.paymentBenefit4,
                                str.paymentBenefit5,
                                str.paymentBenefit6,
                                str.paymentBenefit7
                            )
                            features.forEach { f ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, null, tint = Color(0xFF2ECC71),
                                        modifier = Modifier.size(16.dp))
                                    Text(f, style = MaterialTheme.typography.bodyMedium,
                                        color = cs.onSurface)
                                }
                            }
                        }
                    }

                    // Plan selector
                    Text(str.paymentChoosePlan, style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface, fontWeight = FontWeight.Bold)

                    if (state.isLoadingPlans) {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BpscColors.Primary)
                        }
                    } else {
                        state.plans.forEach { plan ->
                            PlanCard(
                                plan       = plan,
                                isSelected = plan.id == state.selectedPlan?.id,
                                onSelect   = { viewModel.selectPlan(plan) }
                            )
                        }
                    }

                    // Coupon code
                    CouponSection(
                        couponCode    = state.couponCode,
                        couponApplied = state.couponApplied,
                        couponError   = state.couponError,
                        onCodeChange  = { viewModel.setCouponCode(it) },
                        onApply       = { viewModel.applyCoupon() }
                    )

                    // Price breakdown
                    state.selectedPlan?.let { plan ->
                        PriceBreakdown(
                            baseAmount     = plan.price ?: 0,
                            couponDiscount = state.couponDiscount,
                            coinDiscount   = state.coinDiscount,
                            finalAmount    = state.finalAmount,
                            coinsAvailable = state.coinsAvailable,
                            coinsToUse     = state.coinsToUse,
                            onCoinsToggle  = { viewModel.toggleCoinsDiscount() }
                        )
                    }

                    // Payment note
                    Row(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F4FF))
                        .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("🔒", fontSize = 16.sp)
                        Text(str.paymentSecure,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant)
                    }
                }

                // Pay button
                Box(modifier = Modifier.fillMaxWidth().background(cs.surface)
                    .padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Button(
                        onClick  = { viewModel.createOrder() },
                        enabled  = state.selectedPlan != null && !state.isCreatingOrder && !state.isConfirming,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                    ) {
                        if (state.isCreatingOrder || state.isConfirming) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(if (state.isCreatingOrder) str.paymentCreating else str.paymentConfirming,
                                style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text("Pay ₹${state.finalAmount} →",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

// ── Plan card ─────────────────────────────────────────────────
@Composable
private fun PlanCard(plan: SubscriptionPlanDto, isSelected: Boolean, onSelect: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val savings = plan.savings ?: 0
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BpscColors.PrimaryLight else Color.White),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, BpscColors.Primary)
        else null,
        elevation = CardDefaults.cardElevation(if (isSelected) 0.dp else 2.dp)) {
        Row(modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButton(selected = isSelected, onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = BpscColors.Primary))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(plan.name ?: "", style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface, fontWeight = FontWeight.Bold)
                    if (savings > 0) {
                        Text("Save $savings%", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold, fontSize = 9.sp,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8FDF4)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text(plan.billingCycle ?: "", style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant)
                if ((plan.bonusCoins ?: 0) > 0) {
                    Text("🪙 +${plan.bonusCoins} bonus coins", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF57F17), fontWeight = FontWeight.Bold)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if ((plan.originalPrice ?: 0) > (plan.price ?: 0)) {
                    Text("₹${plan.originalPrice}", style = MaterialTheme.typography.bodySmall,
                        color = BpscColors.TextHint, textDecoration = TextDecoration.LineThrough)
                }
                Text("₹${plan.price}", style = MaterialTheme.typography.titleLarge,
                    color = if (isSelected) BpscColors.Primary else BpscColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ── Coupon section ────────────────────────────────────────────
@Composable
private fun CouponSection(
    couponCode: String, couponApplied: Boolean, couponError: String?,
    onCodeChange: (String) -> Unit, onApply: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(str.paymentCoupon, style = MaterialTheme.typography.labelLarge,
            color = cs.onSurface, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = couponCode, onValueChange = onCodeChange,
                modifier = Modifier.weight(1f),
                singleLine = true, placeholder = { Text(str.paymentEnterCoupon) },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BpscColors.Primary,
                    unfocusedBorderColor = cs.outline,
                    disabledBorderColor = Color(0xFF2ECC71)
                ),
                enabled = !couponApplied,
                trailingIcon = if (couponApplied) ({
                    Icon(Icons.Rounded.Check, null, tint = Color(0xFF2ECC71))
                }) else null
            )
            Button(onClick = onApply, enabled = couponCode.isNotBlank() && !couponApplied,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (couponApplied) Color(0xFF2ECC71) else BpscColors.Primary)) {
                Text(if (couponApplied) str.paymentApplied else str.paymentApply)
            }
        }
        couponError?.let { err ->
            Text(err, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE74C3C))
        }
        if (couponApplied) {
            Text(str.paymentSuccess, style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF2ECC71))
        }
    }
}

// ── Price breakdown ───────────────────────────────────────────
@Composable
private fun PriceBreakdown(
    baseAmount: Int, couponDiscount: Int, coinDiscount: Int, finalAmount: Int,
    coinsAvailable: Int, coinsToUse: Int, onCoinsToggle: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(str.paymentBreakdown, style = MaterialTheme.typography.titleSmall,
                color = cs.onSurface, fontWeight = FontWeight.Bold)
            PriceRow(str.paymentBasePrice, "₹$baseAmount")
            if (couponDiscount > 0) PriceRow(str.paymentCouponDiscount, "−₹$couponDiscount", Color(0xFF2ECC71))
            if (coinDiscount > 0)   PriceRow("Coin discount (${coinsToUse} 🪙)", "−₹$coinDiscount", Color(0xFFF57F17))
            HorizontalDivider(color = cs.outline)
            PriceRow(str.paymentTotal, "₹$finalAmount", BpscColors.Primary, bold = true)

            // Coins toggle
            if (coinsAvailable > 0) {
                Row(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFF8E1))
                    .clickable(onClick = onCoinsToggle)
                    .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🪙", fontSize = 18.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Use ${coinsAvailable} coins", style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5D4037), fontWeight = FontWeight.SemiBold)
                        Text("Save ₹${coinsAvailable / 10} (10 coins = ₹1)",
                            style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D6E63))
                    }
                    Switch(checked = coinsToUse > 0, onCheckedChange = { onCoinsToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF57F17)))
                }
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String, color: Color = BpscColors.TextPrimary, bold: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal)
    }
}

// ── Success screen ────────────────────────────────────────────
@Composable
fun SuccessScreen(title: String, message: String, bonusCoins: Int = 0, onDone: () -> Unit) {
    val str = LocalStrings.current
    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), BpscColors.Surface))),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)) {
            Text("🎉", fontSize = 64.sp)
            Text(title, style = MaterialTheme.typography.headlineMedium,
                color = Color.White, fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (bonusCoins > 0) {
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
                    Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙", fontSize = 24.sp)
                        Text("+$bonusCoins bonus coins added to your wallet!",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text(str.paymentStartLearning, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ── Razorpay launcher helper ──────────────────────────────────
fun launchRazorpay(
    context: Context,
    orderId: String,
    keyId: String,
    amount: Int,
    description: String,
    userName: String,
    userEmail: String,
    userPhone: String,
    onSuccess: (paymentId: String, signature: String) -> Unit,
    onFailure: (code: Int, message: String) -> Unit,
    str: AppStrings
) {
    try {
        val checkout = com.razorpay.Checkout()
        checkout.setKeyID(keyId)
        checkout.setImage(com.kuvera.bpscnotes.R.mipmap.ic_launcher)

        val options = org.json.JSONObject().apply {
            put("name",        "BPSCNotes")
            put("description", description)
            put("order_id",    orderId)
            put("currency",    "INR")
            put("amount",      amount * 100)  // paise
            put("prefill", org.json.JSONObject().apply {
                put("name",    userName)
                put("email",   userEmail)
                put("contact", userPhone)
            })
            put("theme", org.json.JSONObject().apply {
                put("color", "#1565C0")
            })
            // Preferred payment methods — UPI first
            put("config", org.json.JSONObject().apply {
                put("display", org.json.JSONObject().apply {
                    put("blocks", org.json.JSONObject().apply {
                        put("utib", org.json.JSONObject().apply {
                            put("name",        "Pay via UPI")
                            put("instruments", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply {
                                    put("method", "upi")
                                    put("flows",  org.json.JSONArray().apply {
                                        put("qr"); put("intent"); put("collect")
                                    })
                                })
                            })
                        })
                        put("other", org.json.JSONObject().apply {
                            put("name",        "Other Payment Methods")
                            put("instruments", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply { put("method", "card") })
                                put(org.json.JSONObject().apply { put("method", "netbanking") })
                                put(org.json.JSONObject().apply { put("method", "wallet") })
                            })
                        })
                    })
                    put("sequence", org.json.JSONArray().apply { put("block.utib"); put("block.other") })
                    put("preferences", org.json.JSONObject().apply { put("show_default_blocks", false) })
                })
            })
        }

        val activity = context as Activity
        // Set up result listener on the activity
        (activity as? com.example.bpscnotes.presentation.payment.RazorpayPaymentListener)
            ?.setPaymentCallbacks(onSuccess, onFailure)
        checkout.open(activity, options)
    } catch (e: Exception) {
        onFailure(-1, e.message ?: str.paymentOpenFailed)
    }
}
