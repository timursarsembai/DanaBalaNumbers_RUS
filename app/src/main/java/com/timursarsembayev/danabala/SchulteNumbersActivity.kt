package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.timursarsembayev.danabalanumbers.R
import java.util.Locale

class SchulteNumbersActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var timerView: TextView
    private lateinit var targetView: TextView
    private lateinit var gameView: SchulteNumbersView
    private lateinit var progressBar: ProgressBar

    private var startTimeMs: Long = 0L
    private var running = false
    private val handler = Handler(Looper.getMainLooper())

    private var targetDigit = 0

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var allowTts = false

    private val updateTimer = object : Runnable {
        override fun run() {
            if (!running) return
            val elapsed = System.currentTimeMillis() - startTimeMs
            timerView.text = formatTime(elapsed)
            handler.postDelayed(this, 100)
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        // Применяем выбранную локаль приложения
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schulte_numbers)

        timerView = findViewById(R.id.timerView)
        targetView = findViewById(R.id.targetView)
        gameView = findViewById(R.id.schulteGameView)
        progressBar = findViewById(R.id.progressBar)

        // Стандартная кнопка Назад
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        // Прогресс всего 10 целей (0..9)
        progressBar.max = 10
        progressBar.progress = 0

        // Настройка TTS в зависимости от языка
        val lang = LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)
        allowTts = lang != LocaleManager.LANGUAGE_KAZAKH
        if (allowTts) {
            tts = TextToSpeech(this, this)
        }

        gameView.onTileTap = { digit ->
            if (digit == targetDigit) {
                speakPraise()
                nextTargetOrFinish()
            }
        }

        startGame()
    }

    private fun startGame() {
        targetDigit = 0
        updateTargetText()
        gameView.resetGrid(targetDigit)
        startTimeMs = System.currentTimeMillis()
        running = true
        handler.post(updateTimer)
        // Озвучим первое задание
        speakTarget()
    }

    private fun nextTargetOrFinish() {
        if (targetDigit >= 9) {
            // Завершаем и заполняем прогресс на 100%
            progressBar.progress = 10
            finishGame()
        } else {
            targetDigit++
            updateTargetText()
            // Обновляем прогресс после успешного нажатия
            progressBar.progress = targetDigit
            gameView.flipToNext(targetDigit)
            // Озвучим следующее задание
            speakTarget()
        }
    }

    private fun updateTargetText() {
        // Показываем числовую цель в UI по шаблону
        targetView.text = getString(R.string.schulte_target_text_template, targetDigit)
    }

    private fun finishGame() {
        running = false
        handler.removeCallbacks(updateTimer)
        val elapsed = System.currentTimeMillis() - startTimeMs
        val intent = Intent(this, SchulteNumbersResultsActivity::class.java)
        intent.putExtra("elapsed", elapsed)
        startActivity(intent)
        finish()
    }

    private fun speakPraise() {
        if (!allowTts || !ttsReady) return
        // Локализованный массив фраз похвалы
        val phrases = resources.getStringArray(R.array.schulte_praise_phrases)
        val phrase = phrases.random()
        // Не перекрываем последующее задание, добавляем в очередь
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "schulte_praise")
    }

    private fun speakTarget() {
        if (!allowTts || !ttsReady) return
        // Используем шаблон из ресурсов с словесным названием числа
        val numberNameResId = when (targetDigit) {
            0 -> R.string.number_name_0
            1 -> R.string.number_name_1
            2 -> R.string.number_name_2
            3 -> R.string.number_name_3
            4 -> R.string.number_name_4
            5 -> R.string.number_name_5
            6 -> R.string.number_name_6
            7 -> R.string.number_name_7
            8 -> R.string.number_name_8
            else -> R.string.number_name_9
        }
        val numberWord = getString(numberNameResId)
        val text = getString(R.string.find_digit_instruction, numberWord)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "schulte_target_$targetDigit")
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val hund = (ms % 1000) / 10
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", min, sec, hund)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Выбираем язык озвучки по текущей локали
            val locale = when (LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)) {
                LocaleManager.LANGUAGE_RUSSIAN -> Locale.forLanguageTag("ru-RU")
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> null // kk — TTS отключён
            }
            if (locale != null) {
                val res = tts?.setLanguage(locale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                ttsReady = true
                // Озвучим текущее задание при готовности TTS
                speakTarget()
            } else {
                ttsReady = false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        running = false
        handler.removeCallbacks(updateTimer)
        tts?.stop()
    }

    override fun onResume() {
        super.onResume()
        if (progressBar.progress < progressBar.max) {
            running = true
            if (startTimeMs == 0L) startTimeMs = System.currentTimeMillis()
            handler.post(updateTimer)
        }
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
