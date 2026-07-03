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

    fun launchPurchase(activity: Activity, productDetails: ProductDetails): BillingResult {
        // The installed Billing Library (7.1.1) predates offer tokens for
        // one-time products entirely — ProductDetails.OneTimePurchaseOfferDetails
        // only exposes price fields (checked directly against the decompiled
        // class: getPriceAmountMicros/getFormattedPrice/getPriceCurrencyCode,
        // no getOfferToken). Only subscriptions have offer tokens in this
        // version, so this is always null for a course purchase.
        return billing.launchBillingFlow(activity, productDetails, offerToken = null)
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
}
