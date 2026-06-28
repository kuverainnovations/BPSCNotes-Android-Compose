package com.example.bpscnotes.presentation.payment

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingClientWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _purchasesFlow = MutableSharedFlow<List<Purchase>>(extraBufferCapacity = 1)
    val purchasesFlow: SharedFlow<List<Purchase>> = _purchasesFlow.asSharedFlow()

    val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
                _purchasesFlow.tryEmit(purchases)
            }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
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

    suspend fun queryProductDetails(productIds: List<String>): List<ProductDetails> {
        if (!ensureConnected()) return emptyList()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productIds.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            })
            .build()
        val result = client.queryProductDetails(params)
        return if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK)
            result.productDetailsList ?: emptyList()
        else emptyList()
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String?
    ): BillingResult {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .apply { if (offerToken != null) setOfferToken(offerToken) }
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        return client.launchBillingFlow(activity, billingFlowParams)
    }
}
