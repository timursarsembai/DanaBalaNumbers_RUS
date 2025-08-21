package com.timursarsembayev.danabalanumbers

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schulte_numbers_results)

        elapsedMs = intent.getLongExtra("elapsed", 0L)
        findViewById<TextView>(R.id.timeText).text = formatTime(elapsedMs)

        tts = TextToSpeech(this, this)

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

    private fun plural(n: Long, forms: Triple<String, String, String>): String {
        val nAbs = (n % 100).toInt()
        val last = nAbs % 10
        return when {
            nAbs in 11..14 -> forms.third
            last == 1 -> forms.first
            last in 2..4 -> forms.second
            else -> forms.third
        }
    }

    private fun buildTimeSpeech(ms: Long): String {
        val totalSec = ms / 1000
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        val minForm = plural(minutes, Triple("минута", "минуты", "минут"))
        val secForm = plural(seconds, Triple("секунда", "секунды", "секунд"))
        return when {
            minutes > 0 && seconds > 0 -> "за $minutes $minForm и $seconds $secForm"
            minutes > 0 -> "за $minutes $minForm"
            else -> "за $seconds $secForm"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.forLanguageTag("ru-RU")
            val kidsPhrases = listOf(
                "Молодец!", "Отлично!", "Здорово!", "Ты умница!", "Супер!", "Браво!", "Так держать!", "У тебя получилось!"
            )
            val timePart = buildTimeSpeech(elapsedMs)
            val text = kidsPhrases.random() + " Ты справился $timePart!"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "schulte_done")
        }
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
