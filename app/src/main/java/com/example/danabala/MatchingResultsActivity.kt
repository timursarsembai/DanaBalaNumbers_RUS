package com.example.danabala

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MatchingResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matching_results)

        val totalScore = intent.getIntExtra("total_score", 0)
        val completedLevels = intent.getIntExtra("completed_levels", 0)

        val titleText = findViewById<TextView>(R.id.titleText)
        val scoreText = findViewById<TextView>(R.id.scoreText)
        val levelsText = findViewById<TextView>(R.id.levelsText)
        val feedbackText = findViewById<TextView>(R.id.feedbackText)
        val playAgainButton = findViewById<Button>(R.id.playAgainButton)
        val homeButton = findViewById<Button>(R.id.homeButton)

        // Устанавливаем результаты
        scoreText.text = "Очки: $totalScore"
        levelsText.text = "Пройдено уровней: $completedLevels из 10"

        // Определяем отзыв в зависимости от результата
        val feedback = when {
            completedLevels == 10 -> "Отлично! Все уровни пройдены! 🌟"
            completedLevels >= 7 -> "Хорошая работа! 👍"
            completedLevels >= 5 -> "Неплохо! Продолжай практиковаться! 📚"
            else -> "Попробуй еще раз! 💪"
        }
        feedbackText.text = feedback

        playAgainButton.setOnClickListener {
            val intent = Intent(this, MatchingActivity::class.java)
            startActivity(intent)
            finish()
        }

        homeButton.setOnClickListener {
            val intent = Intent(this, MathExercisesActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
