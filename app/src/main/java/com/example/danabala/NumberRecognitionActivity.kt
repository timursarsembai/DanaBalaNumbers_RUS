package com.example.danabalanumbers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*
import kotlin.random.Random

class NumberRecognitionActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var targetNumber = 1
    private var score = 0 // Правильные ответы с первого раза
    private var totalCorrectAnswers = 0 // Все правильные ответы (включая с повторными попытками)
    private var currentQuestion = 0
    private val totalQuestions = 20
    private var tts: TextToSpeech? = null
    private var isFirstInit = true // Флаг для первой инициализации
    private var hasTriedCurrentQuestion = false // Флаг для отслеживания попыток на текущем вопросе

    // Список всех вопросов (каждая цифра от 0 до 9 по 2 раза)
    private val questionNumbers = mutableListOf<Int>()
    private var currentQuestionIndex = 0

    // Массив прописных чисел
    private val numberWords = arrayOf(
        "ноль", "один", "два", "три", "четыре", "пять",
        "шесть", "семь", "восемь", "девять"
    )

    // Варианты похвалы за правильные ответы
    private val correctPhrases = listOf(
        "Молодец!",
        "Так держать!",
        "Превосходно!",
        "Отлично!",
        "Замечательно!",
        "Ты супер!",
        "Великолепно!",
        "Браво!",
        "Умница!",
        "Здорово!"
    )

    // Варианты подбадривания для неправильных ответов
    private val incorrectPhrases = listOf(
        "Попробуй ещё раз! У тебя получится!",
        "Не сдавайся! Ты можешь!",
        "Подумай ещё немножко!",
        "Почти правильно! Попробуй снова!",
        "Давай ещё раз! Всё получится!",
        "Не переживай! Попробуй другой вариант!",
        "Ты на верном пути! Попробуй ещё!",
        "Думай внимательнее! У тебя всё получится!",
        "Не расстраивайся! Попробуй другую карточку!",
        "Ты умный! Попробуй ещё раз!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_recognition)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация TTS
        tts = TextToSpeech(this, this)

        setupBackButton()
        // Убираем startNewQuestion() отсюда - перенесено в onInit()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ru", "RU")
            // Запускаем первый вопрос только при первой инициализации TTS
            if (isFirstInit) {
                generateQuestionSequence()
                startNewQuestion()
                isFirstInit = false
            }
        }
    }

    private fun setupBackButton() {
        findViewById<View>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun startNewQuestion() {
        if (currentQuestion >= totalQuestions) {
            showResultsScreen()
            return
        }

        currentQuestion++
        hasTriedCurrentQuestion = false // Сбрасываем флаг для нового вопроса

        // Скрываем loading и показываем игровой контент при первом вопросе
        if (currentQuestion == 1) {
            findViewById<View>(R.id.loadingContainer).visibility = View.GONE
            findViewById<View>(R.id.gameContainer).visibility = View.VISIBLE
        }

        // Берем цифру из заранее сгенерированной последовательности
        targetNumber = questionNumbers[currentQuestionIndex]
        currentQuestionIndex++

        // Обновляем вопрос с прописным числом
        val questionText = "Найди цифру ${numberWords[targetNumber]}"
        findViewById<TextView>(R.id.questionText).text = questionText

        // Озвучиваем вопрос
        tts?.speak(questionText, TextToSpeech.QUEUE_FLUSH, null, "question")

        // Настраиваем кнопку динамика для повторного озвучивания
        findViewById<View>(R.id.speakerButton).setOnClickListener {
            tts?.speak(questionText, TextToSpeech.QUEUE_FLUSH, null, "repeat_question")
        }

        // Обновляем прогресс-бар
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.progress = (currentQuestion * 100) / totalQuestions

        // Генерируем 4 случайные цифры, одна из которых правильная
        val numbers = generateNumberOptions(targetNumber)

        // Находим карточки
        val cards = listOf(
            findViewById<CardView>(R.id.card1),
            findViewById<CardView>(R.id.card2),
            findViewById<CardView>(R.id.card3),
            findViewById<CardView>(R.id.card4)
        )

        val numberTexts = listOf(
            findViewById<TextView>(R.id.number1),
            findViewById<TextView>(R.id.number2),
            findViewById<TextView>(R.id.number3),
            findViewById<TextView>(R.id.number4)
        )

        // Заполняем карточки
        for (i in 0..3) {
            numberTexts[i].text = numbers[i].toString()

            // Сбрасываем цвет карточки
            cards[i].setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))

            // Добавляем анимацию появления
            animateCardEntrance(cards[i], i * 100L)

            cards[i].setOnClickListener {
                checkAnswer(numbers[i], cards[i])
            }
        }
    }

    private fun animateCardEntrance(card: CardView, delay: Long) {
        card.alpha = 0f
        card.scaleX = 0.5f
        card.scaleY = 0.5f

        card.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setStartDelay(delay)
            .setInterpolator(BounceInterpolator())
            .start()
    }

    private fun generateNumberOptions(correct: Int): List<Int> {
        val options = mutableSetOf<Int>()
        options.add(correct)

        // Добавляем 3 неправильных варианта из диапазона 0-9
        while (options.size < 4) {
            val randomNum = Random.nextInt(0, 10)
            options.add(randomNum)
        }

        return options.shuffled()
    }

    private fun checkAnswer(selectedNumber: Int, selectedCard: CardView) {
        // Отключаем все карточки от нажатий
        disableAllCards()

        if (selectedNumber == targetNumber) {
            // Правильный ответ
            totalCorrectAnswers++

            // Засчитываем правильный ответ с первого раза только если не было попыток
            if (!hasTriedCurrentQuestion) {
                score++
            }

            animateCorrectAnswer(selectedCard)

            // Выбираем случайную фразу похвалы
            val randomPraise = correctPhrases.random()
            tts?.speak(randomPraise, TextToSpeech.QUEUE_FLUSH, null, "correct")

            // Переходим к следующему вопросу через 2 секунды
            findViewById<View>(R.id.card1).postDelayed({
                startNewQuestion()
            }, 2000)
        } else {
            // Неправильный ответ - отмечаем что была попытка
            hasTriedCurrentQuestion = true

            animateWrongAnswer(selectedCard)

            // Выбираем случайную фразу подбадривания
            val randomEncouragement = incorrectPhrases.random()
            tts?.speak(randomEncouragement, TextToSpeech.QUEUE_FLUSH, null, "wrong")

            // Через 2 секунды включаем карточки обратно
            findViewById<View>(R.id.card1).postDelayed({
                enableAllCards()
                resetCardColors()
            }, 2000)
        }
    }

    private fun enableAllCards() {
        val cards = listOf(
            findViewById<CardView>(R.id.card1),
            findViewById<CardView>(R.id.card2),
            findViewById<CardView>(R.id.card3),
            findViewById<CardView>(R.id.card4)
        )

        cards.forEach { it.isClickable = true }
    }

    private fun resetCardColors() {
        val cards = listOf(
            findViewById<CardView>(R.id.card1),
            findViewById<CardView>(R.id.card2),
            findViewById<CardView>(R.id.card3),
            findViewById<CardView>(R.id.card4)
        )

        cards.forEach {
            it.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        }
    }

    private fun disableAllCards() {
        val cards = listOf(
            findViewById<CardView>(R.id.card1),
            findViewById<CardView>(R.id.card2),
            findViewById<CardView>(R.id.card3),
            findViewById<CardView>(R.id.card4)
        )

        cards.forEach { it.isClickable = false }
    }

    private fun animateCorrectAnswer(card: CardView) {
        // Зеленый цвет для правильного ответа
        card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))

        // Анимация увеличения и уменьшения
        val scaleUpX = ObjectAnimator.ofFloat(card, "scaleX", 1f, 1.3f)
        val scaleUpY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 1.3f)
        val scaleDownX = ObjectAnimator.ofFloat(card, "scaleX", 1.3f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(card, "scaleY", 1.3f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.play(scaleUpX).with(scaleUpY)
        animatorSet.play(scaleDownX).with(scaleDownY).after(scaleUpX)
        animatorSet.duration = 200
        animatorSet.start()
    }

    private fun animateWrongAnswer(card: CardView) {
        // Красный цвет для неправильного ответа
        card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))

        // Анимация тряски
        val shake = ObjectAnimator.ofFloat(card, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        shake.duration = 500
        shake.start()
    }

    private fun showResultsScreen() {
        val intent = Intent(this, NumberRecognitionResultsActivity::class.java)
        intent.putExtra("SCORE", score)
        intent.putExtra("TOTAL_QUESTIONS", totalQuestions)
        intent.putExtra("TOTAL_CORRECT", totalCorrectAnswers)
        startActivity(intent)
        finish()
    }

    private fun generateQuestionSequence() {
        // Создаем список: каждая цифра от 0 до 9 по 2 раза
        val numbers = mutableListOf<Int>()
        for (digit in 0..9) {
            numbers.add(digit)
            numbers.add(digit)
        }

        // Перемешиваем список
        numbers.shuffle()

        // Проверяем и исправляем подряд идущие одинаковые числа
        for (i in 1 until numbers.size) {
            if (numbers[i] == numbers[i - 1]) {
                // Ищем место для обмена с числом, которое не создаст новую проблему
                for (j in i + 1 until numbers.size) {
                    if (numbers[j] != numbers[i] &&
                        (j == numbers.size - 1 || numbers[j] != numbers[j + 1]) &&
                        (i == 1 || numbers[j] != numbers[i - 2])) {
                        // Меняем местами
                        val temp = numbers[i]
                        numbers[i] = numbers[j]
                        numbers[j] = temp
                        break
                    }
                }
            }
        }

        questionNumbers.clear()
        questionNumbers.addAll(numbers)
        currentQuestionIndex = 0
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
