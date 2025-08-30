package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SorterResultsActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sorter_results)

        val moves = intent.getIntExtra("moves", 0)
        findViewById<TextView>(R.id.congratsText).text = getString(R.string.results_congratulations)
        findViewById<TextView>(R.id.movesText).text = getString(R.string.sorter_moves_count_format, moves)

        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            startActivity(Intent(this, SorterActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.backToMenuButton).setOnClickListener {
            startActivity(Intent(this, MathExercisesActivity::class.java))
            finish()
        }
    }
}
