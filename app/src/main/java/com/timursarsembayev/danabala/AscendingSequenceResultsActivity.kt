package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class AscendingSequenceResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ascending_sequence_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализируем TTS только для ru/en; для kk — отключаем
        when (LocaleManager.getCurrentLanguage(this)) {
            LocaleManager.LANGUAGE_ENGLISH, LocaleManager.LANGUAGE_RUSSIAN -> {
                tts = TextToSpeech(this, this)
            }
            else -> tts = null
        }

        val score = intent.getIntExtra("score", 0)
        val totalCorrectAnswers = intent.getIntExtra("totalCorrectAnswers", 0)
        val totalQuestions = intent.getIntExtra("totalQuestions", 20)

        displayResults(score, totalCorrectAnswers, totalQuestions)
        setupButtons()
    }

    private fun displayResults(score: Int, correctAnswers: Int, totalQuestions: Int) {
        val scoreText = findViewById<TextView>(R.id.scoreText)
        val accuracyText = findViewById<TextView>(R.id.accuracyText)
        val progressBar = findViewById<ProgressBar>(R.id.accuracyProgressBar)
        val congratsText = findViewById<TextView>(R.id.congratsText)

        scoreText.text = getString(R.string.ascending_score_points, score)

        val accuracy = (correctAnswers.toFloat() / totalQuestions * 100).toInt()
        accuracyText.text = getString(R.string.ascending_results_accuracy_line, correctAnswers, totalQuestions, accuracy)
        progressBar.progress = accuracy

        val message = when {
            accuracy >= 90 -> {
                congratsText.text = getString(R.string.ascending_results_congrats_excellent)
                getString(R.string.ascending_results_message_excellent)
            }
            accuracy >= 70 -> {
                congratsText.text = getString(R.string.ascending_results_congrats_good)
                getString(R.string.ascending_results_message_good)
            }
            accuracy >= 50 -> {
                congratsText.text = getString(R.string.ascending_results_congrats_ok)
                getString(R.string.ascending_results_message_ok)
            }
            else -> {
                congratsText.text = getString(R.string.ascending_results_congrats_try)
                getString(R.string.ascending_results_message_try)
            }
        }

        speakText(message)
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            val intent = Intent(this, AscendingSequenceActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.backToMenuButton).setOnClickListener {
            val intent = Intent(this, MathExercisesActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = when (LocaleManager.getCurrentLanguage(this)) {
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> Locale.forLanguageTag("ru-RU")
            }
            val result = tts!!.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts!!.setLanguage(Locale.getDefault())
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
