package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class NumberComparisonResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    // Варианты поздравлений для разных уровней успеха
    private val excellentPhrases = listOf(
        "Поздравляю! Ты настоящий чемпион по сравнению чисел!",
        "Невероятно! Ты отлично знаешь, какие числа больше, а какие меньше!",
        "Браво! Ты справился просто великолепно!",
        "Супер! Ты становишься настоящим математиком!"
    )

    private val goodPhrases = listOf(
        "Молодец! Ты хорошо понимаешь сравнение чисел!",
        "Отлично! Продолжай в том же духе!",
        "Здорово! Ты делаешь большие успехи!",
        "Умница! Ты очень хорошо справился!"
    )

    private val okayPhrases = listOf(
        "Неплохо! Продолжай тренироваться, и у тебя все получится!",
        "Хорошая попытка! Ты на правильном пути!",
        "Так держать! С каждым разом у тебя получается лучше!",
        "Молодец, что стараешься! Продолжай изучать числа!"
    )

    private val encouragementPhrases = listOf(
        "Не расстраивайся! Все учатся постепенно. Попробуй еще раз!",
        "Ничего страшного! Каждый математик начинал с простых заданий!",
        "Не сдавайся! Ты обязательно научишься сравнивать числа!",
        "Попробуй снова! У тебя все получится!"
    )

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

        tts = TextToSpeech(this, this)
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

        scoreDisplay.text = "Очки: $score"
        correctAnswersDisplay.text = "Правильных ответов: $totalCorrectAnswers из $totalQuestions"

        val accuracy = if (totalQuestions > 0) {
            (totalCorrectAnswers.toFloat() / totalQuestions * 100).toInt()
        } else 0

        accuracyDisplay.text = "Точность: $accuracy%"

        // Настройка сообщений и внешнего вида в зависимости от результата
        when {
            accuracy >= 90 -> {
                messageDisplay.text = "🌟 Поздравляем! Ты чемпион!"
                motivationalMessage.text = "Ты отлично знаешь, как сравнивать числа! Продолжай изучать математику!"
                performanceBadge.text = "🏆 ПРЕВОСХОДНО!"
                congratulationsIcon.text = "🎉"
                createStars(achievementStars, 5)
            }
            accuracy >= 75 -> {
                messageDisplay.text = "👍 Отлично! Очень хорошо!"
                motivationalMessage.text = "Ты хорошо понимаешь сравнение чисел! Так держать!"
                performanceBadge.text = "🌟 ОТЛИЧНО!"
                congratulationsIcon.text = "😊"
                createStars(achievementStars, 4)
            }
            accuracy >= 50 -> {
                messageDisplay.text = "📈 Хорошая попытка!"
                motivationalMessage.text = "Ты на правильном пути! Продолжай тренироваться!"
                performanceBadge.text = "👍 ХОРОШО!"
                congratulationsIcon.text = "🙂"
                createStars(achievementStars, 3)
            }
            else -> {
                messageDisplay.text = "💪 Попробуй еще раз!"
                motivationalMessage.text = "Не расстраивайся! Все учатся постепенно. Ты обязательно справишься!"
                performanceBadge.text = "💪 СТАРАЙСЯ!"
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
            val result = tts!!.setLanguage(Locale("ru", "RU"))
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
