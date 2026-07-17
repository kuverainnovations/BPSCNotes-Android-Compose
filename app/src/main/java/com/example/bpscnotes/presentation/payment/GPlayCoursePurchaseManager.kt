package com.example.bpscnotes.presentation.payment

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// Single shared home for the Google Play Billing side of course purchases —
// consolidates what would otherwise be duplicated across CourseDetailViewModel
// and MyLearningViewModel. Deliberately does NOT touch the existing Cashfree
// purchase flow in either ViewModel; this is only reached from the
// !BuildConfig.DEBUG branch each of them adds around their existing 402
// handling.
@Singleton
class GPlayCoursePurchaseManager @Inject constructor(
    private val billing: BillingClientWrapper
) {
    // Must match gplayProductIdForCourse() in the backend
    // (src/common/utils/gplay-catalog.util.ts) exactly — same deterministic
    // scheme, computed independently on each side rather than round-tripped,
    // since it's a pure function of the course id. Play product IDs allow
    // only [a-z0-9_.], no hyphens.
    fun productIdFor(courseId: String): String = "course_${courseId.replace("-", "")}"

    suspend fun queryProductDetails(courseId: String): ProductDetails? =
        billing.queryProductDetails(
            listOf(productIdFor(courseId)),
            BillingClient.ProductType.INAPP
        ).firstOrNull()

    // obfuscatedAccountId binds this purchase to the buyer's user id so the
    // backend verify call (verifyGPlayCoursePurchase) can confirm a purchase
    // token actually belongs to whoever is redeeming it, instead of trusting
    // the caller's userId alone — required, not optional, so no call site can
    // accidentally skip it.
    fun launchPurchase(activity: Activity, productDetails: ProductDetails, obfuscatedAccountId: String): BillingResult {
        // PBL 8+ supports multiple purchase options/offers per one-time
        // product, each with its own offer token. Our synced catalog products
        // (gplay-catalog.util.ts) only ever have the single default
        // purchase option, which launchBillingFlow resolves when no offer
        // token is passed — so null stays correct until the catalog starts
        // creating additional purchase options or offers.
        return billing.launchBillingFlow(activity, productDetails, offerToken = null, obfuscatedAccountId = obfuscatedAccountId)
    }

    // Price the app is about to launch the flow at, in rupees — this is
    // what gets reported to the backend as clientPriceInr. Play locks price
    // to this exact ProductDetails object at query time (launchBillingFlow
    // fails outright rather than silently repricing if it goes stale), so a
    // successful purchase's price here is provably what was charged.
    fun priceInrOf(productDetails: ProductDetails): Double =
        (productDetails.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0L) / 1_000_000.0

    // Suspends until Play reports a purchase for this specific course's
    // product id. Filtered so it only resolves on a matching emission —
    // billing.purchasesFlow is a shared singleton flow also consumed by
    // PaymentViewModel for subscriptions, so an unfiltered collector here
    // would incorrectly react to subscription purchases too. Cancels
    // cleanly if the calling viewModelScope is cancelled first (e.g. the
    // user backs out of the screen mid-purchase) — no persistent collector
    // left running.
    suspend fun awaitPurchase(courseId: String): Purchase {
        val targetProductId = productIdFor(courseId)
        return billing.purchasesFlow
            .first { purchases -> purchases.any { targetProductId in it.products } }
            .first { targetProductId in it.products }
    }

    // User-choice-aware variant of awaitPurchase: also resolves when the
    // user picks Cashfree on Google's billing-choice screen, handing back
    // the externalTransactionToken the confirm call must carry.
    suspend fun awaitPurchaseOrUserChoice(courseId: String): GPlayPurchaseOutcome =
        billing.awaitPurchaseOrUserChoice(productIdFor(courseId))
}
