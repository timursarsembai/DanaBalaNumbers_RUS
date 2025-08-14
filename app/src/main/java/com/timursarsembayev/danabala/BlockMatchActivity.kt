package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BlockMatchActivity : AppCompatActivity() {
    private lateinit var gameView: BlockMatchGameView
    private lateinit var scoreView: TextView
    private lateinit var levelView: TextView
    private var lastScore: Int = 0
    private var defeatDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_match)

        scoreView = findViewById<TextView>(R.id.textScore)
        levelView = findViewById<TextView>(R.id.textLevel)
        gameView = findViewById<BlockMatchGameView>(R.id.blockMatchGameView)

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }

        gameView.onScoreLevelChanged = { score: Int, level: Int ->
            lastScore = score
            scoreView.text = score.toString()
            levelView.text = level.toString()
        }
        gameView.onGameOver = fun() {
            if (defeatDialogShown || isFinishing || isDestroyed) return
            defeatDialogShown = true
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    val intent = Intent(this, BlockMatchResultsActivity::class.java)
                    intent.putExtra("score", lastScore)
                    startActivity(intent)
                    finish()
                } else {
                    defeatDialogShown = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
}
