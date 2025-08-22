package com.timursarsembayev.danabalanumbers

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.timursarsembayev.danabalanumbers.R
import java.util.Locale

class SudokuKidsActivity : AppCompatActivity() {
    private lateinit var view: SudokuKidsView
    private lateinit var levelView: TextView
    private lateinit var sizeView: TextView
    private lateinit var nextButton: MaterialButton
    private lateinit var timerView: TextView

    private var level: Int = 1

    // Таймер
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var startTimeMs = 0L

    private val updateTimer = object : Runnable {
        override fun run() {
            if (!running) return
            val elapsed = System.currentTimeMillis() - startTimeMs
            timerView.text = formatTime(elapsed)
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sudoku_kids)

        view = findViewById<SudokuKidsView>(R.id.sudokuKidsView)
        levelView = findViewById<TextView>(R.id.textLevel)
        sizeView = findViewById<TextView>(R.id.textSize)
        nextButton = findViewById(R.id.buttonNextLevel)
        timerView = findViewById(R.id.timerView)

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }

        nextButton.setOnClickListener {
            // Подтверждение перехода
            level++
            view.startNewGame(gridSizeForLevel(level))
            updateUi()
            nextButton.visibility = View.GONE
            restartTimer()
        }

        updateUi()
        view.onSolved = {
            // Показываем кнопку подтверждения и останавливаем таймер
            nextButton.visibility = View.VISIBLE
            stopTimer()
        }
        // Запуск первого уровня
        view.startNewGame(gridSizeForLevel(level))
        nextButton.visibility = View.GONE
        restartTimer()
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    override fun onResume() {
        super.onResume()
        // Возобновляем таймер, если уровень ещё не решён и кнопка скрыта
        if (nextButton.visibility != View.VISIBLE) startTimerIfNeeded()
    }

    private fun updateUi() {
        levelView.text = level.toString()
        sizeView.text = "${view.gridSize}×${view.gridSize}"
    }

    private fun gridSizeForLevel(lvl: Int): Int {
        val sizes = intArrayOf(3, 4, 6, 8, 9)
        val idx = ((lvl - 1) / 10).coerceAtMost(sizes.lastIndex)
        return sizes[idx]
    }

    private fun restartTimer() {
        startTimeMs = System.currentTimeMillis()
        running = true
        handler.removeCallbacks(updateTimer)
        handler.post(updateTimer)
        timerView.text = formatTime(0)
    }

    private fun stopTimer() {
        running = false
        handler.removeCallbacks(updateTimer)
    }

    private fun startTimerIfNeeded() {
        if (!running) {
            startTimeMs = System.currentTimeMillis()
            running = true
            handler.post(updateTimer)
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val hund = (ms % 1000) / 10
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", min, sec, hund)
    }
}
