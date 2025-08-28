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

class  NumberRecognitionResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_recognition_results)

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
            val locale = Locale.getDefault()
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale("ru")
            }
            isTtsReady = true

            // Если TTS готов, озвучиваем поздравление
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
        findViewById<TextView>(R.id.scoreText).text = getString(R.string.results_score_of_total, score, totalQuestions)
        findViewById<TextView>(R.id.correctAnswersText).text = getString(R.string.results_correct_answers_only, totalCorrect)

        // Определяем сообщение в зависимости от результата
        val percentage = (score * 100) / totalQuestions
        val (message, encouragement) = when {
            percentage >= 90 -> Pair("Превосходно! 🏆", "Ты настоящий мастер цифр!")
            percentage >= 70 -> Pair("Отлично! 🌟", "Ты очень хорошо знаешь цифры!")
            percentage >= 50 -> Pair("Хорошо! 👍", "Продолжай тренироваться!")
            else -> Pair("Не сдавайся! 💪", "С каждым разом будет лучше!")
        }

        findViewById<TextView>(R.id.resultMessage).text = message
        findViewById<TextView>(R.id.encouragementText).text = encouragement
    }

    private fun speakCongratulation() {
        if (isTtsReady) {
            // Получаем данные для расчета процента
            val score = intent.getIntExtra("SCORE", 0)
            val totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 20)

            val percentage = if (totalQuestions > 0) {
                (score * 100) / totalQuestions
            } else {
                0
            }

            // Выбираем фразы в зависимости от результата
            val phrases = when {
                percentage == 100 -> DifferentiatedCongratulationPhrases.perfect100Phrases
                percentage >= 90 -> DifferentiatedCongratulationPhrases.excellent90Phrases
                percentage >= 80 -> DifferentiatedCongratulationPhrases.good80Phrases
                else -> DifferentiatedCongratulationPhrases.encouragement80Phrases
            }

            val randomPhrase = phrases[Random.nextInt(phrases.size)]
            tts?.speak(randomPhrase, TextToSpeech.QUEUE_FLUSH, null, "congratulation")
        }
    }

    private fun setupButtons() {
        findViewById<CardView>(R.id.btnPlayAgain).setOnClickListener {
            stopTTS()
            val intent = Intent(this, NumberRecognitionActivity::class.java)
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
