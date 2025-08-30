package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RowErrorResultsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val ctx = LocaleManager.applyLanguage(newBase)
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_row_error_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val total = intent.getIntExtra("TOTAL", 20)
        val correct = intent.getIntExtra("CORRECT", 0)
        val percent = if (total > 0) (correct * 100) / total else 0

        val titleRes = when {
            percent >= 90 -> R.string.row_error_results_title_excellent
            percent >= 70 -> R.string.row_error_results_title_great
            percent >= 50 -> R.string.row_error_results_title_good
            else -> R.string.row_error_results_title_keep_trying
        }

        findViewById<TextView>(R.id.resultTitle).text = getString(titleRes)
        findViewById<TextView>(R.id.scoreText).text = getString(
            R.string.row_error_correct_answers_with_percent_format,
            correct,
            total,
            percent
        )

        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            startActivity(Intent(this, RowErrorActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.backToMenuButton).setOnClickListener {
            startActivity(Intent(this, MathExercisesActivity::class.java))
            finish()
        }
    }
}
