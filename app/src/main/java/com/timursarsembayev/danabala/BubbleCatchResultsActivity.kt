package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class BubbleCatchResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var totalScore: Int = 0
    private var isGameOver: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bubble_catch_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        totalScore = intent.getIntExtra("SCORE", 0)
        isGameOver = intent.getBooleanExtra("GAME_OVER", false)
        tts = TextToSpeech(this, this)

        setupViews()
        setupButtons()
    }

    private fun setupViews() {
        val emojiText = findViewById<TextView>(R.id.emojiText)
        val titleText = findViewById<TextView>(R.id.messageTitleText)
        val scoreTv = findViewById<TextView>(R.id.scoreText)
        val gameOverMsg = findViewById<TextView>(R.id.gameOverMessageText)

        scoreTv.text = totalScore.toString()

        if (isGameOver) {
            // UI для проигрыша
            emojiText.text = "😔"
            titleText.text = "Игра окончена"
            gameOverMsg.apply {
                text = listOf(
                    "Ты проиграл. Ничего страшного — попробуй ещё раз!",
                    "Сегодня не повезло, но завтра будет лучше!",
                    "Не сдавайся! С каждой попыткой ты становишься сильнее!",
                    "Отличная тренировка! Давай ещё раз и у тебя получится!"
                ).random()
                visibility = android.view.View.VISIBLE
            }
        } else {
            // Обычный итог
            emojiText.text = getString(R.string.celebration_emoji)
            titleText.text = getString(R.string.your_result)
            gameOverMsg.visibility = android.view.View.GONE
        }
    }

    private fun setupButtons() {
        findViewById<CardView>(R.id.btnPlayAgain).setOnClickListener {
            stopTTS()
            startActivity(Intent(this, BubbleCatchActivity::class.java))
            finish()
        }
        findViewById<CardView>(R.id.btnBackToMenu).setOnClickListener {
            stopTTS()
            startActivity(Intent(this, MathExercisesActivity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            stopTTS()
            finish()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.forLanguageTag("ru-RU")
            if (isGameOver) speakGameOver() else speakCongrats()
        }
    }

    private fun speakCongrats() {
        // Простейшая дифференциация фраз по порогам очков
        val phrase = when {
            totalScore >= 150 -> DifferentiatedCongratulationPhrases.excellent90Phrases.random()
            totalScore >= 80 -> DifferentiatedCongratulationPhrases.good80Phrases.random()
            else -> DifferentiatedCongratulationPhrases.encouragement80Phrases.random()
        }
        val text = "$phrase Ты набрал $totalScore баллов!"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bubble_catch_result")
    }

    private fun speakGameOver() {
        val phrase = DifferentiatedCongratulationPhrases.encouragement80Phrases.random()
        val text = "Игра окончена. $phrase Попробуй ещё раз!"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bubble_catch_game_over")
    }

    private fun stopTTS() { tts?.stop() }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
