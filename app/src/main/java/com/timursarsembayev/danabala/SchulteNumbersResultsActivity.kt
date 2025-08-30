package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.util.Locale

class SchulteNumbersResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var elapsedMs: Long = 0L
    private var allowTts: Boolean = false

    override fun attachBaseContext(newBase: Context?) {
        // Применяем выбранную локаль приложения
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schulte_numbers_results)

        elapsedMs = intent.getLongExtra("elapsed", 0L)
        findViewById<TextView>(R.id.timeText).text = formatTime(elapsedMs)

        // Настройка TTS в зависимости от языка: для kk — отключено полностью
        val lang = LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)
        allowTts = lang != LocaleManager.LANGUAGE_KAZAKH
        if (allowTts) {
            tts = TextToSpeech(this, this)
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<CardView>(R.id.btnPlayAgain).setOnClickListener {
            tts?.stop()
            startActivity(Intent(this, SchulteNumbersActivity::class.java))
            finish()
        }
        findViewById<CardView>(R.id.btnBackToMenu).setOnClickListener {
            tts?.stop()
            val i = Intent(this, MathExercisesActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(i)
            finish()
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val hund = (ms % 1000) / 10
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", min, sec, hund)
    }

    // Русская форма времени для TTS
    private fun buildTimeSpeechRu(ms: Long): String {
        fun pluralRu(n: Long, forms: Triple<String, String, String>): String {
            val nAbs = (n % 100).toInt()
            val last = nAbs % 10
            return when {
                nAbs in 11..14 -> forms.third
                last == 1 -> forms.first
                last in 2..4 -> forms.second
                else -> forms.third
            }
        }
        val totalSec = ms / 1000
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        val minForm = pluralRu(minutes, Triple("минута", "минуты", "минут"))
        val secForm = pluralRu(seconds, Triple("секунда", "секунды", "секунд"))
        return when {
            minutes > 0 && seconds > 0 -> "за $minutes $minForm и $seconds $secForm"
            minutes > 0 -> "за $minutes $minForm"
            else -> "за $seconds $secForm"
        }
    }

    // Английская форма времени по ресурсам
    private fun buildTimeSpeechEn(ms: Long): String {
        val totalSec = ms / 1000
        val minutes = (totalSec / 60).toInt()
        val seconds = (totalSec % 60).toInt()
        val minuteWord = if (minutes == 1) getString(R.string.schulte_tts_minute_one) else getString(R.string.schulte_tts_minute_other)
        val secondWord = if (seconds == 1) getString(R.string.schulte_tts_second_one) else getString(R.string.schulte_tts_second_other)
        return when {
            minutes > 0 && seconds > 0 -> getString(R.string.schulte_tts_time_mins_secs, minutes, minuteWord, seconds, secondWord)
            minutes > 0 -> getString(R.string.schulte_tts_time_only_mins, minutes, minuteWord)
            else -> getString(R.string.schulte_tts_time_only_secs, seconds, secondWord)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && allowTts) {
            val current = LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)
            val locale = when (current) {
                LocaleManager.LANGUAGE_RUSSIAN -> Locale.forLanguageTag("ru-RU")
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> null
            }
            if (locale != null) {
                val res = tts?.setLanguage(locale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                // Фразы похвалы из ресурсов
                val phrases = resources.getStringArray(R.array.schulte_results_tts_phrases)
                val prefix = phrases.random()
                val timePart = when (current) {
                    LocaleManager.LANGUAGE_ENGLISH -> buildTimeSpeechEn(elapsedMs)
                    else -> buildTimeSpeechRu(elapsedMs)
                }
                val base = if (current == LocaleManager.LANGUAGE_ENGLISH) {
                    getString(R.string.schulte_tts_base_template, timePart)
                } else {
                    // Для RU используем готовую фразу
                    "Ты справился $timePart!"
                }
                val text = "$prefix $base"
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "schulte_done")
            }
        }
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
