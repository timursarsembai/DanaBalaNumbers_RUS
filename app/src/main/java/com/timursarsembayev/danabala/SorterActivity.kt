// filepath: /Users/timursarsembai/AndroidStudioProjects/DanaBalaNumbers_RUS/app/src/main/java/com/timursarsembayev/danabala/SorterActivity.kt
package com.timursarsembayev.danabalanumbers

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class SorterActivity : AppCompatActivity() {
    private lateinit var gameView: SorterGameView
    private lateinit var movesView: TextView
    private lateinit var levelView: TextView
    private lateinit var timerView: TextView
    private var lastMoves = 0

    // Секундомер уровня
    private val handler = Handler(Looper.getMainLooper())
    private var startTimeMs: Long = 0L
    private var timerRunning = false
    private val updateTimer = object : Runnable {
        override fun run() {
            if (!timerRunning) return
            val elapsed = System.currentTimeMillis() - startTimeMs
            timerView.text = formatTime(elapsed)
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sorter)

        movesView = findViewById(R.id.textMoves)
        levelView = findViewById(R.id.textLevel)
        timerView = findViewById(R.id.textTimer)
        gameView = findViewById(R.id.sorterGameView)

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.buttonRestart)?.setOnClickListener {
            gameView.resetAll()
            resetTimer()
        }

        gameView.onMovesChanged = { m ->
            lastMoves = m
            movesView.text = m.toString()
        }
        gameView.onRoundChanged = { r, _ ->
            levelView.text = r.toString()
            resetTimer()
        }
        gameView.onRoundCompleted = { _, _ -> /* следующий раунд запускается автоматически */ }

        // Инициализация таймера на 0 при первом запуске
        timerView.text = formatTime(0)
    }

    private fun resetTimer() {
        startTimeMs = System.currentTimeMillis()
        timerView.text = formatTime(0)
        // если на экране — продолжаем бежать
        if (hasWindowFocus()) {
            timerRunning = true
            handler.removeCallbacks(updateTimer)
            handler.post(updateTimer)
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val tenth = (ms % 1000) / 100
        return String.format(Locale.getDefault(), "%02d:%02d.%d", min, sec, tenth)
    }

    override fun onResume() {
        super.onResume()
        timerRunning = true
        if (startTimeMs == 0L) startTimeMs = System.currentTimeMillis()
        handler.post(updateTimer)
    }

    override fun onPause() {
        super.onPause()
        timerRunning = false
        handler.removeCallbacks(updateTimer)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateTimer)
        super.onDestroy()
    }
}
