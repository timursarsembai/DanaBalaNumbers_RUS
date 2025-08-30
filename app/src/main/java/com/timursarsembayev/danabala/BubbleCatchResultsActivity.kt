package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class BubbleCatchResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var totalScore: Int = 0
    private var isGameOver: Boolean = false

    private lateinit var languageCode: String
    private var isTtsEnabled: Boolean = false

    override fun attachBaseContext(newBase: Context) {
        val applied = LocaleManager.applyLanguage(newBase, LocaleManager.getCurrentLanguage(newBase))
        super.attachBaseContext(applied)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bubble_catch_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        totalScore = intent.getIntExtra("SCORE", 0)
        isGameOver = intent.getBooleanExtra("GAME_OVER", false)

        languageCode = LocaleManager.getCurrentLanguage(this)
        isTtsEnabled = languageCode != LocaleManager.LANGUAGE_KAZAKH
        if (isTtsEnabled) {
            tts = TextToSpeech(this, this)
        }

        setupViews()
        setupButtons()

        // Если TTS отключён (kk), не озвучиваем
        if (!isTtsEnabled) return
    }

    private fun setupViews() {
        val emojiText = findViewById<TextView>(R.id.emojiText)
        val titleText = findViewById<TextView>(R.id.messageTitleText)
        val scoreTv = findViewById<TextView>(R.id.scoreText)
        val gameOverMsg = findViewById<TextView>(R.id.gameOverMessageText)

        scoreTv.text = totalScore.toString()

        if (isGameOver) {
            // UI для проигрыша
            emojiText.text = "😔"
            titleText.text = getString(R.string.game_over)
            gameOverMsg.apply {
                text = resources.getStringArray(R.array.bubble_catch_game_over_messages).random()
                visibility = android.view.View.VISIBLE
            }
        } else {
            // Обычный итог
            emojiText.text = getString(R.string.celebration_emoji)
            titleText.text = getString(R.string.your_result)
            gameOverMsg.visibility = android.view.View.GONE
        }
    }

    private fun setupButtons() {
        findViewById<CardView>(R.id.btnPlayAgain).setOnClickListener {
            stopTTS()
            startActivity(Intent(this, BubbleCatchActivity::class.java))
            finish()
        }
        findViewById<CardView>(R.id.btnBackToMenu).setOnClickListener {
            stopTTS()
            startActivity(Intent(this, MathExercisesActivity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            stopTTS()
            finish()
        }
    }

    override fun onInit(status: Int) {
        if (!isTtsEnabled) return
        if (status == TextToSpeech.SUCCESS) {
            val ttsLocale = when (languageCode) {
                LocaleManager.LANGUAGE_RUSSIAN -> Locale.forLanguageTag("ru-RU")
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> Locale.getDefault()
            }
            tts?.language = ttsLocale
            if (isGameOver) speakGameOver() else speakCongrats()
        }
    }

    private fun speakCongrats() {
        if (!isTtsEnabled) return
        val phrase = when {
            totalScore >= 150 -> resources.getStringArray(R.array.bubble_catch_results_praise_high).random()
            totalScore >= 80 -> resources.getStringArray(R.array.bubble_catch_results_praise_medium).random()
            else -> resources.getStringArray(R.array.bubble_catch_results_encouragement).random()
        }
        val text = "$phrase ${getString(R.string.bubble_catch_results_tts_score_template, totalScore)}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bubble_catch_result")
    }

    private fun speakGameOver() {
        if (!isTtsEnabled) return
        val phrase = resources.getStringArray(R.array.bubble_catch_results_encouragement).random()
        val text = getString(R.string.bubble_catch_results_tts_game_over_template, phrase)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bubble_catch_game_over")
    }

    private fun stopTTS() { tts?.stop() }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
