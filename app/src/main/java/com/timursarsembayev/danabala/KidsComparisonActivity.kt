package com.timursarsembayev.danabalanumbers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale
import kotlin.random.Random

class KidsComparisonActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var currentQuestion = 0
    private var score = 0
    private var totalCorrectAnswers = 0
    private val totalQuestions = 20

    // UI
    private lateinit var progressBar: ProgressBar
    private lateinit var questionText: TextView
    private lateinit var hintText: TextView

    private lateinit var leftCard: CardView
    private lateinit var rightCard: CardView

    private lateinit var leftPersonDisplay: TextView
    private lateinit var rightPersonDisplay: TextView
    private lateinit var leftObjectsDisplay: TextView
    private lateinit var rightObjectsDisplay: TextView
    private lateinit var centerSymbol: TextView
    private lateinit var leftNumberDisplay: TextView
    private lateinit var rightNumberDisplay: TextView

    private lateinit var nextButton: Button
    private lateinit var equalButton: Button

    // Logic
    private var leftCount = 0
    private var rightCount = 0
    private var currentObjectEmoji = ""
    private var leftIsBoy = true // the other will be girl

    // Эмодзи предметов для выбора (порядок важен для соответствия массиву названий из ресурсов)
    private val objectEmojis = arrayOf(
        "🍎", "🍌", "🍇", "🍓", "🍒", "🥕", "🥒", "🍅",
        "⚽", "🏀", "🎾", "🏐", "🎈", "🎁", "🎂", "🎨",
        "🌟", "⭐", "✨", "🌺", "🌸", "🌼", "🌻", "🌹"
    )

    // Локализованные названия предметов (мн. число), порядок совпадает с objectEmojis
    private val emojiNames: List<String> by lazy {
        resources.getStringArray(R.array.kids_emoji_names).toList()
    }

    // Подтверждающие фразы для TTS
    private val confirmationEndings: List<String> by lazy {
        resources.getStringArray(R.array.kids_comparison_confirmation_endings).toList()
    }

    override fun attachBaseContext(newBase: android.content.Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Альбомная ориентация
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_kids_comparison)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализируем TTS только для ru/en; для kk — отключаем полностью
        val lang = LocaleManager.getCurrentLanguage(this)
        if (lang != LocaleManager.LANGUAGE_KAZAKH) {
            tts = TextToSpeech(this, this)
        }

        initViews()
        setupClicks()
        generateNewQuestion()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        questionText = findViewById(R.id.questionText)
        hintText = findViewById(R.id.hintText)

        leftCard = findViewById(R.id.leftCard)
        rightCard = findViewById(R.id.rightCard)

        leftPersonDisplay = findViewById(R.id.leftPersonDisplay)
        rightPersonDisplay = findViewById(R.id.rightPersonDisplay)
        leftObjectsDisplay = findViewById(R.id.leftObjectsDisplay)
        rightObjectsDisplay = findViewById(R.id.rightObjectsDisplay)
        leftNumberDisplay = findViewById(R.id.leftNumberDisplay)
        rightNumberDisplay = findViewById(R.id.rightNumberDisplay)

        centerSymbol = findViewById(R.id.centerSymbol)
        nextButton = findViewById(R.id.nextButton)
        equalButton = findViewById(R.id.equalButton)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        nextButton.visibility = View.GONE
    }

    private fun setupClicks() {
        leftCard.setOnClickListener { onSideSelected(isLeft = true) }
        rightCard.setOnClickListener { onSideSelected(isLeft = false) }
        equalButton.setOnClickListener { onEqualSelected() }
        nextButton.setOnClickListener { nextQuestion() }
    }

    private fun onSideSelected(isLeft: Boolean) {
        if (nextButton.visibility == View.VISIBLE) return

        // Выделяем сторону
        selected = if (isLeft) Selected.LEFT else Selected.RIGHT
        updateSelectionUI()

        // Озвучиваем выбранный вариант
        speakSelectionQuestion(isLeft)

        // Символ выбора пользователя в центре
        showCenterSymbol(if (isLeft) ">" else "<")

        val correctLeft = leftCount > rightCount
        val correctRight = rightCount > leftCount
        val isCorrect = (isLeft && correctLeft) || (!isLeft && correctRight)

        if (isCorrect) {
            score += 100
            totalCorrectAnswers++
            hintText.text = getString(R.string.kids_comparison_correct)
            hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            centerSymbol.setBackgroundResource(R.drawable.number_input_correct)
            animatePulse(centerSymbol)
            nextButton.visibility = View.VISIBLE
        } else {
            hintText.text = getString(R.string.kids_comparison_try_again)
            hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            val card = if (isLeft) leftCard else rightCard
            card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            card.postDelayed({ updateSelectionUI() }, 500)
            centerSymbol.setBackgroundResource(R.drawable.number_drop_zone)
            animateShake(card)
        }
    }

    private fun onEqualSelected() {
        if (nextButton.visibility == View.VISIBLE) return
        selected = Selected.NONE
        updateSelectionUI()

        // Озвучка равенства
        val item = getItemNamePlural()
        val ending = confirmationEndings.random()
        val phrase = getString(R.string.kids_comparison_equal_tts, item, ending)
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null)

        showCenterSymbol("=")

        val isCorrect = leftCount == rightCount
        if (isCorrect) {
            score += 100
            totalCorrectAnswers++
            hintText.text = getString(R.string.kids_comparison_equal_correct)
            hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            centerSymbol.setBackgroundResource(R.drawable.number_input_correct)
            animatePulse(centerSymbol)
            nextButton.visibility = View.VISIBLE
        } else {
            hintText.text = getString(R.string.kids_comparison_try_again)
            hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            centerSymbol.setBackgroundResource(R.drawable.number_input_incorrect)
            centerSymbol.postDelayed({ centerSymbol.setBackgroundResource(R.drawable.number_drop_zone) }, 500)
            animateShake(centerSymbol)
        }
    }

    private fun showCenterSymbol(symbol: String) { centerSymbol.text = symbol }

    private fun animatePulse(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.15f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.15f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy)
            duration = 300
            start()
        }
    }

    private fun animateShake(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationX", 0f, -10f, 10f, -5f, 5f, 0f)
        anim.duration = 500
        anim.start()
    }

    private enum class Selected { NONE, LEFT, RIGHT }
    private var selected: Selected = Selected.NONE

    private fun updateSelectionUI() {
        val blue = ContextCompat.getColor(this, android.R.color.holo_blue_light)
        val white = ContextCompat.getColor(this, android.R.color.white)
        when (selected) {
            Selected.LEFT -> { leftCard.setCardBackgroundColor(blue); rightCard.setCardBackgroundColor(white) }
            Selected.RIGHT -> { leftCard.setCardBackgroundColor(white); rightCard.setCardBackgroundColor(blue) }
            Selected.NONE -> { leftCard.setCardBackgroundColor(white); rightCard.setCardBackgroundColor(white) }
        }
    }

    private fun generateNewQuestion() {
        nextButton.visibility = View.GONE
        selected = Selected.NONE
        updateSelectionUI()
        centerSymbol.text = ""
        centerSymbol.setBackgroundResource(R.drawable.number_drop_zone)

        // Randomly assign who is boy/girl ensuring left/right are different
        leftIsBoy = Random.nextBoolean()
        leftPersonDisplay.text = if (leftIsBoy) "👦" else "👧"
        rightPersonDisplay.text = if (leftIsBoy) "👧" else "👦"

        // Генерация чисел с шансом равенства ~30%
        leftCount = Random.nextInt(0, 10)
        rightCount = Random.nextInt(0, 10)
        if (Random.nextFloat() < 0.3f) {
            rightCount = leftCount
        } else {
            if (rightCount == leftCount) {
                rightCount = (leftCount + 1) % 10
            }
        }
        if (leftCount == 0 && rightCount == 0) {
            if (Random.nextBoolean()) rightCount = 1 else leftCount = 1
        }

        // Выбор предмета
        currentObjectEmoji = objectEmojis.random()

        leftObjectsDisplay.text = currentObjectEmoji.repeat(leftCount)
        rightObjectsDisplay.text = currentObjectEmoji.repeat(rightCount)
        leftNumberDisplay.text = leftCount.toString()
        rightNumberDisplay.text = rightCount.toString()

        // Текст вопроса и подсказка
        val itemName = getItemNamePlural()
        questionText.text = getString(R.string.kids_comparison_question_template, itemName)
        hintText.text = getString(R.string.kids_comparison_hint_tap_card)
        hintText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        updateProgress()
        speakQuestion()
    }

    private fun getItemNamePlural(): String {
        val index = objectEmojis.indexOf(currentObjectEmoji)
        return if (index in emojiNames.indices) emojiNames[index]
        else getString(R.string.kids_items_generic)
    }

    private fun updateProgress() {
        progressBar.progress = ((currentQuestion.toFloat() / totalQuestions) * 100).toInt()
    }

    private fun nextQuestion() {
        currentQuestion++
        if (currentQuestion >= totalQuestions) finishGame() else generateNewQuestion()
    }

    private fun finishGame() {
        val intent = Intent(this, NumberComparisonResultsActivity::class.java)
        intent.putExtra("score", score)
        intent.putExtra("totalCorrectAnswers", totalCorrectAnswers)
        intent.putExtra("totalQuestions", totalQuestions)
        intent.putExtra("fromKidsComparison", true)
        startActivity(intent)
        finish()
    }

    private fun speakQuestion() {
        if (tts == null) return
        val item = getItemNamePlural()
        val phrase = getString(R.string.kids_comparison_question_template_tts, item)
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun speakSelectionQuestion(isLeft: Boolean) {
        if (tts == null) return
        val item = getItemNamePlural()
        val ending = confirmationEndings.random()
        val selectedIsBoy = if (isLeft) leftIsBoy else !leftIsBoy
        val subject = if (selectedIsBoy) getString(R.string.kids_boy_genitive) else getString(R.string.kids_girl_genitive)
        val other = if (selectedIsBoy) getString(R.string.kids_girl_genitive) else getString(R.string.kids_boy_genitive)
        val phrase = getString(R.string.kids_comparison_selection_tts_template, subject, item, other, ending)
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val lang = LocaleManager.getCurrentLanguage(this)
            val locale = when (lang) {
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> Locale.forLanguageTag("ru-RU")
            }
            val res = tts!!.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts!!.setLanguage(Locale.getDefault())
            }
            speakQuestion()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
