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
import com.timursarsembayev.danabalanumbers.R
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

    // Словарь названий предметов (род. падеж множественного числа)
    private val emojiNames: Map<String, String> = mapOf(
        "🍎" to "яблок",
        "🍌" to "бананов",
        "🍇" to "виноградин",
        "🍓" to "клубничек",
        "🍒" to "вишенок",
        "🥕" to "морковок",
        "🥒" to "огурцов",
        "🍅" to "помидоров",
        "⚽" to "мячей",
        "🏀" to "мячей",
        "🎾" to "мячиков",
        "🏐" to "мячей",
        "🎈" to "шариков",
        "🎁" to "подарков",
        "🎂" to "тортиков",
        "🎨" to "красок",
        "🌟" to "звёзд",
        "⭐" to "звёздочек",
        "✨" to "искорок",
        "🌺" to "цветков",
        "🌸" to "цветков",
        "🌼" to "ромашек",
        "🌻" to "подсолнухов",
        "🌹" to "роз"
    )

    private val confirmationEndings = listOf(
        "Ты уверен?",
        "Верно?",
        "Как думаешь, это правильно?",
        "Проверь внимательно!",
        "Давай проверим!"
    )

    // Эмодзи предметов для выбора
    private val objectEmojis = arrayOf(
        "🍎", "🍌", "🍇", "🍓", "🍒", "🥕", "🥒", "🍅",
        "⚽", "🏀", "🎾", "🏐", "🎈", "🎁", "🎂", "🎨",
        "🌟", "⭐", "✨", "🌺", "🌸", "🌼", "🌻", "🌹"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep landscape like original comparison
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_kids_comparison)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tts = TextToSpeech(this, this)
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

        // back button
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

        // Озвучиваем выбранный вариант с названием предмета
        speakSelectionQuestion(isLeft)

        val correctLeft = leftCount > rightCount
        val correctRight = rightCount > leftCount

        // Determine symbol for relation left ? right
        val symbol = when {
            leftCount > rightCount -> ">"
            rightCount > leftCount -> "<"
            else -> "="
        }
        showCenterSymbol(symbol)

        val isCorrect = (isLeft && correctLeft) || (!isLeft && correctRight)

        if (isCorrect) {
            score += 100
            totalCorrectAnswers++
            hintText.text = "Отлично!"
            hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            centerSymbol.setBackgroundResource(R.drawable.number_input_correct)
            animatePulse(centerSymbol)
            nextButton.visibility = View.VISIBLE
        } else {
            hintText.text = "Попробуй ещё раз"
            hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            // light red highlight for 0.5s on the tapped card
            val card = if (isLeft) leftCard else rightCard
            val originalColor = ContextCompat.getColor(this, android.R.color.white)
            card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            card.postDelayed({ card.setCardBackgroundColor(originalColor) }, 500)
            centerSymbol.setBackgroundResource(R.drawable.number_drop_zone)
            animateShake(card)
        }
    }

    private fun onEqualSelected() {
        if (nextButton.visibility == View.VISIBLE) return
        // Озвучка равенства
        val item = getItemNamePlural()
        val ending = confirmationEndings.random()
        val phrase = "Поровну ${item}? $ending"
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null)

        // Показать символ '=' в центре
        showCenterSymbol("=")

        val isCorrect = leftCount == rightCount
        if (isCorrect) {
            score += 100
            totalCorrectAnswers++
            hintText.text = "Верно!"
            hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            centerSymbol.setBackgroundResource(R.drawable.number_input_correct)
            animatePulse(centerSymbol)
            nextButton.visibility = View.VISIBLE
        } else {
            hintText.text = "Попробуй ещё раз"
            hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            centerSymbol.setBackgroundResource(R.drawable.number_input_incorrect)
            // вернуть фон через 500 мс
            centerSymbol.postDelayed({ centerSymbol.setBackgroundResource(R.drawable.number_drop_zone) }, 500)
            animateShake(centerSymbol)
        }
    }

    private fun showCenterSymbol(symbol: String) {
        centerSymbol.text = symbol
    }

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

    private fun generateNewQuestion() {
        nextButton.visibility = View.GONE
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
            // гарантируем неравенство
            if (rightCount == leftCount) {
                rightCount = (leftCount + 1) % 10
            }
        }
        // Исключаем случай одновременного нуля с обеих сторон
        if (leftCount == 0 && rightCount == 0) {
            if (Random.nextBoolean()) rightCount = 1 else leftCount = 1
        }

        // Choose object
        currentObjectEmoji = objectEmojis.random()

        leftObjectsDisplay.text = currentObjectEmoji.repeat(leftCount)
        rightObjectsDisplay.text = currentObjectEmoji.repeat(rightCount)
        leftNumberDisplay.text = leftCount.toString()
        rightNumberDisplay.text = rightCount.toString()

        // Текст вопроса на экране и TTS
        val itemName = emojiNames[currentObjectEmoji] ?: "предметов"
        questionText.text = "У кого $itemName больше, у мальчика или девочки?"
        hintText.text = "Нажми на карточку"
        hintText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        updateProgress()
        speakQuestion()
    }

    private fun getItemNamePlural(): String = emojiNames[currentObjectEmoji] ?: "предметов"

    private fun updateProgress() {
        progressBar.progress = ((currentQuestion.toFloat() / totalQuestions) * 100).toInt()
    }

    private fun nextQuestion() {
        currentQuestion++
        if (currentQuestion >= totalQuestions) {
            finishGame()
        } else {
            generateNewQuestion()
        }
    }

    private fun finishGame() {
        val intent = Intent(this, NumberComparisonResultsActivity::class.java)
        intent.putExtra("score", score)
        intent.putExtra("totalCorrectAnswers", totalCorrectAnswers)
        intent.putExtra("totalQuestions", totalQuestions)
        startActivity(intent)
        finish()
    }

    private fun speakQuestion() {
        val text = "У кого ${getItemNamePlural()} больше, у мальчика или девочки?"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun speakSelectionQuestion(isLeft: Boolean) {
        val item = getItemNamePlural()
        val ending = confirmationEndings.random()
        val phrase = if (isLeft) {
            "У мальчика $item больше, чем у девочки? $ending"
        } else {
            "У девочки $item больше, чем у мальчика? $ending"
        }
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts!!.setLanguage(Locale("ru", "RU"))
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
