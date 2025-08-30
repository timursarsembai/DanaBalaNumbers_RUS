// filepath: /Users/timursarsembai/AndroidStudioProjects/DanaBalaNumbers_RUS/app/src/main/java/com/timursarsembayev/danabala/SorterActivity.kt
package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
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

    // TTS
    private var tts: TextToSpeech? = null
    private lateinit var languageCode: String

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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sorter)

        // Текущий язык приложения
        languageCode = LocaleManager.getCurrentLanguage(this)

        movesView = findViewById(R.id.textMoves)
        levelView = findViewById(R.id.textLevel)
        timerView = findViewById(R.id.textTimer)
        gameView = findViewById(R.id.sorterGameView)

        // Инициализируем стартовые значения до первых колбеков
        levelView.text = "1"
        movesView.text = "0"

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.buttonRestart)?.setOnClickListener {
            gameView.resetAll()
            resetTimer()
        }

        gameView.onMovesChanged = { m ->
            lastMoves = m
            movesView.text = m.toString()
        }
        gameView.onRoundChanged = { r, targets ->
            levelView.text = r.toString()
            resetTimer()
            speakRoundTargets(targets)
        }
        gameView.onRoundCompleted = { _, _ -> /* следующий раунд запускается автоматически */ }

        // Инициализация таймера на 0 при первом запуске
        timerView.text = formatTime(0)

        initTtsIfNeeded()
    }

    private fun initTtsIfNeeded() {
        if (languageCode == LocaleManager.LANGUAGE_KAZAKH) return // kk: озвучка отключена
        if (tts != null) return
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = when (languageCode) {
                    LocaleManager.LANGUAGE_RUSSIAN -> Locale.forLanguageTag("ru-RU")
                    LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                    else -> null
                }
                if (locale != null) {
                    @Suppress("DEPRECATION")
                    tts?.language = locale
                }
            }
        }
    }

    private fun speakRoundTargets(targets: IntArray) {
        val engine = tts ?: return
        if (languageCode == LocaleManager.LANGUAGE_KAZAKH) return
        if (targets.isEmpty()) return
        val prefix = getString(R.string.sorter_speak_instruction_prefix)
        val names = targets.map { d ->
            val resId = resources.getIdentifier("number_name_" + d, "string", packageName)
            if (resId != 0) getString(resId) else d.toString()
        }
        val text = "$prefix ${names.joinToString(separator = ", ")}".trim()
        @Suppress("DEPRECATION")
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sorter_round")
    }

    private fun resetTimer() {
        startTimeMs = System.currentTimeMillis()
        timerView.text = formatTime(0)
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
        tts?.stop()
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateTimer)
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
