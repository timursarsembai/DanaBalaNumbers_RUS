package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class BlockMatchResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var allowTts = false
    private var announced = false

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_match_results)

        val score = intent.getIntExtra("score", 0)

        // Локализованные тексты
        val congrats = resources.getStringArray(R.array.block_match_results_congrats_phrases).random()
        findViewById<TextView>(R.id.congratsText).text = congrats
        findViewById<TextView>(R.id.scoreText).text = getString(R.string.block_match_results_score_format, score)
        findViewById<TextView>(R.id.motivationalText)?.text =
            resources.getStringArray(R.array.block_match_results_motivation_phrases).random()

        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            startActivity(Intent(this, BlockMatchActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.backToMenuButton).setOnClickListener {
            startActivity(Intent(this, MathExercisesActivity::class.java))
            finish()
        }

        val lang = LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)
        allowTts = lang != LocaleManager.LANGUAGE_KAZAKH
        if (allowTts) tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (!allowTts) return
        if (status == TextToSpeech.SUCCESS) {
            val locale = when (LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)) {
                LocaleManager.LANGUAGE_RUSSIAN -> Locale.forLanguageTag("ru-RU")
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> null
            }
            if (locale != null) {
                val res = tts?.setLanguage(locale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                if (!announced) {
                    announced = true
                    val title = findViewById<TextView>(R.id.congratsText).text.toString()
                    val message = findViewById<TextView>(R.id.scoreText).text.toString()
                    val motivation = findViewById<TextView>(R.id.motivationalText)?.text?.toString()
                    tts?.speak(title, TextToSpeech.QUEUE_FLUSH, null, null)
                    tts?.speak(message, TextToSpeech.QUEUE_ADD, null, null)
                    if (!motivation.isNullOrBlank()) {
                        tts?.speak(motivation, TextToSpeech.QUEUE_ADD, null, null)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
