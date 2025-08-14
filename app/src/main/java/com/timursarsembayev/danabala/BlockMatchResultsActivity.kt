package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.timursarsembayev.danabalanumbers.R

class BlockMatchResultsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_match_results)

        val score = intent.getIntExtra("score", 0)

        val title = findViewById<TextView>(R.id.congratsText)
        val message = findViewById<TextView>(R.id.scoreText)
        title.text = getString(R.string.defeat_title)
        message.text = getString(R.string.defeat_message, score)

        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            startActivity(Intent(this, BlockMatchActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.backToMenuButton).setOnClickListener {
            startActivity(Intent(this, MathExercisesActivity::class.java))
            finish()
        }
    }
}
