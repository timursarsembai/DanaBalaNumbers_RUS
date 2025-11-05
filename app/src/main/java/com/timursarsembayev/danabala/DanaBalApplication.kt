package com.timursarsembayev.danabalanumbers

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

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

        // Инициализация Google Mobile Ads SDK
        // Указываем, что контент ориентирован на детей (для Family/Designed for Families)
        val requestConfig = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
            .build()
        MobileAds.setRequestConfiguration(requestConfig)
        MobileAds.initialize(this) { /* SDK initialized */ }

        billing = BillingManager(this)
        billing.start()
    }

    override fun attachBaseContext(base: Context) {
        // Применяем язык к базовому контексту
        super.attachBaseContext(LocaleManager.applyLanguage(base))
    }
}
