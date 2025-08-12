package com.timursarsembayev.danabalanumbers

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

class DescendingSequenceResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_descending_sequence_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tts = TextToSpeech(this, this)

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

        scoreText.text = "Очки: $score"

        val accuracy = (correctAnswers.toFloat() / totalQuestions * 100).toInt()
        accuracyText.text = "Правильных ответов: $correctAnswers из $totalQuestions ($accuracy%)"
        progressBar.progress = accuracy

        val message = when {
            accuracy >= 90 -> {
                congratsText.text = "🌟 Превосходно! 🌟"
                "Отличная работа! Ты великолепно справился с упражнением по убыванию!"
            }
            accuracy >= 70 -> {
                congratsText.text = "👏 Хорошо! 👏"
                "Хорошая работа! Продолжай тренироваться!"
            }
            accuracy >= 50 -> {
                congratsText.text = "👍 Неплохо! 👍"
                "Неплохой результат! Попробуй еще раз!"
            }
            else -> {
                congratsText.text = "💪 Попробуй еще! 💪"
                "Не расстраивайся! Попробуй еще раз, и у тебя обязательно получится!"
            }
        }

        speakText(message)
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            val intent = Intent(this, DescendingSequenceActivity::class.java)
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
            val result = tts!!.setLanguage(Locale("ru", "RU"))
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
