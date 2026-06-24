package com.example.bpscnotes.presentation.payment

/**
 * Interface that MainActivity must implement to receive Cashfree callbacks.
 *
 * How it works:
 *   Payment screens call setPaymentCallbacks() just before launching
 *   CFPaymentGatewayService. MainActivity holds the lambda pair and
 *   forwards the CFPaymentResultCallback result to whichever ViewModel
 *   initiated the payment.
 *
 * Example MainActivity wiring:
 *
 *   class MainActivity : ComponentActivity(), CashfreePaymentListener {
 *
 *     private var onPaymentSuccessCallback: ((String) -> Unit)? = null
 *     private var onPaymentFailureCallback: ((Int, String) -> Unit)? = null
 *
 *     override fun setPaymentCallbacks(
 *         onSuccess: (cfPaymentId: String) -> Unit,
 *         onFailure: (code: Int, message: String) -> Unit
 *     ) {
 *         onPaymentSuccessCallback = onSuccess
 *         onPaymentFailureCallback = onFailure
 *     }
 *
 *     fun onCashfreePaymentSuccess(cfPaymentId: String) {
 *         onPaymentSuccessCallback?.invoke(cfPaymentId)
 *         onPaymentSuccessCallback = null
 *         onPaymentFailureCallback = null
 *     }
 *
 *     fun onCashfreePaymentError(code: Int, message: String) {
 *         onPaymentFailureCallback?.invoke(code, message)
 *         onPaymentSuccessCallback = null
 *         onPaymentFailureCallback = null
 *     }
 *   }
 *
 * Cashfree CFPaymentResultCallback registered in launchCashfree():
 *   val callback = object : CFPaymentResultCallback {
 *     override fun onPaymentVerify(orderId: String) {
 *         // orderId is Cashfree's order_id — we use cfPaymentId from server verify
 *         (activity as? CashfreePaymentListener)
 *             ?.setPaymentCallbacks already wired; call onCashfreePaymentSuccess
 *     }
 *     override fun onError(error: CFErrorResponse, orderId: String) {
 *         (activity as? MainActivity)?.onCashfreePaymentError(error.code.toInt(), error.message)
 *     }
 *   }
 */
interface CashfreePaymentListener {
    fun setPaymentCallbacks(
        onSuccess: (cfPaymentId: String) -> Unit,
        onFailure: (code: Int, message: String) -> Unit
    )
}
