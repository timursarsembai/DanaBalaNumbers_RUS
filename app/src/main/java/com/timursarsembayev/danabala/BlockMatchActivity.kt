package com.timursarsembayev.danabalanumbers

import android.content.Context
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
    private var allowTts = false

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_match)

        scoreView = findViewById<TextView>(R.id.textScore)
        levelView = findViewById<TextView>(R.id.textLevel)
        gameView = findViewById<BlockMatchGameView>(R.id.blockMatchGameView)

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }

        val lang = LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)
        allowTts = lang != LocaleManager.LANGUAGE_KAZAKH
        if (allowTts) {
            tts = TextToSpeech(this, this)
        }

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
        if (!allowTts) return
        if (status == TextToSpeech.SUCCESS) {
            val locale = when (LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)) {
                LocaleManager.LANGUAGE_RUSSIAN -> Locale.forLanguageTag("ru-RU")
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> null
            }
            if (locale != null) {
                val res = tts?.setLanguage(locale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                ttsReady = true
            } else {
                ttsReady = false
            }
        }
    }

    private fun announceLevel(level: Int) {
        if (!allowTts || !ttsReady) return
        val phrase = when (level) {
            2 -> getString(R.string.block_match_announce_level_2)
            3 -> getString(R.string.block_match_announce_level_3)
            4 -> getString(R.string.block_match_announce_level_4)
            5 -> getString(R.string.block_match_announce_level_5)
            6 -> getString(R.string.block_match_announce_level_6)
            7 -> getString(R.string.block_match_announce_level_7)
            8 -> getString(R.string.block_match_announce_level_8)
            9 -> getString(R.string.block_match_announce_level_9)
            10 -> getString(R.string.block_match_announce_level_10)
            else -> getString(R.string.block_match_announce_level_generic, level)
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
