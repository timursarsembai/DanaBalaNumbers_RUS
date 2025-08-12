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
import java.util.*
import kotlin.random.Random

class AudioMatchingResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_matching_results)

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
            val result = tts?.setLanguage(Locale("ru"))
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED

            if (isTtsReady) {
                speakCongratulation()
            }
        }
    }

    private fun setupViews() {
        val completedLevels = intent.getIntExtra("completed_levels", 10)
        val correctActions = intent.getIntExtra("correct_actions", 0)
        val incorrectActions = intent.getIntExtra("incorrect_actions", 0)
        val finalScore = intent.getIntExtra("final_score", 0)

        // Обновляем UI с результатами
        findViewById<TextView>(R.id.completedLevelsText).text = "Пройдено уровней: $completedLevels"
        findViewById<TextView>(R.id.correctActionsText).text = "Правильных действий: $correctActions"
        findViewById<TextView>(R.id.incorrectActionsText).text = "Ошибок: $incorrectActions"
        findViewById<TextView>(R.id.finalScoreText).text = "Итоговые очки: $finalScore"

        // Определяем сообщение в зависимости от результата
        val accuracy = if (correctActions + incorrectActions > 0) {
            (correctActions * 100) / (correctActions + incorrectActions)
        } else {
            100
        }

        val (message, encouragement) = when {
            accuracy >= 90 -> Pair("Превосходно! 🏆", "Ты отлично слышишь и различаешь числа!")
            accuracy >= 70 -> Pair("Отлично! 🌟", "Ты хорошо сопоставляешь звуки с цифрами!")
            accuracy >= 50 -> Pair("Хорошо! 👍", "Продолжай тренироваться!")
            else -> Pair("Не сдавайся! 💪", "С каждым разом будет лучше!")
        }

        findViewById<TextView>(R.id.resultMessage).text = message
        findViewById<TextView>(R.id.encouragementText).text = encouragement
    }

    private fun speakCongratulation() {
        if (isTtsReady) {
            val correctActions = intent.getIntExtra("correct_actions", 0)
            val incorrectActions = intent.getIntExtra("incorrect_actions", 0)

            val accuracy = if (correctActions + incorrectActions > 0) {
                (correctActions * 100) / (correctActions + incorrectActions)
            } else {
                100
            }

            val phrases = when {
                accuracy == 100 -> DifferentiatedCongratulationPhrases.perfect100Phrases
                accuracy >= 90 -> DifferentiatedCongratulationPhrases.excellent90Phrases
                accuracy >= 80 -> DifferentiatedCongratulationPhrases.good80Phrases
                else -> DifferentiatedCongratulationPhrases.encouragement80Phrases
            }

            val randomPhrase = phrases[Random.nextInt(phrases.size)]
            tts?.speak(randomPhrase, TextToSpeech.QUEUE_FLUSH, null, "congratulation")
        }
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
        tts?.stop()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
