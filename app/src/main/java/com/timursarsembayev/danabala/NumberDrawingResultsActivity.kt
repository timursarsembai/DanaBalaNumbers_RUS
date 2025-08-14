package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class NumberDrawingResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    // Фразы похвалы и мотивации
    private val praisePhrases = listOf(
        "Отлично! Ты замечательно закрасил(а) все цифры!",
        "Браво! У тебя получилось очень аккуратно!",
        "Здорово! Прекрасная работа!",
        "Молодец! Так держать!"
    )

    private val motivationPhrases = listOf(
        "Продолжай тренироваться, и будет ещё лучше!",
        "Хочешь попробовать ещё раз и сделать ещё красивее?",
        "Ты большой молодец! Переходи к следующему заданию!",
        "С каждым разом у тебя получается всё лучше!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_drawing_results)

        tts = TextToSpeech(this, this)

        setupViews()
    }

    private fun setupViews() {
        // Тексты
        findViewById<TextView>(R.id.messageDisplay)?.text = "Отлично! 🏆"
        findViewById<TextView>(R.id.motivationalMessage)?.text = "Ты замечательно справился(ась)!"
        findViewById<TextView>(R.id.congratulationsIcon)?.text = "🎉"

        findViewById<ImageButton>(R.id.backButton)?.setOnClickListener { finish() }

        findViewById<Button>(R.id.restartButton)?.setOnClickListener {
            startActivity(Intent(this, NumberDrawingActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.homeButton)?.setOnClickListener {
            val intent = Intent(this, MathExercisesActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ru", "RU"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            // Небольшая задержка, чтобы не перебивать системные звуки
            android.os.Handler(mainLooper).postDelayed({
                speak(praisePhrases.random())
                speak(motivationPhrases.random())
            }, 600)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
