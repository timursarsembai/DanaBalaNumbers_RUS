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
import kotlin.random.Random

class ObjectCountingResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var allowTts: Boolean = true

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_counting_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Определяем локаль и отключаем TTS для казахского
        val currentLang = LocaleManager.getCurrentLanguage(this)
        allowTts = currentLang.lowercase(Locale.ROOT) != LocaleManager.LANGUAGE_KAZAKH

        if (allowTts) {
            tts = TextToSpeech(this, this)
        }

        setupViews()
        setupButtons()
    }

    override fun onInit(status: Int) {
        if (!allowTts) return
        if (status == TextToSpeech.SUCCESS) {
            val langCode = LocaleManager.getCurrentLanguage(this)
            val locale = when (langCode) {
                LocaleManager.LANGUAGE_RUSSIAN -> Locale.forLanguageTag("ru-RU")
                LocaleManager.LANGUAGE_KAZAKH -> Locale.forLanguageTag("kk-KZ")
                else -> Locale.forLanguageTag("en-US")
            }
            val result = tts?.setLanguage(locale)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED

            if (isTtsReady) {
                speakCongratulation()
            }
        }
    }

    private fun setupViews() {
        // Получаем результаты из Intent
        val score = intent.getIntExtra("SCORE", 0)
        val totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 20)
        val totalCorrect = intent.getIntExtra("TOTAL_CORRECT", 0)

        // Обновляем UI с результатами
        findViewById<TextView>(R.id.scoreText).text = getString(R.string.score_of_format, score, totalQuestions)
        findViewById<TextView>(R.id.correctAnswersText).text = getString(R.string.correct_answers_format, totalCorrect)

        // Определяем сообщение в зависимости от результата (переиспользуем строки counting)
        val percentage = (score * 100) / totalQuestions
        val (messageRes, encouragementRes) = when {
            percentage >= 90 -> Pair(R.string.counting_result_title_perfect, R.string.counting_result_encouragement_perfect)
            percentage >= 70 -> Pair(R.string.counting_result_title_excellent, R.string.counting_result_encouragement_excellent)
            percentage >= 50 -> Pair(R.string.counting_result_title_good, R.string.counting_result_encouragement_good)
            else -> Pair(R.string.counting_result_title_try_again, R.string.counting_result_encouragement_try_again)
        }

        findViewById<TextView>(R.id.resultMessage).setText(messageRes)
        findViewById<TextView>(R.id.encouragementText).setText(encouragementRes)
    }

    private fun speakCongratulation() {
        if (!allowTts || !isTtsReady) return

        val score = intent.getIntExtra("SCORE", 0)
        val totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 20)

        val percentage = if (totalQuestions > 0) {
            (score * 100) / totalQuestions
        } else {
            0
        }

        // Переиспользуем локализованные массивы из counting
        val phrasesArrayId = when {
            percentage == 100 -> R.array.counting_results_phrases_perfect
            percentage >= 90 -> R.array.counting_results_phrases_excellent
            percentage >= 80 -> R.array.counting_results_phrases_good
            else -> R.array.counting_results_phrases_encouragement
        }

        val phrases = resources.getStringArray(phrasesArrayId)
        val randomPhrase = phrases[Random.nextInt(phrases.size)]
        tts?.speak(randomPhrase, TextToSpeech.QUEUE_FLUSH, null, "congratulation")
    }

    private fun setupButtons() {
        findViewById<CardView>(R.id.btnPlayAgain).setOnClickListener {
            stopTTS()
            val intent = Intent(this, ObjectCountingActivity::class.java)
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
