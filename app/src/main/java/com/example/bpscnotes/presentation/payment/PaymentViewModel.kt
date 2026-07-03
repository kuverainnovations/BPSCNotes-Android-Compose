package com.example.bpscnotes.presentation.payment

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.bpscnotes.app.BuildConfig
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.ConfirmCoursePurchaseRequest
import com.example.bpscnotes.data.remote.api.ConfirmSubscriptionRequest
import com.example.bpscnotes.data.remote.api.CreateSubscriptionRequest
import com.example.bpscnotes.data.remote.api.GPlayVerifyRequest
import com.example.bpscnotes.data.remote.api.SubscriptionPlanDto
import com.example.bpscnotes.core.analytics.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentState(
    // Plans
    val plans: List<SubscriptionPlanDto>    = emptyList(),
    val isLoadingPlans: Boolean             = true,
    val selectedPlan: SubscriptionPlanDto?  = null,

    // Coupon
    val couponCode: String                  = "",
    val couponApplied: Boolean              = false,
    val couponError: String?                = null,
    val couponDiscount: Int                 = 0,

    // Coins
    val coinsAvailable: Int                 = 0,
    val coinsToUse: Int                     = 0,
    val coinDiscount: Double               = 0.0,

    // Order — Cashfree
    val subscriptionId: String?             = null,
    val paymentSessionId: String?           = null,
    val providerOrderId: String?            = null,
    val paymentEnvironment: String          = "sandbox",  // 'sandbox' | 'production'
    val finalAmount: Double                = 0.0,
    val isCreatingOrder: Boolean            = false,
    val isConfirming: Boolean               = false,

    // User info (phone required by Cashfree customer_details)
    val userName: String                    = "",
    val userEmail: String                   = "",
    val userPhone: String                   = "",

    // Course purchase (separate from subscription)
    val courseId: String?                   = null,
    val courseTitle: String?                = null,
    val coursePrice: Double                = 0.0,
    val _pendingSessionId: String?          = null,   // stored until user taps Pay

    // Google Play Billing — only active in release builds
    val useGPlay: Boolean                   = false,
    val gplayVerifying: Boolean             = false,

    // Result
    val isSuccess: Boolean                  = false,
    val bonusCoins: Int                     = 0,
    val error: String?                      = null,
)

// Maps backend plan IDs to Google Play subscription product IDs
private val PLAN_TO_GPLAY_PRODUCT = mapOf(
    "monthly"   to "bpscnotes_monthly",
    "quarterly" to "bpscnotes_quarterly",
    "annual"    to "bpscnotes_annual"
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val api: CoursesApiService,
    private val authApi: AuthApiService,
    private val tokenStore: TokenStore,
    private val bus: RefreshEventBus,
    private val billing: BillingClientWrapper,
    val coinsConfig: com.example.bpscnotes.core.config.CoinsConfigRepository) : ViewModel() {

    private val _state = MutableStateFlow(PaymentState())
    val state: StateFlow<PaymentState> = _state.asStateFlow()

    // Keyed by Play product ID; populated in loadPlans() when useGPlay is true
    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    init {
        val useGPlay = !BuildConfig.DEBUG
        _state.update { it.copy(useGPlay = useGPlay) }
        loadPlans()
        loadUserInfo()
        if (useGPlay) {
            collectGPlayPurchases()
        }
    }

    private fun collectGPlayPurchases() {
        viewModelScope.launch {
            // billing.purchasesFlow is a shared singleton flow — also consumed
            // by GPlayCoursePurchaseManager for one-time course purchases.
            // Filtering to known subscription product ids stops this from
            // reacting to a course purchase (which would otherwise get sent
            // to subscriptions/gplay/verify and fail there for the wrong reason).
            billing.purchasesFlow.collect { purchases ->
                purchases.firstOrNull { purchase ->
                    purchase.products.any { it in PLAN_TO_GPLAY_PRODUCT.values }
                }?.let { purchase ->
                    val productId = purchase.products.first { it in PLAN_TO_GPLAY_PRODUCT.values }
                    verifyGPlayPurchase(purchase.purchaseToken, productId)
                }
            }
        }
    }

    fun startGPlayPurchase(activity: Activity) {
        val plan = _state.value.selectedPlan ?: return
        val productId = PLAN_TO_GPLAY_PRODUCT[plan.id] ?: run {
            _state.update { it.copy(error = "No Play Store product for plan: ${plan.id}") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCreatingOrder = true, error = null) }
            try {
                val details = productDetailsCache[productId]
                    ?: billing.queryProductDetails(listOf(productId)).firstOrNull()
                    ?: run {
                        _state.update { it.copy(isCreatingOrder = false, error = "Plan not found on Play Store") }
                        return@launch
                    }
                productDetailsCache[productId] = details
                val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
                val result = billing.launchBillingFlow(activity, details, offerToken)
                if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                    _state.update { it.copy(
                        isCreatingOrder = false,
                        error = "Could not open Play Store checkout (${result.debugMessage})"
                    )}
                } else {
                    _state.update { it.copy(isCreatingOrder = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isCreatingOrder = false, error = parseError(e.message)) }
            }
        }
    }

    private fun verifyGPlayPurchase(purchaseToken: String, productId: String) {
        viewModelScope.launch {
            _state.update { it.copy(gplayVerifying = true, error = null) }
            try {
                val res = api.verifyGPlaySubscription(GPlayVerifyRequest(
                    purchaseToken = purchaseToken,
                    productId     = productId
                ))
                val bonusCoins = res.data?.bonusCoins ?: 0
                val plan       = _state.value.selectedPlan?.id ?: productId
                Event.paymentSuccess(plan, _state.value.finalAmount, "gplay")
                bus.emit(RefreshEvent.SubscriptionChanged)
                _state.update { it.copy(gplayVerifying = false, isSuccess = true, bonusCoins = bonusCoins) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    gplayVerifying = false,
                    error = "Purchase received but activation failed. Contact support with purchase token: ${purchaseToken.take(16)}…"
                )}
            }
        }
    }

    // Mirrors SubscriptionsService.initiate() — keeps preview in sync with backend math.
    private fun coinDiscountFor(requestedCoins: Int, coinsAvailable: Int, base: Double, ceiling: Double = base): Pair<Int, Double> {
        val rate   = coinsConfig.economy.coinToInrRate
        val maxPct = coinsConfig.economy.maxCoinDiscountPctSubscription
        val maxDiscountInr = (base * maxPct / 100.0).toInt()
        val maxCoinsUsable = if (rate > 0) (maxDiscountInr / rate).toInt() else 0
        val coinsToUse = minOf(requestedCoins, coinsAvailable, maxCoinsUsable).coerceAtLeast(0)
        val discount   = minOf(coinsToUse * rate, ceiling.coerceAtLeast(0.0))
        return coinsToUse to discount
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPlans = true) }
            try {
                val plans = api.getSubscriptionPlans().data?.plans ?: emptyList()
                val first = plans.firstOrNull()
                _state.update { it.copy(
                    plans          = plans,
                    isLoadingPlans = false,
                    selectedPlan   = first,
                    finalAmount    = first?.price ?: 0.0
                )}
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingPlans = false, error = "Failed to load plans") }
            }
        }
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val user = authApi.getMe().data?.user
                _state.update { it.copy(
                    userName       = user?.name   ?: "",
                    userEmail      = user?.email  ?: "",
                    userPhone      = user?.mobile ?: "",
                    coinsAvailable = user?.coins  ?: 0
                )}
            } catch (_: Exception) {}
        }
    }

    fun selectPlan(plan: SubscriptionPlanDto) {
        _state.update { s ->
            val base = plan.price ?: 0.0
            val (coinsToUse, coinDis) = coinDiscountFor(s.coinsToUse, s.coinsAvailable, base)
            s.copy(
                selectedPlan   = plan,
                coinsToUse     = coinsToUse,
                coinDiscount   = coinDis,
                couponDiscount = 0,
                couponCode     = "",
                couponApplied  = false,
                finalAmount    = maxOf(1.0, base.toDouble() - coinDis)
            )
        }
    }

    fun setCouponCode(code: String) {
        _state.update { it.copy(couponCode = code.uppercase(), couponError = null) }
    }

    fun applyCoupon() {
        val code = _state.value.couponCode.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            try {
                val plan = _state.value.selectedPlan ?: return@launch
                val res = api.createSubscription(CreateSubscriptionRequest(
                    plan       = plan.id ?: "monthly",
                    couponCode = code,
                    coinsToUse = 0
                ))
                val breakdown = res.data?.breakdown
                if (breakdown != null) {
                    _state.update { it.copy(
                        couponApplied  = true,
                        couponDiscount = breakdown.couponDiscount,
                        couponError    = null,
                        finalAmount    = maxOf(1.0, breakdown.finalAmount.toDouble())
                    )}
                    // Abandon the preview order — real order created on Pay tap
                }
            } catch (e: Exception) {
                _state.update { it.copy(couponError = "Invalid or expired coupon code") }
            }
        }
    }

    fun toggleCoinsDiscount() {
        _state.update { s ->
            val plan = s.selectedPlan ?: return@update s
            val base = plan.price ?: 0.0
            val requested = if (s.coinsToUse > 0) 0 else s.coinsAvailable
            val (coinsToUse, coinDis) = coinDiscountFor(requested, s.coinsAvailable, base, ceiling = base - s.couponDiscount)
            s.copy(
                coinsToUse   = coinsToUse,
                coinDiscount = coinDis,
                finalAmount  = maxOf(1.0, base.toDouble() - s.couponDiscount - coinDis)
            )
        }
    }

    /** Called by the coin slider — sets exact coins the user wants to redeem. */
    fun setCoinsToUse(requested: Int) {
        _state.update { s ->
            val plan = s.selectedPlan ?: return@update s
            val base = plan.price ?: 0.0
            val (coinsToUse, coinDis) = coinDiscountFor(requested, s.coinsAvailable, base, ceiling = base - s.couponDiscount)
            s.copy(
                coinsToUse   = coinsToUse,
                coinDiscount = coinDis,
                finalAmount  = maxOf(1.0, base.toDouble() - s.couponDiscount - coinDis)
            )
        }
    }

    /** Called when user taps "Pay Now" — creates Cashfree order on backend. */
    fun createOrder() {
        val s    = _state.value
        val plan = s.selectedPlan ?: return
        viewModelScope.launch {
            _state.update { it.copy(isCreatingOrder = true, error = null, paymentSessionId = null) }
            Event.paymentInitiated(plan.id ?: "", s.finalAmount)
            try {
                val res = api.createSubscription(CreateSubscriptionRequest(
                    plan       = plan.id ?: "monthly",
                    couponCode = if (s.couponApplied) s.couponCode else null,
                    coinsToUse = s.coinsToUse
                ))
                val data = res.data ?: throw Exception("Invalid response from server")
                val finalAmt        = data.breakdown.finalAmount
                val sessionId       = data.paymentSessionId
                val providerOrderId = data.providerOrderId

                when {
                    // Case 1: Fully covered by coins — auto-confirm, no SDK needed
                    finalAmt == 0 -> {
                        _state.update { it.copy(isCreatingOrder = false, subscriptionId = data.subscriptionId) }
                        confirmSubscription("FREE_COINS_DISCOUNT")
                    }

                    // Case 2: Gateway not configured on server
                    sessionId.isNullOrBlank() -> {
                        _state.update { it.copy(
                            isCreatingOrder = false,
                            subscriptionId  = data.subscriptionId,
                            finalAmount     = finalAmt.toDouble(),
                            error           = "Payment gateway is not configured yet. Please contact support. " +
                                    "(Ref: ${data.subscriptionId.take(8)})"
                        )}
                    }

                    // Case 3: Normal Cashfree flow — SDK picks up paymentSessionId
                    else -> {
                        _state.update { it.copy(
                            isCreatingOrder  = false,
                            subscriptionId   = data.subscriptionId,
                            paymentSessionId = sessionId,
                            providerOrderId  = providerOrderId,
                            paymentEnvironment = data.paymentEnvironment ?: "sandbox",
                            finalAmount      = finalAmt.toDouble()
                        )}
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isCreatingOrder = false, error = parseError(e.message)) }
            }
        }
    }

    /**
     * Called after Cashfree SDK returns success.
     * [cfPaymentId] is the Cashfree payment ID returned by the SDK or
     * fetched server-side — backend verifies via GET /pg/orders/{orderId}/payments.
     */
    fun confirmSubscription(cfPaymentId: String) {
        val subId = _state.value.subscriptionId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isConfirming = true, paymentSessionId = null) }
            try {
                api.confirmSubscription(subId, ConfirmSubscriptionRequest(
                    cfPaymentId   = cfPaymentId,
                    paymentMethod = "upi"
                ))
                val bonusCoins = _state.value.selectedPlan?.bonusCoins ?: 0
                val plan       = _state.value.selectedPlan?.id ?: "subscription"
                Event.paymentSuccess(plan, _state.value.finalAmount, "cashfree")
                // Notify all screens that subscription state has changed so they
                // re-fetch isPurchased / subscription status and remove Buy buttons.
                bus.emit(RefreshEvent.SubscriptionChanged)
                _state.update { it.copy(isConfirming = false, isSuccess = true, bonusCoins = bonusCoins) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isConfirming = false,
                    error        = "Payment received but activation failed. Contact support with payment ID: $cfPaymentId"
                )}
            }
        }
    }

    fun handlePaymentFailure(code: Int, message: String) {
        if (code == 0) {
            // User cancelled — clear session, no error shown
            _state.update { it.copy(paymentSessionId = null) }
            return
        }
        _state.update { it.copy(
            paymentSessionId = null,
            error = "Payment failed (code: $code): $message. Please try again."
        )}
    }

    // ── Course purchase ─────────────────────────────────────────
    fun initCoursePurchase(
        courseId: String, courseTitle: String, price: Double,
        paymentSessionId: String?,
        providerOrderId: String? = null,
        paymentEnvironment: String = "sandbox",
    ) {
        _state.update { it.copy(
            courseId           = courseId,
            courseTitle        = courseTitle,
            coursePrice        = price,
            finalAmount        = price,
            paymentEnvironment = paymentEnvironment,
            // Store separately — LaunchedEffect triggers only when user taps Pay
            _pendingSessionId  = paymentSessionId,
            // Also store providerOrderId so LaunchedEffect(paymentSessionId, providerOrderId)
            // can fire correctly when triggerCoursePayment() sets paymentSessionId.
            providerOrderId    = providerOrderId,
        )}
    }

    /** Call when user taps "Pay" — moves session ID into state to trigger LaunchedEffect. */
    fun triggerCoursePayment() {
        val pending = _state.value._pendingSessionId ?: return
        // FIX: providerOrderId was already stored in initCoursePurchase.
        // Moving paymentSessionId into state triggers the LaunchedEffect in CoursePaymentScreen.
        _state.update { it.copy(paymentSessionId = pending, _pendingSessionId = null) }
    }

    /** Called after Cashfree SDK success for a course purchase. */
    fun confirmCoursePurchase(cfPaymentId: String) {
        val courseId = _state.value.courseId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isConfirming = true) }
            try {
                api.confirmCoursePurchase(courseId, ConfirmCoursePurchaseRequest(
                    cfPaymentId   = cfPaymentId,
                    paymentMethod = "upi"
                ))
                bus.emit(RefreshEvent.CourseEnrolled)
                _state.update { it.copy(isConfirming = false, isSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isConfirming = false,
                    error        = "Payment received but course unlock failed. Contact support: $cfPaymentId"
                )}
            }
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    /** Called immediately after Cashfree SDK launches — prevents double-launch on recompose. */
    fun consumePaymentSessionId() {
        _state.update {
            it.copy(
                paymentSessionId = null,
                providerOrderId = null
            )
        }
    }

    private fun parseError(msg: String?): String {
        if (msg == null) return "Something went wrong. Please try again."
        return when {
            msg.contains("network", true) -> "No internet connection. Please check your connection."
            msg.contains("timeout", true) -> "Request timed out. Please try again."
            msg.contains("coupon",  true) -> "Invalid coupon code."
            msg.contains("coins",   true) -> "Insufficient coins."
            else                           -> msg
        }
    }
}