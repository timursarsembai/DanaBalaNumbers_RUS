package com.example.danabala

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.random.Random

class MatchingResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matching_results)

        initTTS()

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

        // Устанавливаем результаты
        scoreText.text = "Итоговые очки: $finalScore"
        levelsText.text = "Пройдено уровней: $completedLevels из 10"
        correctActionsText.text = "✅ Правильных действий: $correctActions (+${correctActions * 10} очков)"
        incorrectActionsText.text = "❌ Ошибок: $incorrectActions (-${incorrectActions * 5} очков)"

        // Определяем отзыв в зависимости от результата
        val feedback = when {
            finalScore >= 450 -> "Превосходно! Ты настоящий мастер! 🌟"
            finalScore >= 350 -> "Отлично! Великолепная работа! 👏"
            finalScore >= 250 -> "Хорошо! Ты хорошо справился! 👍"
            finalScore >= 150 -> "Неплохо! Продолжай практиковаться! 📚"
            finalScore >= 50 -> "Попробуй еще раз! У тебя получится! 💪"
            else -> "Не расстраивайся! Попробуй снова! 🎯"
        }
        feedbackText.text = feedback

        // Озвучиваем поздравление
        speakCongratulation()

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

    private fun initTTS() {
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("ru"))
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED

            // Если TTS готов и это первый запуск, озвучиваем поздравление
            if (isTtsReady) {
                speakCongratulation()
            }
        }
    }

    private fun speakCongratulation() {
        if (isTtsReady) {
            // Получаем данные для расчета процента
            val correctActions = intent.getIntExtra("correct_actions", 0)
            val incorrectActions = intent.getIntExtra("incorrect_actions", 0)
            val totalActions = correctActions + incorrectActions

            val percentage = if (totalActions > 0) {
                (correctActions * 100) / totalActions
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
            tts.speak(randomPhrase, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun stopTTS() {
        if (::tts.isInitialized) {
            tts.stop()
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
