package com.example.bpscnotes.presentation.payment

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Play Billing for paid mock tests.
 *
 * Mirrors [GPlayCoursePurchaseManager] exactly — same catalogue scheme,
 * same account binding, same outcome handling — because a mock test is the
 * same kind of thing to Play as a course: a one-time in-app product.
 *
 * Why Play rather than Cashfree: this is digital content bought inside the
 * app, which Play policy requires to go through their billing. Coins remain
 * the other unlock path; this is the money one.
 */
@Singleton
class GPlayQuizPurchaseManager @Inject constructor(
    private val billing: BillingClientWrapper
) {
    /**
     * Must match gplayProductIdForQuiz() in the backend
     * (src/common/utils/gplay-catalog.util.ts) exactly. Computed
     * independently on each side rather than round-tripped, since it is a
     * pure function of the quiz id. Play product IDs allow only
     * [a-z0-9_.] — no hyphens.
     */
    fun productIdFor(quizId: String): String = "quiz_${quizId.replace("-", "")}"

    suspend fun queryProductDetails(quizId: String): ProductDetails? =
        billing.queryProductDetails(
            listOf(productIdFor(quizId)),
            BillingClient.ProductType.INAPP
        ).firstOrNull()

    /**
     * obfuscatedAccountId binds the purchase to the buyer's user id so the
     * backend can confirm a token belongs to whoever is redeeming it rather
     * than trusting the caller's userId. Required, not optional, so no call
     * site can skip it.
     */
    fun launchPurchase(
        activity: Activity,
        productDetails: ProductDetails,
        obfuscatedAccountId: String,
    ): BillingResult =
        billing.launchBillingFlow(
            activity,
            productDetails,
            offerToken = null,
            obfuscatedAccountId = obfuscatedAccountId,
        )

    /**
     * The price Play is about to charge, in rupees — reported to the
     * backend as clientPriceInr. Play locks the price to this exact
     * ProductDetails object at query time and fails the flow outright
     * rather than silently repricing, so a successful purchase's price
     * here is provably what was charged.
     */
    fun priceInrOf(productDetails: ProductDetails): Double =
        (productDetails.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0L) / 1_000_000.0

    /**
     * Suspends until this test's billing flow resolves. Filtered to this
     * quiz's product id, because the underlying flows are shared singletons
     * also consumed for courses, materials and subscriptions.
     */
    suspend fun awaitPurchaseOrUserChoice(quizId: String): GPlayPurchaseOutcome =
        billing.awaitPurchaseOrUserChoice(productIdFor(quizId))
}
