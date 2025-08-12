package com.timursarsembayev.danabalanumbers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.Intent
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

class DescendingSequenceActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var currentQuestion = 0
    private var score = 0
    private var totalCorrectAnswers = 0
    private val totalQuestions = 20
    private var hasTriedCurrentQuestion = false

    // UI элементы
    private lateinit var progressBar: ProgressBar
    private lateinit var questionText: TextView
    private lateinit var dropZones: List<TextView>
    private lateinit var checkButton: Button
    private lateinit var nextButton: Button
    private lateinit var hintText: TextView
    private lateinit var draggableContainer: LinearLayout

    // Логика игры
    private var sequenceSize = 5
    private var shownPosition = 0 // позиция видимого числа (0-4)
    private var shownNumber = 0 // показанное число
    private var startNumber = 0 // начальное число последовательности (наибольшее)
    private var availableNumbers = mutableListOf<Int>()
    private var userAnswers = IntArray(5) { -1 } // -1 означает пустое место

    // Варианты похвалы за правильные ответы
    private val correctPhrases = listOf(
        "Молодец!", "Так держать!", "Превосходно!", "Отлично!",
        "Замечательно!", "Ты супер!", "Великолепно!", "Браво!",
        "Умница!", "Здорово!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_descending_sequence)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tts = TextToSpeech(this, this)
        initializeViews()
        setupDragAndDrop()
        generateNewQuestion()
    }

    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBar)
        questionText = findViewById(R.id.questionText)
        checkButton = findViewById(R.id.checkButton)
        nextButton = findViewById(R.id.nextButton)
        hintText = findViewById(R.id.hintText)
        draggableContainer = findViewById(R.id.draggableNumbersContainer)

        // Инициализируем drop zones
        dropZones = listOf(
            findViewById(R.id.dropZone1),
            findViewById(R.id.dropZone2),
            findViewById(R.id.dropZone3),
            findViewById(R.id.dropZone4),
            findViewById(R.id.dropZone5)
        )

        checkButton.setOnClickListener { checkAnswer() }
        nextButton.setOnClickListener { nextQuestion() }

        // Добавляем обработчик для кнопки "Назад"
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish() // Возвращаемся к предыдущему экрану
        }

        // Изначально скрываем кнопку "Далее"
        nextButton.visibility = Button.GONE
    }

    private fun setupDragAndDrop() {
        // Настройка drop zones
        dropZones.forEachIndexed { index, dropZone ->
            dropZone.setOnDragListener { view, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> true
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        view.setBackgroundResource(R.drawable.number_drop_zone_highlight)
                        true
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        resetDropZoneBackground(view as TextView, index)
                        true
                    }
                    DragEvent.ACTION_DROP -> {
                        val draggedNumber = event.clipData.getItemAt(0).text.toString().toInt()
                        (view as TextView).text = draggedNumber.toString()
                        view.setBackgroundResource(R.drawable.number_drop_zone_filled)
                        userAnswers[index] = draggedNumber

                        // Удаляем использованную карточку
                        removeDraggedCard(draggedNumber)
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        resetDropZoneBackground(view as TextView, index)
                        true
                    }
                    else -> false
                }
            }

            // Добавляем обработчик нажа��ия для возврата карточек
            dropZone.setOnClickListener {
                returnCardToSelection(dropZone as TextView, index)
            }
        }
    }

    private fun resetDropZoneBackground(dropZone: TextView, index: Int) {
        if (index == shownPosition) {
            dropZone.setBackgroundResource(R.drawable.number_input_shown)
        } else if (dropZone.text.isNotEmpty()) {
            dropZone.setBackgroundResource(R.drawable.number_drop_zone_filled)
        } else {
            dropZone.setBackgroundResource(R.drawable.number_drop_zone)
        }
    }

    private fun createDraggableCard(number: Int): TextView {
        val card = TextView(this).apply {
            text = number.toString()
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@DescendingSequenceActivity, android.R.color.black))
            setBackgroundResource(R.drawable.draggable_number_card)
            gravity = android.view.Gravity.CENTER
            setPadding(24, 16, 24, 16)

            // Используем фиксированные размеры вместо app_icon_size
            val layoutParams = LinearLayout.LayoutParams(
                120, // ширина в пикселях (примерно 60dp)
                120  // высота в пикселях (примерно 60dp)
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            this.layoutParams = layoutParams

            // Настройка drag functionality
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val clipData = ClipData.newPlainText("number", number.toString())
                    val shadowBuilder = View.DragShadowBuilder(view)
                    view.startDragAndDrop(clipData, shadowBuilder, view, 0)
                    true
                } else {
                    false
                }
            }
        }
        return card
    }

    private fun removeDraggedCard(number: Int) {
        val firstRowContainer = findViewById<LinearLayout>(R.id.firstRowContainer)
        val secondRowContainer = findViewById<LinearLayout>(R.id.secondRowContainer)

        // Ищем карточку в первом ряду
        for (i in 0 until firstRowContainer.childCount) {
            val child = firstRowContainer.getChildAt(i) as TextView
            if (child.text.toString() == number.toString()) {
                firstRowContainer.removeView(child)
                return
            }
        }

        // Ищем карточку во втором ряду
        for (i in 0 until secondRowContainer.childCount) {
            val child = secondRowContainer.getChildAt(i) as TextView
            if (child.text.toString() == number.toString()) {
                secondRowContainer.removeView(child)
                return
            }
        }
    }

    private fun generateNewQuestion() {
        hasTriedCurrentQuestion = false
        checkButton.isEnabled = true
        nextButton.visibility = Button.GONE
        userAnswers.fill(-1)

        // Генерируем случайную позицию для показанног�� числа (0-4)
        shownPosition = Random.nextInt(sequenceSize)

        // Генерируем показанное число от 0 до 9
        shownNumber = Random.nextInt(10)

        // Вычисляем начальное число последовательности (наибольшее)
        startNumber = shownNumber + shownPosition

        // Если начальное число больше 9, корректируем
        if (startNumber > 9) {
            startNumber = 9
            shownNumber = startNumber - shownPosition
        }

        // Если последовательность выходит за 0, корректируем
        if (startNumber - sequenceSize + 1 < 0) {
            startNumber = sequenceSize - 1
            shownNumber = startNumber - shownPosition
        }

        // Очищаем и настраиваем drop zones
        dropZones.forEachIndexed { index, dropZone ->
            if (index == shownPosition) {
                dropZone.text = shownNumber.toString()
                dropZone.setBackgroundResource(R.drawable.number_input_shown)
                userAnswers[index] = shownNumber
            } else {
                dropZone.text = ""
                dropZone.setBackgroundResource(R.drawable.number_drop_zone)
            }
        }

        // Создаем доступные числа для перетаскивания (по ��быва��ию)
        availableNumbers.clear()
        for (i in 0 until sequenceSize) {
            val number = startNumber - i
            if (i != shownPosition) { // Не добавляем уже показанное число
                availableNumbers.add(number)
            }
        }

        // Добавляем несколько лишних чисел для усложнения
        val extraNumbers = mutableSetOf<Int>()
        while (extraNumbers.size < 3) {
            val extraNumber = Random.nextInt(10)
            val sequenceNumbers = (startNumber - sequenceSize + 1..startNumber).toSet()
            if (extraNumber !in sequenceNumbers) {
                extraNumbers.add(extraNumber)
            }
        }
        availableNumbers.addAll(extraNumbers)

        // Перемешиваем числа
        availableNumbers.shuffle()

        // Создаем перетаскиваемые карточки и размещаем их по 4 в ряд
        val firstRowContainer = findViewById<LinearLayout>(R.id.firstRowContainer)
        val secondRowContainer = findViewById<LinearLayout>(R.id.secondRowContainer)

        firstRowContainer.removeAllViews()
        secondRowContainer.removeAllViews()

        availableNumbers.forEachIndexed { index, number ->
            val card = createDraggableCard(number)

            if (index < 4) {
                firstRowContainer.addView(card)
            } else {
                secondRowContainer.addView(card)
            }
        }

        updateProgress()
        updateQuestionText()
        speakQuestion()
    }

    private fun updateQuestionText() {
        questionText.text = "Заполни числа по убыванию"
        hintText.text = "Перетащи числа в правильном порядке"
    }

    private fun updateProgress() {
        progressBar.progress = ((currentQuestion.toFloat() / totalQuestions) * 100).toInt()
    }

    private fun checkAnswer() {
        if (!hasTriedCurrentQuestion) {
            hasTriedCurrentQuestion = true
        }

        var allCorrect = true

        // Проверяем каждую drop zone (по убыванию)
        dropZones.forEachIndexed { index, dropZone ->
            val correctAnswer = startNumber - index
            val userAnswer = userAnswers[index]

            if (userAnswer == correctAnswer) {
                dropZone.setBackgroundResource(R.drawable.number_input_correct)
                animateCorrectAnswer(dropZone)
            } else {
                dropZone.setBackgroundResource(R.drawable.number_input_incorrect)
                animateIncorrectAnswer(dropZone)
                allCorrect = false
            }
        }

        if (allCorrect) {
            score += 100
            totalCorrectAnswers++
            showCorrectFeedback()
            checkButton.isEnabled = false
            nextButton.visibility = Button.VISIBLE
        } else {
            showIncorrectFeedback()
        }
    }

    private fun showCorrectFeedback() {
        val phrase = correctPhrases.random()
        hintText.text = phrase
        hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        speakText(phrase)
    }

    private fun showIncorrectFeedback() {
        hintText.text = "Попробуй еще раз! Числа должны уменьшаться по порядку."
        hintText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        speakText("Попробуй еще раз")
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
        val intent = Intent(this, DescendingSequenceResultsActivity::class.java)
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
        speakText("Заполни числа по убыванию. Показано число ${shownNumber}")
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale("ru", "RU"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts!!.setLanguage(Locale.getDefault())
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun returnCardToSelection(dropZone: TextView, index: Int) {
        // Проверяем, что это не стартовое число и ячейка не пустая
        if (index != shownPosition && dropZone.text.isNotEmpty()) {
            val number = dropZone.text.toString().toInt()

            // Очищаем ячейку
            dropZone.text = ""
            dropZone.setBackgroundResource(R.drawable.number_drop_zone)
            userAnswers[index] = -1

            // Возвращаем карточку в блок выбора
            addCardBackToSelection(number)
        }
    }

    private fun addCardBackToSelection(number: Int) {
        val firstRowContainer = findViewById<LinearLayout>(R.id.firstRowContainer)
        val secondRowContainer = findViewById<LinearLayout>(R.id.secondRowContainer)

        // Создаем новую карточку
        val card = createDraggableCard(number)

        // Добавляем в первый ряд, если там меньш�� 4 карточек, иначе во второй
        if (firstRowContainer.childCount < 4) {
            firstRowContainer.addView(card)
        } else {
            secondRowContainer.addView(card)
        }
    }
}
