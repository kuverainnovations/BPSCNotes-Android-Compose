package com.example.bpscnotes.presentation.payment

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// How a launched Play billing flow resolved once the user got past
// Google's billing-choice screen (user choice billing, India program).
sealed interface GPlayPurchaseOutcome {
    /** User paid through Google Play — verify purchaseToken as before. */
    data class PlayPurchase(val purchase: Purchase) : GPlayPurchaseOutcome

    /**
     * User picked our alternative (Cashfree) on the choice screen. The
     * token must ride along to the backend with the Cashfree confirm call
     * so the payment gets reported to the Play Developer API.
     */
    data class AlternativeBillingChosen(val externalTransactionToken: String) : GPlayPurchaseOutcome
}

@Singleton
class BillingClientWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _purchasesFlow = MutableSharedFlow<List<Purchase>>(extraBufferCapacity = 1)
    val purchasesFlow: SharedFlow<List<Purchase>> = _purchasesFlow.asSharedFlow()

    // User choice billing (India): when the app is enrolled in the program,
    // launchBillingFlow shows Google's billing-choice screen first. If the
    // user picks our alternative (Cashfree), this fires INSTEAD of the
    // purchases listener, carrying the externalTransactionToken the backend
    // must report to Play after the Cashfree payment succeeds. While the
    // Play Console enrollment is not yet active, Google never shows the
    // choice screen and this flow simply never emits — the Play-billing
    // path behaves exactly as before, so this is safe to ship ahead of
    // enrollment.
    private val _userChoiceFlow = MutableSharedFlow<UserChoiceDetails>(extraBufferCapacity = 1)
    val userChoiceFlow: SharedFlow<UserChoiceDetails> = _userChoiceFlow.asSharedFlow()

    val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
                _purchasesFlow.tryEmit(purchases)
            }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableUserChoiceBilling { userChoiceDetails ->
            _userChoiceFlow.tryEmit(userChoiceDetails)
        }
        .build()

    private var connected = false

    suspend fun ensureConnected(): Boolean {
        if (connected && client.isReady) return true
        return suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connected = result.responseCode == BillingClient.BillingResponseCode.OK
                    if (cont.isActive) cont.resume(connected)
                }
                override fun onBillingServiceDisconnected() {
                    connected = false
                }
            })
        }
    }

    // productType defaults to SUBS to keep the existing single call site
    // (PaymentViewModel's subscription flow) source-compatible. Course
    // purchases pass BillingClient.ProductType.INAPP explicitly.
    //
    // For one-time products, the offer token to pass into launchBillingFlow
    // comes from productDetails.oneTimePurchaseOfferDetailsList — each
    // entry has an .offerToken, analogous to subscriptionOfferDetails for
    // SUBS. This wrapper doesn't pick one; the caller does (Phase 5).
    suspend fun queryProductDetails(
        productIds: List<String>,
        productType: String = BillingClient.ProductType.SUBS
    ): List<ProductDetails> {
        if (!ensureConnected()) return emptyList()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productIds.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(productType)
                    .build()
            })
            .build()
        val result = client.queryProductDetails(params)
        return if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK)
            result.productDetailsList ?: emptyList()
        else emptyList()
    }

    // Restore/recovery: surfaces purchases Google already has on record for
    // this user+productType, independent of the purchasesFlow emitted by
    // the PurchasesUpdatedListener (which only fires for purchases made in
    // the current billing-flow session). Needed because a purchase can
    // complete without the app ever seeing it happen in-session — killed
    // mid-flow, completed while briefly offline, etc. Callers should run
    // this on app/screen start and re-submit anything PURCHASED-but-unseen
    // to the same backend verify endpoint a fresh purchase uses; there is
    // no client-side acknowledge here on purpose — acknowledgement already
    // happens server-side in CoursesService.verifyGPlayCoursePurchase after
    // it verifies the token, and Google only needs it done once, from
    // either side.
    suspend fun queryPurchases(productType: String): List<Purchase> {
        if (!ensureConnected()) return emptyList()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()
        return suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(params) { result, purchases ->
                if (cont.isActive) {
                    cont.resume(
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases
                        else emptyList()
                    )
                }
            }
        }
    }

    // Awaits whichever way the billing flow launched for [productId]
    // resolves: a completed Play purchase, or the user picking the
    // alternative (Cashfree) option on Google's billing-choice screen.
    // Never resolves if the user just dismisses the flow — callers launch
    // this inside their own cancellable scope, same as awaitPurchase-style
    // collectors.
    suspend fun awaitPurchaseOrUserChoice(productId: String): GPlayPurchaseOutcome =
        kotlinx.coroutines.flow.merge(
            purchasesFlow
                .filter { purchases -> purchases.any { productId in it.products } }
                .map { purchases ->
                    GPlayPurchaseOutcome.PlayPurchase(purchases.first { productId in it.products })
                },
            userChoiceFlow
                .filter { details -> details.products.any { it.id == productId } }
                .map { GPlayPurchaseOutcome.AlternativeBillingChosen(it.externalTransactionToken) },
        ).first()

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String?,
        // Google's account-binding field — passed through verbatim to the
        // backend's purchase record (externalAccountIdentifiers) so a
        // verify call can confirm the purchase belongs to the requesting
        // user instead of trusting a client-supplied id alone. Optional and
        // defaults to null to keep the existing course/subscription call
        // sites source-compatible; only the new study-materials gplay path
        // passes it.
        obfuscatedAccountId: String? = null
    ): BillingResult {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .apply { if (offerToken != null) setOfferToken(offerToken) }
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .apply { if (obfuscatedAccountId != null) setObfuscatedAccountId(obfuscatedAccountId) }
            .build()

        return client.launchBillingFlow(activity, billingFlowParams)
    }
}
