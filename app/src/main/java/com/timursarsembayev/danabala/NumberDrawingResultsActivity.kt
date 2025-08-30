package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class NumberDrawingResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun attachBaseContext(newBase: android.content.Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_drawing_results)

        val lang = LocaleManager.getCurrentLanguage(this)
        if (lang != LocaleManager.LANGUAGE_KAZAKH) {
            tts = TextToSpeech(this, this)
        } else {
            tts = null // Отключаем TTS для казахского языка
        }

        setupViews()
    }

    private fun setupViews() {
        // Тексты из ресурсов
        findViewById<TextView>(R.id.messageDisplay)?.text = getString(R.string.number_drawing_results_main_message)
        findViewById<TextView>(R.id.motivationalMessage)?.text = getString(R.string.number_drawing_results_secondary_message)
        findViewById<TextView>(R.id.congratulationsIcon)?.text = getString(R.string.celebration_emoji)

        findViewById<ImageButton>(R.id.backButton)?.setOnClickListener { finish() }

        findViewById<Button>(R.id.restartButton)?.setOnClickListener {
            startActivity(Intent(this, NumberDrawingActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.homeButton)?.setOnClickListener {
            val intent = Intent(this, MathExercisesActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val lang = LocaleManager.getCurrentLanguage(this)
            val locale = when (lang) {
                LocaleManager.LANGUAGE_RUSSIAN -> Locale.forLanguageTag("ru-RU")
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> Locale.getDefault()
            }
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            // Небольшая задержка, чтобы не перебивать системные звуки
            android.os.Handler(mainLooper).postDelayed({
                val praise = resources.getStringArray(R.array.number_drawing_results_praise_phrases).random()
                val motivation = resources.getStringArray(R.array.number_drawing_results_motivation_phrases).random()
                speak(praise)
                speak(motivation)
            }, 600)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
