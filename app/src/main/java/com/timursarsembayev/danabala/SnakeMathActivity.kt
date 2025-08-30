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
import android.content.Context

class SnakeMathActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var gameView: SnakeMathGameView
    private lateinit var levelView: TextView
    private lateinit var timerView: TextView
    private var lastLevel: Int = 1

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsEnabled = false

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

    override fun attachBaseContext(newBase: Context) {
        // Применяем выбранный язык ко всем вью этой активности
        super.attachBaseContext(LocaleManager.applyLanguage(newBase))
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
            levelView.text = getString(R.string.snake_math_level_format, level)
            if (level > lastLevel) speak(getString(R.string.snake_math_speak_level, level))
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
        gameView.onCorrectPickup = { digit: Int ->
            val name = getNumberName(digit)
            speak(getString(R.string.snake_math_correct_template, name))
        }
        gameView.onWrongPickup = { _: Int -> speak(getString(R.string.snake_math_wrong)) }

        // Инициализация TTS в зависимости от языка (kk — отключено полностью)
        val lang = LocaleManager.getCurrentLanguage(this)
        ttsEnabled = lang != LocaleManager.LANGUAGE_KAZAKH
        if (ttsEnabled) {
            tts = TextToSpeech(this, this)
        }

        // Инициализация секундомера
        if (startTimeMs == 0L) startTimeMs = System.currentTimeMillis()
        timerView.text = formatTime(0)
        // Инициализация стартового текста уровня
        levelView.text = getString(R.string.snake_math_level_format, 1)
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
        if (!ttsEnabled) return
        if (status == TextToSpeech.SUCCESS) {
            val langCode = LocaleManager.getCurrentLanguage(this)
            val tag = when (langCode) {
                LocaleManager.LANGUAGE_RUSSIAN -> "ru-RU"
                LocaleManager.LANGUAGE_ENGLISH -> "en-US"
                else -> null
            }
            if (tag != null) {
                val res = tts?.setLanguage(Locale.forLanguageTag(tag))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            }
            ttsReady = true
        }
    }

    private fun speak(text: String) {
        if (!ttsEnabled || !ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "snake_say")
    }

    private fun getNumberName(d: Int): String {
        return when (d) {
            0 -> getString(R.string.number_name_0)
            1 -> getString(R.string.number_name_1)
            2 -> getString(R.string.number_name_2)
            3 -> getString(R.string.number_name_3)
            4 -> getString(R.string.number_name_4)
            5 -> getString(R.string.number_name_5)
            6 -> getString(R.string.number_name_6)
            7 -> getString(R.string.number_name_7)
            8 -> getString(R.string.number_name_8)
            9 -> getString(R.string.number_name_9)
            else -> d.toString()
        }
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
