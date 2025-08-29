package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class AudioMatchingResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var allowTts = true

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_matching_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Определяем язык и отключаем TTS для казахского
        val currentLang = LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)
        allowTts = currentLang != LocaleManager.LANGUAGE_KAZAKH

        if (allowTts) tts = TextToSpeech(this, this)

        setupViews()
        setupButtons()
    }

    override fun onInit(status: Int) {
        if (!allowTts) return
        if (status == TextToSpeech.SUCCESS) {
            val langCode = LocaleManager.getCurrentLanguage(this)
            val locale = when (langCode) {
                LocaleManager.LANGUAGE_RUSSIAN -> Locale.forLanguageTag("ru-RU")
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> Locale.forLanguageTag("en-US")
            }
            val result = tts?.setLanguage(locale)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (isTtsReady) speakCongratulation()
        }
    }

    private fun setupViews() {
        val completedLevels = intent.getIntExtra("completed_levels", 10)
        val correctActions = intent.getIntExtra("correct_actions", 0)
        val incorrectActions = intent.getIntExtra("incorrect_actions", 0)
        val finalScore = intent.getIntExtra("final_score", 0)

        findViewById<TextView>(R.id.completedLevelsText).text =
            getString(R.string.audio_matching_completed_levels_format, completedLevels)
        findViewById<TextView>(R.id.correctActionsText).text =
            getString(R.string.audio_matching_correct_actions_format, correctActions)
        findViewById<TextView>(R.id.incorrectActionsText).text =
            getString(R.string.audio_matching_incorrect_actions_format, incorrectActions)
        findViewById<TextView>(R.id.finalScoreText).text =
            getString(R.string.audio_matching_final_score_format, finalScore)

        val accuracy = if (correctActions + incorrectActions > 0) {
            (correctActions * 100) / (correctActions + incorrectActions)
        } else {
            100
        }

        val (messageRes, encouragementRes) = when {
            accuracy >= 90 -> R.string.audio_matching_result_excellent to R.string.audio_matching_result_excellent_desc
            accuracy >= 70 -> R.string.audio_matching_result_good to R.string.audio_matching_result_good_desc
            accuracy >= 50 -> R.string.audio_matching_result_ok to R.string.audio_matching_result_ok_desc
            else -> R.string.audio_matching_result_try to R.string.audio_matching_result_try_desc
        }

        findViewById<TextView>(R.id.resultMessage).setText(messageRes)
        findViewById<TextView>(R.id.encouragementText).setText(encouragementRes)
    }

    private fun speakCongratulation() {
        if (!allowTts || !isTtsReady) return
        val message = findViewById<TextView>(R.id.resultMessage).text.toString()
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "audio_matching_results")
    }

    private fun setupButtons() {
        findViewById<CardView>(R.id.btnPlayAgain).setOnClickListener {
            stopTTS()
            val intent = Intent(this, AudioMatchingActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<CardView>(R.id.btnBackToMenu).setOnClickListener {
            stopTTS()
            val intent = Intent(this, MathExercisesActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            stopTTS()
            finish()
        }
    }

    private fun stopTTS() {
        if (allowTts) tts?.stop()
    }

    override fun onDestroy() {
        if (allowTts) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }
}
