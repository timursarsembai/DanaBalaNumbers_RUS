package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class BlockMatchActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var gameView: BlockMatchGameView
    private lateinit var scoreView: TextView
    private lateinit var levelView: TextView
    private var lastScore: Int = 0
    private var lastLevel: Int = 1
    private var defeatDialogShown = false

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_match)

        scoreView = findViewById<TextView>(R.id.textScore)
        levelView = findViewById<TextView>(R.id.textLevel)
        gameView = findViewById<BlockMatchGameView>(R.id.blockMatchGameView)

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }

        tts = TextToSpeech(this, this)

        gameView.onScoreLevelChanged = { score: Int, level: Int ->
            lastScore = score
            scoreView.text = score.toString()
            levelView.text = level.toString()
            if (level > lastLevel) {
                announceLevel(level)
                lastLevel = level
            } else if (level < lastLevel) {
                lastLevel = level
            }
        }
        gameView.onGameOver = fun() {
            if (defeatDialogShown || isFinishing || isDestroyed) return
            defeatDialogShown = true
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    val intent = Intent(this, BlockMatchResultsActivity::class.java)
                    intent.putExtra("score", lastScore)
                    startActivity(intent)
                    finish()
                } else {
                    defeatDialogShown = false
                }
            }
        }
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

    private fun announceLevel(level: Int) {
        if (!ttsReady) return
        val phrase = when (level) {
            2 -> "Уровень 2. Соберись!"
            3 -> "Уровень 3. Продолжаем!"
            4 -> "Уровень 4. Ускоряйся!"
            5 -> "Отлично! Уже уровень 5!"
            6 -> "Ого! Уже уровень 6, молодец!"
            7 -> "Уровень 7. Держи темп!"
            8 -> "Уровень 8. Почти там!"
            9 -> "Уровень 9. Вперед!"
            10 -> "Уровень 10. Супер скорость!"
            else -> "Уровень $level"
        }
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "bm_level_$level")
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
        tts?.stop()
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
