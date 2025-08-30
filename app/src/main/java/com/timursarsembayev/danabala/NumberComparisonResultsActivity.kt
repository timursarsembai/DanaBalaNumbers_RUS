package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class NumberComparisonResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    // Фразы для TTS берем из ресурсов
    private val excellentPhrases by lazy { resources.getStringArray(R.array.number_comparison_results_excellent_phrases).toList() }
    private val goodPhrases by lazy { resources.getStringArray(R.array.number_comparison_results_good_phrases).toList() }
    private val okayPhrases by lazy { resources.getStringArray(R.array.number_comparison_results_okay_phrases).toList() }
    private val encouragementPhrases by lazy { resources.getStringArray(R.array.number_comparison_results_encouragement_phrases).toList() }

    override fun attachBaseContext(newBase: Context) {
        val ctx = LocaleManager.applyLanguage(newBase)
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Принудительно устанавливаем альбомную ориентацию
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContentView(R.layout.activity_number_comparison_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализируем TTS только для ru/en; для kk — отключаем полностью
        val lang = LocaleManager.getCurrentLanguage(this)
        if (lang != LocaleManager.LANGUAGE_KAZAKH) {
            tts = TextToSpeech(this, this)
        }

        setupViews()
    }

    private fun setupViews() {
        val score = intent.getIntExtra("score", 0)
        val totalCorrectAnswers = intent.getIntExtra("totalCorrectAnswers", 0)
        val totalQuestions = intent.getIntExtra("totalQuestions", 20)
        val fromKidsComparison = intent.getBooleanExtra("fromKidsComparison", false)

        val scoreDisplay = findViewById<TextView>(R.id.scoreDisplay)
        val correctAnswersDisplay = findViewById<TextView>(R.id.correctAnswersDisplay)
        val accuracyDisplay = findViewById<TextView>(R.id.accuracyDisplay)
        val messageDisplay = findViewById<TextView>(R.id.messageDisplay)
        val motivationalMessage = findViewById<TextView>(R.id.motivationalMessage)
        val performanceBadge = findViewById<TextView>(R.id.performanceBadge)
        val congratulationsIcon = findViewById<TextView>(R.id.congratulationsIcon)
        val achievementStars = findViewById<LinearLayout>(R.id.achievementStars)

        scoreDisplay.text = getString(R.string.number_comparison_score_points, score)
        correctAnswersDisplay.text = getString(R.string.number_comparison_correct_answers_format, totalCorrectAnswers, totalQuestions)

        val accuracy = if (totalQuestions > 0) {
            (totalCorrectAnswers.toFloat() / totalQuestions * 100).toInt()
        } else 0

        accuracyDisplay.text = getString(R.string.number_comparison_accuracy_format, accuracy)

        // Настройка сообщений и внешнего вида в зависимости от результата
        when {
            accuracy >= 90 -> {
                messageDisplay.text = getString(R.string.number_comparison_results_title_excellent)
                motivationalMessage.text = getString(R.string.number_comparison_results_message_excellent)
                performanceBadge.text = getString(R.string.number_comparison_results_badge_excellent)
                congratulationsIcon.text = "🎉"
                createStars(achievementStars, 5)
            }
            accuracy >= 75 -> {
                messageDisplay.text = getString(R.string.number_comparison_results_title_good)
                motivationalMessage.text = getString(R.string.number_comparison_results_message_good)
                performanceBadge.text = getString(R.string.number_comparison_results_badge_good)
                congratulationsIcon.text = "😊"
                createStars(achievementStars, 4)
            }
            accuracy >= 50 -> {
                messageDisplay.text = getString(R.string.number_comparison_results_title_okay)
                motivationalMessage.text = getString(R.string.number_comparison_results_message_okay)
                performanceBadge.text = getString(R.string.number_comparison_results_badge_okay)
                congratulationsIcon.text = "🙂"
                createStars(achievementStars, 3)
            }
            else -> {
                messageDisplay.text = getString(R.string.number_comparison_results_title_try_again)
                motivationalMessage.text = getString(R.string.number_comparison_results_message_try_again)
                performanceBadge.text = getString(R.string.number_comparison_results_badge_try_again)
                congratulationsIcon.text = "🤗"
                createStars(achievementStars, 2)
            }
        }

        // Кнопки
        findViewById<Button>(R.id.restartButton).setOnClickListener {
            val intent = Intent(this, if (fromKidsComparison) KidsComparisonActivity::class.java else NumberComparisonActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.homeButton).setOnClickListener {
            val intent = Intent(this, MathExercisesActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun createStars(container: LinearLayout, count: Int) {
        container.removeAllViews()

        for (i in 1..5) {
            val star = TextView(this).apply {
                text = if (i <= count) "⭐" else "☆"
                textSize = 24f
                setPadding(4, 0, 4, 0)
            }
            container.addView(star)
        }
    }

    private fun speakCongratulations(accuracy: Int) {
        val congratulationPhrase = when {
            accuracy >= 90 -> excellentPhrases.random()
            accuracy >= 75 -> goodPhrases.random()
            accuracy >= 50 -> okayPhrases.random()
            else -> encouragementPhrases.random()
        }

        speakText(congratulationPhrase)
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val lang = LocaleManager.getCurrentLanguage(this)
            val locale = when (lang) {
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> Locale.forLanguageTag("ru-RU")
            }
            val result = tts!!.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts!!.setLanguage(Locale.getDefault())
            }

            // Озвучиваем поздравления через секунду после инициализации TTS
            val accuracy = if (intent.getIntExtra("totalQuestions", 20) > 0) {
                (intent.getIntExtra("totalCorrectAnswers", 0).toFloat() / intent.getIntExtra("totalQuestions", 20) * 100).toInt()
            } else 0

            // Небольшая задержка для лучшего восприятия
            android.os.Handler(mainLooper).postDelayed({
                speakCongratulations(accuracy)
            }, 1000)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
