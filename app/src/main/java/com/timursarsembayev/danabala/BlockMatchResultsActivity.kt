package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class BlockMatchResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    private val praisePhrases = listOf(
        "Отличная игра!",
        "Здорово! Прекрасный результат!",
        "Молодец! Так держать!",
        "Супер! Ты отлично справился(ась)!"
    )

    private val motivationPhrases = listOf(
        "Продолжай в том же духе!",
        "Хочешь попробовать ещё раз и набрать больше очков?",
        "Каждый раз у тебя получается всё лучше!",
        "Ещё немного практики — и будет ещё круче!"
    )

    private var announced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_match_results)

        val score = intent.getIntExtra("score", 0)

        // Позитивные тексты завершения игры
        findViewById<TextView>(R.id.congratsText).text = "Отличная игра!"
        findViewById<TextView>(R.id.scoreText).text = "Ты заработал(а) ${score} очков"

        // Устанавливаем мотивационный текст, если такой TextView есть в разметке
        val motId = resources.getIdentifier("motivationalText", "id", packageName)
        if (motId != 0) {
            findViewById<TextView>(motId)?.text = motivationPhrases.random()
        }

        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            startActivity(Intent(this, BlockMatchActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.backToMenuButton).setOnClickListener {
            startActivity(Intent(this, MathExercisesActivity::class.java))
            finish()
        }

        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val langResult = tts?.setLanguage(Locale("ru", "RU"))
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            // Озвучиваем поздравление, мотивацию и фразу похвалы
            if (!announced) {
                announced = true
                val title = findViewById<TextView>(R.id.congratsText).text.toString()
                val message = findViewById<TextView>(R.id.scoreText).text.toString()
                val motId = resources.getIdentifier("motivationalText", "id", packageName)
                val motivation = if (motId != 0) findViewById<TextView>(motId)?.text?.toString() else null
                val praise = praisePhrases.random()
                tts?.speak(title, TextToSpeech.QUEUE_FLUSH, null, null)
                tts?.speak(message, TextToSpeech.QUEUE_ADD, null, null)
                tts?.speak(praise, TextToSpeech.QUEUE_ADD, null, null)
                if (!motivation.isNullOrBlank()) {
                    tts?.speak(motivation, TextToSpeech.QUEUE_ADD, null, null)
                }
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
