// filepath: app/src/main/java/com/timursarsembayev/danabala/BillingManager.kt
package com.timursarsembayev.danabalanumbers

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*

class BillingManager(private val app: Application) : PurchasesUpdatedListener {
    companion object {
        const val PRODUCT_ID_FULL = "full_version"
        private const val PREFS = "billing_prefs"
        private const val KEY_PREMIUM = "is_premium"
        private const val KEY_PENDING = "is_pending"
        private const val TAG = "BillingManager"
    }

    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var billingClient: BillingClient = BillingClient.newBuilder(app)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    @Volatile
    private var productDetails: ProductDetails? = null

    fun start() {
        if (billingClient.isReady) {
            queryExistingPurchases()
            queryProductDetails()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryExistingPurchases()
                    queryProductDetails()
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                // Попробуем переподключиться позже
            }
        })
    }

    fun isPremium(): Boolean = prefs.getBoolean(KEY_PREMIUM, false)
    fun isPending(): Boolean = prefs.getBoolean(KEY_PENDING, false)

    fun restorePurchases() {
        // Явный запрос существующих покупок
        queryExistingPurchases()
    }

    // Разблокировка для ревью без оплаты
    fun grantPremiumForReview() {
        setPending(false)
        setPremium(true)
    }

    private fun setPremium(value: Boolean) {
        prefs.edit().putBoolean(KEY_PREMIUM, value).apply()
    }

    private fun setPending(value: Boolean) {
        prefs.edit().putBoolean(KEY_PENDING, value).apply()
    }

    private fun queryProductDetails() {
        val products = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_FULL)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, detailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsList.firstOrNull()
                if (productDetails == null) {
                    Log.w(TAG, "ProductDetails not found for $PRODUCT_ID_FULL")
                }
            } else {
                Log.w(TAG, "queryProductDetails failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPurchased = purchases.any { it.products.contains(PRODUCT_ID_FULL) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                val hasPending = purchases.any { it.products.contains(PRODUCT_ID_FULL) && it.purchaseState == Purchase.PurchaseState.PENDING }
                if (hasPurchased) {
                    purchases.forEach { acknowledgeIfNeeded(it) }
                    setPending(false)
                    setPremium(true)
                } else if (hasPending) {
                    setPremium(false)
                    setPending(true)
                } else {
                    setPremium(false)
                    setPending(false)
                }
            } else {
                Log.w(TAG, "queryPurchases failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchase(activity: Activity): Boolean {
        val details = productDetails ?: run {
            queryProductDetails()
            return false
        }
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.products.contains(PRODUCT_ID_FULL)) {
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            acknowledgeIfNeeded(purchase)
                            setPending(false)
                            setPremium(true)
                        }
                        Purchase.PurchaseState.PENDING -> {
                            // Отложенная покупка: ждем одобрение родителя
                            setPremium(false)
                            setPending(true)
                        }
                        else -> { /* UNSPECIFIED_STATE */ }
                    }
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Пользователь отменил: очищаем флаг ожидания
            setPending(false)
        } else {
            Log.w(TAG, "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    setPremium(true)
                } else {
                    Log.w(TAG, "Acknowledge failed: ${result.debugMessage}")
                }
            }
        }
    }
}
