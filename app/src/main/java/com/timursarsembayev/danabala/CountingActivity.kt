package com.timursarsembayev.danabalanumbers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*
import kotlin.random.Random

class CountingActivity : BaseActivity(), TextToSpeech.OnInitListener {

    override val adUnitId: String? get() = getString(R.string.admob_banner_id)

    private var tts: TextToSpeech? = null
    private var currentQuestion = 0
    private var score = 0
    private var totalCorrectAnswers = 0
    private val totalQuestions = 20
    private var currentCorrectAnswer = 0
    private var hasTriedCurrentQuestion = false
    private var targetNumber = 0

    // Массивы эмодзи для разных категорий
    private val fruits = listOf("🍎", "🍌", "🍊", "🍇", "🍓", "🥝", "🍑", "🍒")
    private val vegetables = listOf("🥕", "🥒", "🌶️", "🌽", "🥔", "🧄", "🧅", "🥬")
    private val animals = listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼")
    private val objects = listOf("⚽", "🏀", "🎈", "🎁", "🎂", "🧸", "🚗", "✈️")

    private val allCategories = listOf(fruits, vegetables, animals, objects)

    // Фразы обратной связи – из ресурсов
    private lateinit var correctPhrases: Array<String>
    private lateinit var incorrectPhrases: Array<String>
    private var allowTts: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counting)

        // Подключение баннера
        val bannerContainer = findViewById<ViewGroup?>(R.id.ad_container)
        attachBannerIfPossible(bannerContainer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Определяем текущую локаль и запрещаем TTS для казахского языка
        val currentLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION") resources.configuration.locale
        }
        allowTts = currentLocale.language.lowercase(Locale.ROOT) != "kk"

        // Инициализация TTS только если разрешено
        if (allowTts) {
            tts = TextToSpeech(this, this)
        }

        // Инициализация локализованных фраз
        correctPhrases = resources.getStringArray(R.array.counting_correct_phrases)
        incorrectPhrases = resources.getStringArray(R.array.counting_incorrect_phrases)

        setupBackButton()

        // Показываем загрузочный экран и начинаем первый вопрос
        startNewQuestion()
    }

    override fun onInit(status: Int) {
        if (!allowTts) return
        if (status == TextToSpeech.SUCCESS) {
            val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION") resources.configuration.locale
            }
            tts?.language = locale
        }
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun startNewQuestion() {
        if (currentQuestion >= totalQuestions) {
            showResultsScreen()
            return
        }

        // Сбрасываем флаг попыток для нового вопроса
        hasTriedCurrentQuestion = false

        // Для первого вопроса показываем загрузочный экран
        if (currentQuestion == 0) {
            findViewById<LinearLayout>(R.id.loadingContainer).visibility = android.view.View.VISIBLE
            findViewById<LinearLayout>(R.id.gameContainer).visibility = android.view.View.GONE

            findViewById<LinearLayout>(R.id.loadingContainer).postDelayed({
                generateQuestion()
                findViewById<LinearLayout>(R.id.loadingContainer).visibility = android.view.View.GONE
                findViewById<LinearLayout>(R.id.gameContainer).visibility = android.view.View.VISIBLE
            }, 1000)
        } else {
            // Для остальных вопросов сразу генерируем вопрос
            generateQuestion()
        }
    }

    private fun generateQuestion() {
        // Генерируем число от 1 до 9
        targetNumber = Random.nextInt(1, 10)
        currentCorrectAnswer = Random.nextInt(0, 4) // Позиция правильного ответа (0-3)

        // Отображаем число
        findViewById<TextView>(R.id.questionNumber).text = targetNumber.toString()

        // Обновляем прогресс-бар
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.progress = (currentQuestion * 100) / totalQuestions

        // Генерируем варианты ответов
        val answers = generateAnswerOptions(targetNumber)

        // Находим карточки
        val cards = listOf(
            findViewById<CardView>(R.id.answer1),
            findViewById<CardView>(R.id.answer2),
            findViewById<CardView>(R.id.answer3),
            findViewById<CardView>(R.id.answer4)
        )

        val answerTexts = listOf(
            findViewById<TextView>(R.id.answerText1),
            findViewById<TextView>(R.id.answerText2),
            findViewById<TextView>(R.id.answerText3),
            findViewById<TextView>(R.id.answerText4)
        )

        // Заполняем карточки разными предметами
        for (i in 0..3) {
            // Выбираем случайную категорию и предмет для каждого варианта ответа
            val selectedCategory = allCategories.random()
            val selectedEmoji = selectedCategory.random()
            val emojiString = selectedEmoji.repeat(answers[i])
            answerTexts[i].text = emojiString

            // Сбрасываем цвет карточки
            cards[i].setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))

            // Добавляем анимацию появления
            animateCardEntrance(cards[i], i * 100L)

            cards[i].setOnClickListener {
                checkAnswer(answers[i], cards[i])
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
            .start()
    }

    private fun generateAnswerOptions(correctAnswer: Int): List<Int> {
        val answers = mutableListOf<Int>()

        // Добавляем правильный ответ
        answers.add(correctAnswer)

        // Генерируем 3 неправильных ответа
        val usedNumbers = mutableSetOf(correctAnswer)

        while (answers.size < 4) {
            val wrongAnswer = Random.nextInt(1, 10)
            if (wrongAnswer !in usedNumbers) {
                answers.add(wrongAnswer)
                usedNumbers.add(wrongAnswer)
            }
        }

        // Перемешиваем ответы
        answers.shuffle()

        // Запоминаем позицию правильного ответа
        currentCorrectAnswer = answers.indexOf(correctAnswer)

        return answers
    }

    private fun checkAnswer(selectedAnswer: Int, selectedCard: CardView) {
        // Отключаем все карточки от нажатий
        disableAllCards()

        if (selectedAnswer == targetNumber) {
            // Правильный ответ
            totalCorrectAnswers++

            // Засчитываем правильный ответ с первого раза только если не было попыток
            if (!hasTriedCurrentQuestion) {
                score++
            }

            animateCorrectAnswer(selectedCard)

            // Выбираем случайную фразу похвалы
            val randomPraise = correctPhrases.random()
            if (allowTts) tts?.speak(randomPraise, TextToSpeech.QUEUE_FLUSH, null, "correct")

            // Переходим к следующему вопросу через 2 секунды
            selectedCard.postDelayed({
                currentQuestion++
                startNewQuestion()
            }, 2000)
        } else {
            // Неправильный ответ - отмечаем что была попытка
            hasTriedCurrentQuestion = true

            animateWrongAnswer(selectedCard)

            // Выбираем случайную фразу подбадривания
            val randomEncouragement = incorrectPhrases.random()
            if (allowTts) tts?.speak(randomEncouragement, TextToSpeech.QUEUE_FLUSH, null, "wrong")

            // Через 2 секунды включаем карточки обратно
            selectedCard.postDelayed({
                enableAllCards()
                resetCardColors()
            }, 2000)
        }
    }

    private fun enableAllCards() {
        val cards = listOf(
            findViewById<CardView>(R.id.answer1),
            findViewById<CardView>(R.id.answer2),
            findViewById<CardView>(R.id.answer3),
            findViewById<CardView>(R.id.answer4)
        )

        cards.forEach { it.isClickable = true }
    }

    private fun resetCardColors() {
        val cards = listOf(
            findViewById<CardView>(R.id.answer1),
            findViewById<CardView>(R.id.answer2),
            findViewById<CardView>(R.id.answer3),
            findViewById<CardView>(R.id.answer4)
        )

        cards.forEach {
            it.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        }
    }

    private fun disableAllCards() {
        val cards = listOf(
            findViewById<CardView>(R.id.answer1),
            findViewById<CardView>(R.id.answer2),
            findViewById<CardView>(R.id.answer3),
            findViewById<CardView>(R.id.answer4)
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
        val intent = Intent(this, CountingResultsActivity::class.java)
        intent.putExtra("SCORE", score)
        intent.putExtra("TOTAL_QUESTIONS", totalQuestions)
        intent.putExtra("TOTAL_CORRECT", totalCorrectAnswers)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        if (allowTts) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }
}
