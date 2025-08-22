package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.view.HapticFeedbackConstants
import android.view.View
import android.os.Handler
import android.os.Looper

class SnakeMathActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var gameView: SnakeMathGameView
    private lateinit var levelView: TextView
    private lateinit var timerView: TextView
    private var lastLevel: Int = 1

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // Секундомер
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
        setContentView(R.layout.activity_snake_math)

        levelView = findViewById(R.id.textLevel)
        timerView = findViewById(R.id.timerView)
        gameView = findViewById(R.id.snakeMathGameView)

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }

        // Виртуальный джойстик с тактильной отдачей (одношаговое движение)
        val btnUp = findViewById<Button>(R.id.btnUp).apply { isHapticFeedbackEnabled = true }
        val btnDown = findViewById<Button>(R.id.btnDown).apply { isHapticFeedbackEnabled = true }
        val btnLeft = findViewById<Button>(R.id.btnLeft).apply { isHapticFeedbackEnabled = true }
        val btnRight = findViewById<Button>(R.id.btnRight).apply { isHapticFeedbackEnabled = true }

        btnUp?.setOnClickListener { v -> hapticTap(v); gameView.moveOnceUp() }
        btnDown?.setOnClickListener { v -> hapticTap(v); gameView.moveOnceDown() }
        btnLeft?.setOnClickListener { v -> hapticTap(v); gameView.moveOnceLeft() }
        btnRight?.setOnClickListener { v -> hapticTap(v); gameView.moveOnceRight() }

        gameView.onLevelChanged = { level: Int ->
            levelView.text = "Уровень $level"
            if (level > lastLevel) speak("Уровень $level")
            lastLevel = level
        }
        gameView.onGameOver = { levelsCompleted: Int ->
            val intent = Intent(this, SnakeMathResultsActivity::class.java)
            intent.putExtra("levels", levelsCompleted)
            intent.putExtra("won", false)
            intent.putExtra("level", lastLevel)
            startActivity(intent)
            finish()
        }
        gameView.onWin = { levelsCompleted: Int ->
            val intent = Intent(this, SnakeMathResultsActivity::class.java)
            intent.putExtra("levels", levelsCompleted)
            intent.putExtra("won", true)
            intent.putExtra("level", lastLevel)
            startActivity(intent)
            finish()
        }
        gameView.onCorrectPickup = { digit: Int -> speak("Правильно: $digit") }
        gameView.onWrongPickup = { _: Int -> speak("Не та цифра") }

        tts = TextToSpeech(this, this)

        // Инициализация секундомера
        if (startTimeMs == 0L) startTimeMs = System.currentTimeMillis()
        timerView.text = formatTime(0)
    }

    private fun hapticTap(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val tenth = (ms % 1000) / 100
        return String.format(Locale.getDefault(), "%02d:%02d.%d", min, sec, tenth)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale("ru", "RU"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
            ttsReady = true
        }
    }

    private fun speak(text: String) {
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "snake_say")
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
        timerRunning = true
        if (startTimeMs == 0L) startTimeMs = System.currentTimeMillis()
        handler.post(updateTimer)
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
        tts?.stop()
        timerRunning = false
        handler.removeCallbacks(updateTimer)
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); tts = null
        handler.removeCallbacks(updateTimer)
        super.onDestroy()
    }
}
