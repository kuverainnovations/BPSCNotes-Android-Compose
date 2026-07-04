package com.example.bpscnotes.presentation.payment

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// Google Play Billing side of study-material purchases — sibling to
// GPlayCoursePurchaseManager, same shape, kept separate rather than shared
// since the two product families have independent id schemes and this
// keeps the course purchase code completely untouched.
//
// No coin-discount support: Play always charges the full synced catalog
// price, so this is only used for the remaining full-price amount — the
// coin discount UI is hidden by the caller whenever this path is taken.
@Singleton
class GPlayMaterialPurchaseManager @Inject constructor(
    private val billing: BillingClientWrapper
) {
    // Must match gplayProductIdForMaterial() in the backend
    // (src/common/utils/gplay-catalog.util.ts) exactly. "mat_" not
    // "material_" — material_ + 32-hex-uuid would be 41 chars, one over
    // Play's 40-char product id limit.
    fun productIdFor(materialId: String): String = "mat_${materialId.replace("-", "")}"

    suspend fun queryProductDetails(materialId: String): ProductDetails? =
        billing.queryProductDetails(
            listOf(productIdFor(materialId)),
            BillingClient.ProductType.INAPP
        ).firstOrNull()

    // obfuscatedAccountId binds this purchase to the buyer's user id so the
    // backend verify call can reject a token replayed by a different
    // account — the course purchase flow is missing this; this path isn't.
    fun launchPurchase(activity: Activity, productDetails: ProductDetails, obfuscatedAccountId: String): BillingResult =
        billing.launchBillingFlow(activity, productDetails, offerToken = null, obfuscatedAccountId = obfuscatedAccountId)

    fun priceInrOf(productDetails: ProductDetails): Double =
        (productDetails.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0L) / 1_000_000.0

    suspend fun awaitPurchase(materialId: String): Purchase {
        val targetProductId = productIdFor(materialId)
        return billing.purchasesFlow
            .first { purchases -> purchases.any { targetProductId in it.products } }
            .first { targetProductId in it.products }
    }
}
