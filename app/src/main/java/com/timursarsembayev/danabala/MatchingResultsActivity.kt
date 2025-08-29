package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.random.Random

class MatchingResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var allowTts = true

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matching_results)

        // Определяем язык и отключаем TTS для казахского
        val currentLang = LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)
        allowTts = currentLang != LocaleManager.LANGUAGE_KAZAKH

        if (allowTts) {
            tts = TextToSpeech(this, this)
        }

        // Получаем новые данные о статистике
        val completedLevels = intent.getIntExtra("completed_levels", 0)
        val correctActions = intent.getIntExtra("correct_actions", 0)
        val incorrectActions = intent.getIntExtra("incorrect_actions", 0)
        val finalScore = intent.getIntExtra("final_score", 0)

        val titleText = findViewById<TextView>(R.id.titleText)
        val scoreText = findViewById<TextView>(R.id.scoreText)
        val levelsText = findViewById<TextView>(R.id.levelsText)
        val correctActionsText = findViewById<TextView>(R.id.correctActionsText)
        val incorrectActionsText = findViewById<TextView>(R.id.incorrectActionsText)
        val feedbackText = findViewById<TextView>(R.id.feedbackText)
        val playAgainButton = findViewById<Button>(R.id.playAgainButton)
        val homeButton = findViewById<Button>(R.id.homeButton)

        // Заголовок из ресурсов
        titleText.setText(R.string.matching_results_title)

        // Устанавливаем результаты из ресурсов
        scoreText.text = getString(R.string.matching_score_format, finalScore)
        levelsText.text = getString(R.string.matching_levels_format, completedLevels, 10)
        correctActionsText.text = getString(R.string.matching_correct_actions_format, correctActions, correctActions * 10)
        incorrectActionsText.text = getString(R.string.matching_incorrect_actions_format, incorrectActions, incorrectActions * 5)

        // Отзыв в зависимости от результата (локализовано)
        val feedbackRes = when {
            finalScore >= 450 -> R.string.matching_feedback_450
            finalScore >= 350 -> R.string.matching_feedback_350
            finalScore >= 250 -> R.string.matching_feedback_250
            finalScore >= 150 -> R.string.matching_feedback_150
            finalScore >= 50 -> R.string.matching_feedback_50
            else -> R.string.matching_feedback_else
        }
        feedbackText.setText(feedbackRes)

        // Озвучиваем поздравление
        speakCongratulation()

        playAgainButton.setText(R.string.matching_play_again)
        homeButton.setText(R.string.matching_home)

        playAgainButton.setOnClickListener {
            stopTTS()
            val intent = Intent(this, MatchingActivity::class.java)
            startActivity(intent)
            finish()
        }

        homeButton.setOnClickListener {
            stopTTS()
            val intent = Intent(this, MathExercisesActivity::class.java)
            startActivity(intent)
            finish()
        }
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

    private fun speakCongratulation() {
        if (!allowTts || !isTtsReady) return

        // Получаем данные для расчета процента
        val correctActions = intent.getIntExtra("correct_actions", 0)
        val incorrectActions = intent.getIntExtra("incorrect_actions", 0)
        val totalActions = correctActions + incorrectActions

        val percentage = if (totalActions > 0) {
            (correctActions * 100) / totalActions
        } else {
            0
        }

        // Выбираем фразы TTS в зависимости от результата
        val phrasesArrayId = when {
            percentage == 100 -> R.array.matching_results_tts_perfect
            percentage >= 90 -> R.array.matching_results_tts_excellent
            percentage >= 80 -> R.array.matching_results_tts_good
            else -> R.array.matching_results_tts_encouragement
        }

        val phrases = resources.getStringArray(phrasesArrayId)
        val randomPhrase = phrases[Random.nextInt(phrases.size)]
        tts?.speak(randomPhrase, TextToSpeech.QUEUE_FLUSH, null, "matching_results_tts")
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
