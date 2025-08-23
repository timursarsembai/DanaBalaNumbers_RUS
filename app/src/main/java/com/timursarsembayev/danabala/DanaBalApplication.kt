package com.timursarsembayev.danabalanumbers

import android.app.Application

/**
 * Главный класс приложения
 */
class DanaBalApplication : Application() {
    lateinit var billing: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        billing = BillingManager(this)
        billing.start()
    }
}
