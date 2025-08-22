package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.timursarsembayev.danabalanumbers.R

class SorterResultsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sorter_results)

        val moves = intent.getIntExtra("moves", 0)
        findViewById<TextView>(R.id.congratsText).text = "Молодец! Ты всё разложил(а)!"
        findViewById<TextView>(R.id.movesText).text = "Количество ходов: $moves"

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
