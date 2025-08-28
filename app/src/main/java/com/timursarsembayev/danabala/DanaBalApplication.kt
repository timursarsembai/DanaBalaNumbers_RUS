package com.timursarsembayev.danabalanumbers

import android.app.Application
import android.content.Context

/**
 * Главный класс приложения
 */
class DanaBalApplication : Application() {
    lateinit var billing: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        // Применяем выбранный язык при запуске приложения
        LocaleManager.applyLanguage(this)

        billing = BillingManager(this)
        billing.start()
    }

    override fun attachBaseContext(base: Context) {
        // Применяем язык к базовому контексту
        super.attachBaseContext(LocaleManager.applyLanguage(base))
    }
}
