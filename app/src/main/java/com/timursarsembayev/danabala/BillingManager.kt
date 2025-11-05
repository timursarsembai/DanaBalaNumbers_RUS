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
        // Updated for Play Billing Library 8: explicit PendingPurchasesParams enabling one-time products
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    @Volatile
    private var productDetails: ProductDetails? = null

    fun start() {
        // Премиум временно отключён — не подключаемся к биллингу
        // Оставляем no-op, чтобы не влиять на остальной код
    }

    // Премиум полностью выключен
    fun isPremium(): Boolean = false
    fun isPending(): Boolean = false

    fun restorePurchases() {
        // no-op в режиме отключённого премиума
    }

    // Разблокировка для ревью — тоже no-op, премиум выключен
    fun grantPremiumForReview() {
        // no-op
    }

    private fun setPremium(value: Boolean) {
        prefs.edit().putBoolean(KEY_PREMIUM, value).apply()
    }

    private fun setPending(value: Boolean) {
        prefs.edit().putBoolean(KEY_PENDING, value).apply()
    }

    private fun queryProductDetails() {
        // no-op в режиме отключённого премиума
    }

    private fun queryExistingPurchases() {
        // no-op в режиме отключённого премиума
    }

    fun launchPurchase(activity: Activity): Boolean {
        // Покупки отключены — всегда возвращаем false
        return false
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        // no-op в режиме отключённого премиума
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        // no-op в режиме отключённого премиума
    }
}
