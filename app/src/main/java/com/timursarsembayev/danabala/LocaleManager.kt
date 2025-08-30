package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.*

/**
 * Менеджер для управления языками приложения
 */
object LocaleManager {

    private const val PREF_NAME = "language_settings"
    private const val KEY_LANGUAGE = "selected_language"

    // Поддерживаемые языки
    const val LANGUAGE_RUSSIAN = "ru"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_KAZAKH = "kk"

    private val supportedLanguages = listOf(
        LANGUAGE_RUSSIAN,
        LANGUAGE_ENGLISH,
        LANGUAGE_KAZAKH
    )

    /**
     * Получить список поддерживаемых языков
     */
    fun getSupportedLanguages(): List<String> = supportedLanguages

    /**
     * Получить текущий выбранный язык
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = getPreferences(context)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_RUSSIAN) ?: LANGUAGE_RUSSIAN
    }

    /**
     * Установить новый язык
     */
    fun setLanguage(context: Context, languageCode: String) {
        if (languageCode !in supportedLanguages) {
            throw IllegalArgumentException("Unsupported language: $languageCode")
        }

        val prefs = getPreferences(context)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    /**
     * Применить выбранный язык к контексту
     */
    fun applyLanguage(context: Context, languageCode: String = getCurrentLanguage(context)): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /**
     * Получить название языка на родном языке
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_RUSSIAN -> "Русский"
            LANGUAGE_ENGLISH -> "English"
            LANGUAGE_KAZAKH -> "Қазақша"
            else -> languageCode
        }
    }

    /**
     * Получить ресурс иконки-флага по коду языка
     */
    fun getLanguageFlagRes(languageCode: String): Int {
        return when (languageCode) {
            LANGUAGE_RUSSIAN -> R.drawable.ic_flag_ru
            LANGUAGE_ENGLISH -> R.drawable.ic_flag_us
            LANGUAGE_KAZAKH -> R.drawable.ic_flag_kz
            else -> R.drawable.ic_language
        }
    }

    /**
     * Проверить, нужно ли перезапустить активность при смене языка
     */
    fun shouldRecreateActivity(context: Context, newLanguage: String): Boolean {
        return getCurrentLanguage(context) != newLanguage
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}
