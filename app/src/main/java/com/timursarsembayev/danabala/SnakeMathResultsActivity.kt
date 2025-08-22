package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class SnakeMathResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snake_math_results)

        val levels = intent.getIntExtra("levels", 0)
        val won = intent.getBooleanExtra("won", false)
        val levelReached = intent.getIntExtra("level", 1)

        val titleView = findViewById<TextView>(R.id.congratsText)
        val infoView = findViewById<TextView>(R.id.scoreText)
        val motId = resources.getIdentifier("motivationalText", "id", packageName)
        val motView = if (motId != 0) findViewById<TextView>(motId) else null

        if (won) {
            titleView.text = "Победа!"
            infoView.text = "Ты дошёл до уровня $levelReached"
            motView?.text = "Фантастически! Змейка выросла до цели."
        } else {
            titleView.text = "Итоги: Змейка в математике"
            infoView.text = "Ты дошёл до уровня $levelReached"
            motView?.text = "Отличная попытка! Попробуешь ещё и пройдёшь дальше."
        }

        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            startActivity(Intent(this, SnakeMathActivity::class.java))
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
            val res = tts?.setLanguage(Locale("ru", "RU"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
            ttsReady = true
            speakResults()
        }
    }

    private fun speakResults() {
        if (!ttsReady) return
        val won = intent.getBooleanExtra("won", false)
        val levelReached = intent.getIntExtra("level", 1)
        val phrase = if (won) {
            "Поздравляю! Победа! Ты дошёл до уровня $levelReached. Молодец!"
        } else {
            "Хорошая игра! Ты дошёл до уровня $levelReached. Так держать!"
        }
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "snake_results")
    }

    override fun onPause() {
        super.onPause()
        tts?.stop()
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
