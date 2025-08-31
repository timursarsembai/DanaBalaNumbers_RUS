package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
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

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsEnabled = false

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sudoku_kids)

        view = findViewById(R.id.sudokuKidsView)
        levelView = findViewById(R.id.textLevel)
        sizeView = findViewById(R.id.textSize)
        nextButton = findViewById(R.id.buttonNextLevel)
        timerView = findViewById(R.id.timerView)

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }

        // Инициализация TTS
        initTts()

        nextButton.setOnClickListener {
            level++
            view.startNewGame(gridSizeForLevel(level))
            updateUi()
            nextButton.visibility = View.GONE
            restartTimer()
            speakStartForCurrentLevel()
        }

        updateUi()
        view.onSolved = {
            nextButton.visibility = View.VISIBLE
            stopTimer()
            speakSolved()
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
        if (nextButton.visibility != View.VISIBLE) startTimerIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun updateUi() {
        levelView.text = level.toString()
        sizeView.text = getString(R.string.sudoku_kids_size_value_format, view.gridSize, view.gridSize)
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

    private fun initTts() {
        val lang = LocaleManager.getCurrentLanguage(this)
        if (lang == LocaleManager.LANGUAGE_KAZAKH) {
            ttsEnabled = false
            return
        }
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val localeTag = if (lang == LocaleManager.LANGUAGE_RUSSIAN) "ru-RU" else "en-US"
                val res = tts?.setLanguage(Locale.forLanguageTag(localeTag))
                ttsEnabled = when (res) {
                    TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE -> true
                    else -> false
                }
                if (ttsEnabled) speakStartForCurrentLevel()
            } else {
                ttsEnabled = false
            }
        }
    }

    private fun speakStartForCurrentLevel() {
        if (!ttsEnabled) return
        val instr = getString(R.string.sudoku_kids_tts_instruction)
        val size = getString(R.string.sudoku_kids_tts_grid_size_format, view.gridSize, view.gridSize)
        speakQueued(instr, true)
        speakQueued(size, false)
    }

    private fun speakSolved() {
        if (!ttsEnabled) return
        speakQueued(getString(R.string.sudoku_kids_tts_solved), true)
    }

    private fun speakQueued(text: String, flush: Boolean) {
        if (!ttsEnabled || text.isBlank()) return
        val qMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, qMode, null, "sudokuKids")
    }
}
