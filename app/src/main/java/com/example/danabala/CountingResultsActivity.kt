package com.example.danabala

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

class CountingResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counting_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация TTS
        tts = TextToSpeech(this, this)

        setupViews()
        setupButtons()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.forLanguageTag("ru-RU")
        }
    }

    private fun setupViews() {
        // Получаем результаты из Intent
        val score = intent.getIntExtra("SCORE", 0)
        val totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 20)
        val totalCorrect = intent.getIntExtra("TOTAL_CORRECT", 0)

        // Обновляем UI с результатами
        findViewById<TextView>(R.id.scoreText).text = "$score из $totalQuestions"
        findViewById<TextView>(R.id.correctAnswersText).text = "Правильных ответов: $totalCorrect"

        // Определяем сообщение в зависимости от результата
        val percentage = (score * 100) / totalQuestions
        val (message, encouragement) = when {
            percentage >= 90 -> Pair("Превосходно! 🏆", "Ты отлично считаешь предметы!")
            percentage >= 70 -> Pair("Отлично! 🌟", "Ты хорошо различаешь количества!")
            percentage >= 50 -> Pair("Хорошо! 👍", "Продолжай учиться считать!")
            else -> Pair("Не сдавайся! 💪", "Счет станет легче с практикой!")
        }

        findViewById<TextView>(R.id.resultMessage).text = message
        findViewById<TextView>(R.id.encouragementText).text = encouragement

        // Озвучиваем результат
        val speechText = "$message $encouragement"
        tts?.speak(speechText, TextToSpeech.QUEUE_ADD, null, "result")
    }

    private fun setupButtons() {
        findViewById<CardView>(R.id.btnPlayAgain).setOnClickListener {
            val intent = Intent(this, CountingActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<CardView>(R.id.btnBackToMenu).setOnClickListener {
            val intent = Intent(this, MathExercisesActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
