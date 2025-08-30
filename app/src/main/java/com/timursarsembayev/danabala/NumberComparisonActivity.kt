package com.timursarsembayev.danabalanumbers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*
import kotlin.random.Random

class NumberComparisonActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var currentQuestion = 0
    private var score = 0
    private var totalCorrectAnswers = 0
    private val totalQuestions = 20
    private var hasTriedCurrentQuestion = false

    // UI элементы
    private lateinit var progressBar: ProgressBar
    private lateinit var questionText: TextView
    private lateinit var leftNumberDisplay: TextView
    private lateinit var rightNumberDisplay: TextView
    private lateinit var leftObjectsDisplay: TextView
    private lateinit var rightObjectsDisplay: TextView
    private lateinit var comparisonDropZone: TextView
    private lateinit var checkButton: Button
    private lateinit var nextButton: Button
    private lateinit var hintText: TextView
    private lateinit var comparisonSymbolsContainer: LinearLayout

    // Логика игры
    private var leftNumber = 0
    private var rightNumber = 0
    private var correctComparison = ""
    private var currentObjectType = ""
    private var selectedSymbol = ""

    // Варианты похвалы за правильные ответы (из ресурсов)
    private val correctPhrases: List<String> by lazy {
        resources.getStringArray(R.array.number_comparison_praise).toList()
    }

    // Эмодзи для предметов
    private val objectEmojis = arrayOf(
        "🍎", "🍌", "🍇", "🍓", "🍒", "🥕", "🥒", "🍅",
        "⚽", "🏀", "🎾", "🏐", "🎈", "🎁", "🎂", "🎨",
        "🌟", "⭐", "✨", "🌺", "🌸", "🌼", "🌻", "🌹"
    )

    // Варианты побуждающих фраз для озвучки (из ресурсов)
    private val encouragementPhrases: List<String> by lazy {
        resources.getStringArray(R.array.number_comparison_tts_encouragement).toList()
    }

    override fun attachBaseContext(newBase: Context) {
        val ctx = LocaleManager.applyLanguage(newBase)
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Принудительно устанавливаем альбомную ориентацию
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContentView(R.layout.activity_number_comparison)

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

        initializeViews()
        setupDragAndDrop()
        generateNewQuestion()
    }

    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBar)
        questionText = findViewById(R.id.questionText)
        leftNumberDisplay = findViewById(R.id.leftNumberDisplay)
        rightNumberDisplay = findViewById(R.id.rightNumberDisplay)
        leftObjectsDisplay = findViewById(R.id.leftObjectsDisplay)
        rightObjectsDisplay = findViewById(R.id.rightObjectsDisplay)
        comparisonDropZone = findViewById(R.id.comparisonDropZone)
        checkButton = findViewById(R.id.checkButton)
        nextButton = findViewById(R.id.nextButton)
        hintText = findViewById(R.id.hintText)
        comparisonSymbolsContainer = findViewById(R.id.comparisonSymbolsContainer)

        checkButton.setOnClickListener { checkAnswer() }
        nextButton.setOnClickListener { nextQuestion() }

        // Добавляем обработчик для кнопки "Назад"
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Изначально скрываем кнопку "Далее"
        nextButton.visibility = Button.GONE

        // Создаем символы сравнения
        createComparisonSymbols()
    }

    private fun createComparisonSymbols() {
        val symbols = arrayOf("<", ">", "=")

        symbols.forEach { symbol ->
            val symbolView = TextView(this).apply {
                text = symbol
                textSize = 28f  // Уменьшили с 32f до 28f
                setTextColor(ContextCompat.getColor(this@NumberComparisonActivity, android.R.color.black))
                setBackgroundResource(R.drawable.draggable_number_card)
                gravity = android.view.Gravity.CENTER
                setPadding(4, 4, 4, 4)
                includeFontPadding = false

                val layoutParams = LinearLayout.LayoutParams(70, 70).apply {
                    setMargins(8, 4, 8, 4)
                    gravity = android.view.Gravity.CENTER
                }
                this.layoutParams = layoutParams

                // Настройка drag functionality
                setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val clipData = ClipData.newPlainText("symbol", symbol)
                        val shadowBuilder = View.DragShadowBuilder(view)
                        view.startDragAndDrop(clipData, shadowBuilder, view, 0)
                        true
                    } else {
                        false
                    }
                }
            }
            comparisonSymbolsContainer.addView(symbolView)
        }
    }

    private fun setupDragAndDrop() {
        // Настройка drop zone для символов сравнения
        comparisonDropZone.setOnDragListener { view, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> {
                    view.setBackgroundResource(R.drawable.number_drop_zone_highlight)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    resetDropZoneBackground()
                    true
                }
                DragEvent.ACTION_DROP -> {
                    val symbol = event.clipData.getItemAt(0).text.toString()
                    handleSymbolDrop(symbol)
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    resetDropZoneBackground()
                    true
                }
                else -> false
            }
        }

        // Клик по drop zone для возврата символа
        comparisonDropZone.setOnClickListener {
            returnSymbolToContainer()
        }

        // Настройка контейнера символов как drop zone для возврата
        comparisonSymbolsContainer.setOnDragListener { view, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> {
                    view.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
                    view.alpha = 0.3f
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    view.alpha = 1.0f
                    true
                }
                DragEvent.ACTION_DROP -> {
                    view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    view.alpha = 1.0f
                    returnSymbolToContainer()
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    view.alpha = 1.0f
                    true
                }
                else -> false
            }
        }
    }

    private fun handleSymbolDrop(symbol: String) {
        // Если уже есть символ, возвращаем его
        if (selectedSymbol.isNotEmpty()) {
            returnSymbolToContainer()
        }

        // Устанавливаем новый символ
        selectedSymbol = symbol
        comparisonDropZone.text = symbol
        comparisonDropZone.setBackgroundResource(R.drawable.number_drop_zone_filled)

        // Удаляем символ из контейнера
        removeSymbolFromContainer(symbol)

        // Активируем кнопку проверки
        checkButton.isEnabled = true

        // Озвучиваем вопрос с добавленным символом
        speakComparisonQuestion(symbol)
    }

    private fun returnSymbolToContainer() {
        if (selectedSymbol.isNotEmpty()) {
            // Очищаем drop zone
            comparisonDropZone.text = ""
            comparisonDropZone.setBackgroundResource(R.drawable.number_drop_zone)

            // Возвращаем символ в контейнер
            addSymbolToContainer(selectedSymbol)

            selectedSymbol = ""
            checkButton.isEnabled = false
        }
    }

    private fun removeSymbolFromContainer(symbol: String) {
        for (i in 0 until comparisonSymbolsContainer.childCount) {
            val child = comparisonSymbolsContainer.getChildAt(i) as TextView
            if (child.text.toString() == symbol) {
                comparisonSymbolsContainer.removeView(child)
                break
            }
        }
    }

    private fun addSymbolToContainer(symbol: String) {
        val symbolView = TextView(this).apply {
            text = symbol
            textSize = 28f  // Изменили с 32f на 28f для соответствия
            setTextColor(ContextCompat.getColor(this@NumberComparisonActivity, android.R.color.black))
            setBackgroundResource(R.drawable.draggable_number_card)
            gravity = android.view.Gravity.CENTER
            setPadding(4, 4, 4, 4)
            includeFontPadding = false

            val layoutParams = LinearLayout.LayoutParams(70, 70).apply {
                setMargins(8, 4, 8, 4)
                gravity = android.view.Gravity.CENTER
            }
            this.layoutParams = layoutParams

            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val clipData = ClipData.newPlainText("symbol", symbol)
                    val shadowBuilder = View.DragShadowBuilder(view)
                    view.startDragAndDrop(clipData, shadowBuilder, view, 0)
                    true
                } else {
                    false
                }
            }
        }

        // Вставляем символ в правильном порядке
        val symbols = arrayOf("<", ">", "=")
        val position = symbols.indexOf(symbol)
        if (position != -1 && position <= comparisonSymbolsContainer.childCount) {
            comparisonSymbolsContainer.addView(symbolView, position)
        } else {
            comparisonSymbolsContainer.addView(symbolView)
        }
    }

    private fun resetDropZoneBackground() {
        if (comparisonDropZone.text.isEmpty()) {
            comparisonDropZone.setBackgroundResource(R.drawable.number_drop_zone)
        } else {
            comparisonDropZone.setBackgroundResource(R.drawable.number_drop_zone_filled)
        }
    }

    private fun generateNewQuestion() {
        hasTriedCurrentQuestion = false
        checkButton.isEnabled = false
        nextButton.visibility = Button.GONE

        // Очищаем предыдущее состояние
        returnSymbolToContainer()

        // Генерируем два разных числа от 0 до 9
        do {
            leftNumber = Random.nextInt(10)
            rightNumber = Random.nextInt(10)
        } while (leftNumber == rightNumber) // Убеждаемся, что числа разные для начала

        // В 30% случаев делаем числа равными
        if (Random.nextFloat() < 0.3f) {
            rightNumber = leftNumber
        }

        // Выбираем случайный объект
        currentObjectType = objectEmojis[Random.nextInt(objectEmojis.size)]

        // Определяем правильный символ сравнения
        correctComparison = when {
            leftNumber < rightNumber -> "<"
            leftNumber > rightNumber -> ">"
            else -> "="
        }

        // Обновляем отображение
        leftNumberDisplay.text = leftNumber.toString()
        rightNumberDisplay.text = rightNumber.toString()

        leftObjectsDisplay.text = generateObjects(leftNumber)
        rightObjectsDisplay.text = generateObjects(rightNumber)

        updateProgress()
        updateQuestionText()
        speakQuestion()
    }

    private fun generateObjects(count: Int): String {
        if (count == 0) return ""

        // Отображаем все предметы в одну строку
        return currentObjectType.repeat(count)
    }

    private fun updateQuestionText() {
        questionText.text = getString(R.string.number_comparison_question)
        hintText.text = getString(R.string.number_comparison_hint_drag_symbol)
    }

    private fun updateProgress() {
        progressBar.progress = ((currentQuestion.toFloat() / totalQuestions) * 100).toInt()
    }

    private fun checkAnswer() {
        if (!hasTriedCurrentQuestion) {
            hasTriedCurrentQuestion = true
        }

        if (selectedSymbol == correctComparison) {
            // Правильный ответ
            score += 100
            totalCorrectAnswers++
            showCorrectFeedback()
            checkButton.isEnabled = false
            nextButton.visibility = Button.VISIBLE

            comparisonDropZone.setBackgroundResource(R.drawable.number_input_correct)
            animateCorrectAnswer(comparisonDropZone)
        } else {
            // Неправильный ответ
            showIncorrectFeedback()
            comparisonDropZone.setBackgroundResource(R.drawable.number_input_incorrect)
            animateIncorrectAnswer(comparisonDropZone)
        }
    }

    private fun showCorrectFeedback() {
        val phrase = correctPhrases.random()
        hintText.text = phrase
        hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        speakText(phrase)
    }

    private fun showIncorrectFeedback() {
        hintText.text = getString(R.string.number_comparison_try_again)
        hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        speakText(getString(R.string.number_comparison_try_again_short))
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

    private fun animateCorrectAnswer(view: TextView) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.start()
    }

    private fun animateIncorrectAnswer(view: TextView) {
        val shake = ObjectAnimator.ofFloat(view, "translationX", 0f, -10f, 10f, -5f, 5f, 0f)
        shake.duration = 500
        shake.start()
    }

    private fun speakQuestion() {
        if (tts == null) return
        val leftDescription = getNumberDescription(leftNumber)
        val rightDescription = getNumberDescription(rightNumber)
        val template = getString(R.string.number_comparison_tts_compare_template)
        speakText(String.format(template, leftDescription, rightDescription))
    }

    private fun speakComparisonQuestion(symbol: String) {
        if (tts == null) return
        val leftDescription = getNumberDescription(leftNumber)
        val rightDescription = getNumberDescription(rightNumber)

        val symbolDescription = when (symbol) {
            "<" -> getString(R.string.less_than_word)
            ">" -> getString(R.string.greater_than_word)
            "=" -> getString(R.string.equals_word)
            else -> symbol
        }

        val encouragement = encouragementPhrases.random()
        val template = getString(R.string.number_comparison_tts_compare_with_symbol_template)
        val question = String.format(template, leftDescription, symbolDescription, rightDescription, encouragement)
        speakText(question)
    }

    private fun getNumberDescription(number: Int): String {
        return when (number) {
            0 -> getString(R.string.number_name_0)
            1 -> getString(R.string.number_name_1)
            2 -> getString(R.string.number_name_2)
            3 -> getString(R.string.number_name_3)
            4 -> getString(R.string.number_name_4)
            5 -> getString(R.string.number_name_5)
            6 -> getString(R.string.number_name_6)
            7 -> getString(R.string.number_name_7)
            8 -> getString(R.string.number_name_8)
            9 -> getString(R.string.number_name_9)
            else -> number.toString()
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val lang = LocaleManager.getCurrentLanguage(this)
            val locale = when (lang) {
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                else -> Locale.forLanguageTag("ru-RU")
            }
            val result = tts!!.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
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
