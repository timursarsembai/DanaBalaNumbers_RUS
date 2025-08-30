package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.content.Context

class SnakeMathResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsEnabled = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snake_math_results)

        val levels = intent.getIntExtra("levels", 0)
        val won = intent.getBooleanExtra("won", false)
        val levelReached = intent.getIntExtra("level", 1)

        val titleView = findViewById<TextView>(R.id.congratsText)
        val infoView = findViewById<TextView>(R.id.scoreText)
        val motId = resources.getIdentifier("motivationalText", "id", packageName)
        val motView = if (motId != 0) findViewById<TextView>(motId) else null

        if (won) {
            titleView.text = getString(R.string.snake_math_results_win_title)
            infoView.text = getString(R.string.snake_math_results_level_reached_format, levelReached)
            motView?.text = getString(R.string.snake_math_results_win_motivation)
        } else {
            titleView.text = getString(R.string.results_snake_math_title)
            infoView.text = getString(R.string.snake_math_results_level_reached_format, levelReached)
            motView?.text = getString(R.string.snake_math_results_lose_motivation)
        }

        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            startActivity(Intent(this, SnakeMathActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.backToMenuButton).setOnClickListener {
            startActivity(Intent(this, MathExercisesActivity::class.java))
            finish()
        }

        val lang = LocaleManager.getCurrentLanguage(this)
        ttsEnabled = lang != LocaleManager.LANGUAGE_KAZAKH
        if (ttsEnabled) {
            tts = TextToSpeech(this, this)
        }
    }

    override fun onInit(status: Int) {
        if (!ttsEnabled) return
        if (status == TextToSpeech.SUCCESS) {
            val langCode = LocaleManager.getCurrentLanguage(this)
            val tag = when (langCode) {
                LocaleManager.LANGUAGE_RUSSIAN -> "ru-RU"
                LocaleManager.LANGUAGE_ENGLISH -> "en-US"
                else -> null
            }
            if (tag != null) {
                val res = tts?.setLanguage(Locale.forLanguageTag(tag))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            }
            ttsReady = true
            speakResults()
        }
    }

    private fun speakResults() {
        if (!ttsEnabled || !ttsReady) return
        val won = intent.getBooleanExtra("won", false)
        val levelReached = intent.getIntExtra("level", 1)
        val phrase = if (won) {
            getString(R.string.snake_math_results_speak_win_template, levelReached)
        } else {
            getString(R.string.snake_math_results_speak_lose_template, levelReached)
        }
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "snake_results")
    }

    override fun onPause() {
        super.onPause()
        tts?.stop()
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
