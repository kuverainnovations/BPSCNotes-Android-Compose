package com.example.bpscnotes.presentation.payment

/**
 * Interface that MainActivity must implement to receive Razorpay callbacks.
 *
 * Add to MainActivity:
 *   class MainActivity : ComponentActivity(),
 *       PaymentResultWithDataListener,
 *       RazorpayPaymentListener {
 *
 *   private var onPaymentSuccess: ((String, String) -> Unit)? = null
 *   private var onPaymentFailure: ((Int, String) -> Unit)? = null
 *
 *   override fun setPaymentCallbacks(
 *       onSuccess: (String, String) -> Unit,
 *       onFailure: (Int, String) -> Unit
 *   ) {
 *       onPaymentSuccess = onSuccess
 *       onPaymentFailure = onFailure
 *   }
 *
 *   override fun onPaymentSuccess(paymentId: String?, response: PaymentData?) {
 *       val orderId   = response?.orderId   ?: ""
 *       val signature = response?.signature ?: ""
 *       onPaymentSuccess?.invoke(paymentId ?: "", signature)
 *   }
 *
 *   override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
 *       onPaymentFailure?.invoke(code, response ?: "Payment failed")
 *   }
 * }
 */
interface RazorpayPaymentListener {
    fun setPaymentCallbacks(
        onSuccess: (paymentId: String, signature: String) -> Unit,
        onFailure: (code: Int, message: String) -> Unit
    )
}
