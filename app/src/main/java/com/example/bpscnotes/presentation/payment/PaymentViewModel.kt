package com.example.bpscnotes.presentation.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.ConfirmCoursePurchaseRequest
import com.example.bpscnotes.data.remote.api.ConfirmSubscriptionRequest
import com.example.bpscnotes.data.remote.api.CreateSubscriptionRequest
import com.example.bpscnotes.data.remote.api.SubscriptionPlanDto
import com.example.bpscnotes.core.analytics.Event
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val coinDiscount: Int                   = 0,

    // Order
    val subscriptionId: String?             = null,
    val razorpayOrderId: String?            = null,
    val razorpayKeyId: String?              = null,
    val finalAmount: Int                    = 0,
    val isCreatingOrder: Boolean            = false,
    val isConfirming: Boolean               = false,

    // User info for Razorpay prefill
    val userName: String                    = "",
    val userEmail: String                   = "",
    val userPhone: String                   = "",

    // Course purchase (separate from subscription)
    val courseId: String?                   = null,
    val courseTitle: String?                = null,
    val coursePrice: Int                    = 0,
    val _pendingOrderId: String?            = null,  // stored until user taps Pay

    // Result
    val isSuccess: Boolean                  = false,
    val bonusCoins: Int                     = 0,
    val error: String?                      = null,
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val api: CoursesApiService,
    private val authApi: AuthApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentState())
    val state: StateFlow<PaymentState> = _state.asStateFlow()

    init {
        loadPlans()
        loadUserInfo()
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPlans = true) }
            try {
                val plans = api.getSubscriptionPlans().data?.plans ?: emptyList()
                val first = plans.firstOrNull()
                _state.update { it.copy(
                    plans = plans,
                    isLoadingPlans = false,
                    selectedPlan = first,
                    finalAmount  = first?.price ?: 0
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
                    userName       = user?.name ?: "",
                    userEmail      = user?.email ?: "",
                    userPhone      = user?.mobile ?: "",
                    coinsAvailable = user?.coins ?: 0
                )}
            } catch (_: Exception) {}
        }
    }

    fun selectPlan(plan: SubscriptionPlanDto) {
        _state.update { s ->
            val base    = plan.price ?: 0
            val coinDis = if (s.coinsToUse > 0) minOf(s.coinsToUse / 10, base) else 0
            s.copy(
                selectedPlan   = plan,
                coinDiscount   = coinDis,
                couponDiscount = 0,   // reset coupon when plan changes
                couponCode     = "",
                couponApplied  = false,
                finalAmount    = maxOf(1, base - coinDis)
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
            // Validate coupon via API (using create with coupon to preview discount)
            try {
                val plan = _state.value.selectedPlan ?: return@launch
                val res = api.createSubscription(CreateSubscriptionRequest(
                    plan = plan.id ?: "monthly",
                    couponCode = code,
                    coinsToUse = 0
                ))
                val breakdown = res.data?.breakdown
                if (breakdown != null) {
                    _state.update { it.copy(
                        couponApplied  = true,
                        couponDiscount = breakdown.couponDiscount,
                        couponError    = null,
                        finalAmount    = maxOf(1, breakdown.finalAmount)
                    )}
                    // Cancel the pending order immediately (we'll create real one on pay tap)
                    res.data?.subscriptionId?.let { sid ->
                        try { api.confirmSubscription(sid, ConfirmSubscriptionRequest(
                            razorpayOrderId  = "",
                            transactionId    = "COUPON_VALIDATION_ONLY",
                            razorpaySignature = ""
                        )) } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(couponError = "Invalid or expired coupon code") }
            }
        }
    }

    fun toggleCoinsDiscount() {
        _state.update { s ->
            val plan = s.selectedPlan ?: return@update s
            val base = plan.price ?: 0
            val newCoinsToUse = if (s.coinsToUse > 0) 0 else s.coinsAvailable
            val coinDis       = if (newCoinsToUse > 0) minOf(newCoinsToUse / 10, base - s.couponDiscount) else 0
            s.copy(
                coinsToUse   = newCoinsToUse,
                coinDiscount = coinDis,
                finalAmount  = maxOf(1, base - s.couponDiscount - coinDis)
            )
        }
    }

    fun createOrder() {
        val s    = _state.value
        val plan = s.selectedPlan ?: return
        viewModelScope.launch {
            _state.update { it.copy(isCreatingOrder = true, error = null, razorpayOrderId = null) }
                val planId = _state.value.selectedPlan?.id ?: ""
                val amount = _state.value.finalAmount
                Event.paymentInitiated(planId, amount)
            try {
                val res = api.createSubscription(CreateSubscriptionRequest(
                    plan        = plan.id ?: "monthly",
                    couponCode  = if (s.couponApplied) s.couponCode else null,
                    coinsToUse  = s.coinsToUse
                ))
                val data = res.data ?: throw Exception("Invalid response from server")
                val finalAmt  = data.breakdown.finalAmount
                val orderId   = data.razorpayOrderId
                val keyId     = data.razorpayKeyId

                when {
                    // Case 1: Fully covered by coins — auto-confirm, no payment needed
                    finalAmt == 0 -> {
                        _state.update { it.copy(isCreatingOrder = false, subscriptionId = data.subscriptionId) }
                        confirmSubscription("FREE_COINS_DISCOUNT", "", "")
                    }

                    // Case 2: Payment needed but Razorpay not configured on server
                    orderId.isNullOrBlank() || keyId.isNullOrBlank() -> {
                        _state.update { it.copy(
                            isCreatingOrder = false,
                            subscriptionId  = data.subscriptionId,
                            finalAmount     = finalAmt,
                            error           = "Payment gateway is not configured yet. Please contact support or try again later. " +
                                              "(Order ID: ${data.subscriptionId.take(8)})"
                        )}
                    }

                    // Case 3: Normal Razorpay flow
                    else -> {
                        _state.update { it.copy(
                            isCreatingOrder = false,
                            subscriptionId  = data.subscriptionId,
                            razorpayOrderId = orderId,
                            razorpayKeyId   = keyId,
                            finalAmount     = finalAmt
                        )}
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isCreatingOrder = false,
                    error = parseError(e.message)
                )}
            }
        }
    }

    fun confirmSubscription(paymentId: String, orderId: String, signature: String) {
        val subId = _state.value.subscriptionId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isConfirming = true, razorpayOrderId = null) }
            try {
                api.confirmSubscription(subId, ConfirmSubscriptionRequest(
                    razorpayOrderId   = orderId,
                    transactionId     = paymentId,
                    razorpaySignature = signature,
                    paymentMethod     = "upi"
                ))
                val bonusCoins = _state.value.selectedPlan?.bonusCoins ?: 0
                val plan = _state.value.selectedPlan?.id ?: "subscription"
                Event.paymentSuccess(plan, _state.value.finalAmount, "razorpay")
                _state.update { it.copy(
                    isConfirming = false,
                    isSuccess    = true,
                    bonusCoins   = bonusCoins
                )}
            } catch (e: Exception) {
                _state.update { it.copy(
                    isConfirming = false,
                    error        = "Payment received but activation failed. Contact support with payment ID: $paymentId"
                )}
            }
        }
    }

    fun handlePaymentFailure(code: Int, message: String) {
        if (code == 0) {
            // User cancelled — don't show error
            _state.update { it.copy(razorpayOrderId = null) }
            return
        }
        _state.update { it.copy(
            razorpayOrderId = null,
            error = when (code) {
                -1   -> "Payment failed: $message"
                BAD_REQUEST_CODE -> "Invalid payment request. Please try again."
                else -> "Payment failed (code: $code). Please try again or use a different payment method."
            }
        )}
    }

    // ── Course purchase ─────────────────────────────────────────
    fun initCoursePurchase(courseId: String, courseTitle: String, price: Int,
                           razorpayOrderId: String?, razorpayKeyId: String?) {
        _state.update { it.copy(
            courseId        = courseId,
            courseTitle     = courseTitle,
            coursePrice     = price,
            // Don't set razorpayOrderId here — wait for triggerCoursePayment()
            // so LaunchedEffect fires only when user taps Pay button
            razorpayKeyId   = razorpayKeyId,
            finalAmount     = price,
            // Store it separately so we can set it on demand
            _pendingOrderId = razorpayOrderId
        )}
    }

    // Call this when user taps "Pay" — sets orderId in state to fire LaunchedEffect
    fun triggerCoursePayment() {
        val pendingId = _state.value._pendingOrderId ?: return
        _state.update { it.copy(razorpayOrderId = pendingId) }
    }

    fun confirmCoursePurchase(paymentId: String, orderId: String, signature: String) {
        val courseId = _state.value.courseId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isConfirming = true) }
            try {
                api.confirmCoursePurchase(courseId, ConfirmCoursePurchaseRequest(
                    razorpayOrderId   = orderId,
                    razorpayPaymentId = paymentId,
                    razorpaySignature = signature
                ))
                _state.update { it.copy(isConfirming = false, isSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isConfirming = false,
                    error        = "Payment received but course unlock failed. Contact support: $paymentId"
                )}
            }
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    private fun parseError(msg: String?): String {
        if (msg == null) return "Something went wrong. Please try again."
        return when {
            msg.contains("network", true)   -> "No internet connection. Please check your connection."
            msg.contains("timeout", true)   -> "Request timed out. Please try again."
            msg.contains("coupon", true)    -> "Invalid coupon code."
            msg.contains("coins", true)     -> "Insufficient coins."
            else                             -> msg
        }
    }

    companion object { private const val BAD_REQUEST_CODE = 2 }
}
