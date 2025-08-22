package com.timursarsembayev.danabalanumbers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.GestureDetectorCompat
import android.view.GestureDetector
import android.view.MotionEvent
import java.util.*
import kotlin.random.Random

class NumberIntroductionActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var currentSlide = 0
    private val totalSlides = 10 // Цифры от 0 до 9
    private var currentObjectType = "" // Текущий тип предмета для озвучивания

    // UI элементы
    private lateinit var numberDisplay: TextView
    private lateinit var objectsDisplay: TextView
    private lateinit var lessonText: TextView
    private lateinit var speakButton: ImageButton
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var slideIndicator: TextView

    // Массивы разных предметов для рандомного отображения
    private val objectTypes = arrayOf(
        "🎈", "🎁", "🎂", "🎨", "🎲", "🎭",
        "🍎", "🍌", "🍇", "🍓", "🍒", "🥕", "🥒", "🍅",
        "⚽", "🏀", "🎾", "🏐",
        "🌟", "⭐", "✨", "☀️", "🌙",
        "🦋", "🐝", "🐞", "🐸", "🐢", "🐠", "🐛",
        "🚗", "🚌", "🚓", "🚑", "🚒",
        "📖", "⏰", "👓", "👑",
        "🌺", "🌸", "🌼", "🌻", "🌹", "🌷", "💐"
    )

    // Класс для хранения информации о слове с полными склонениями
    data class WordDeclension(
        val nominative: String,      // один цветок, одна бабочка
        val genitiveSingular: String, // два цветка, две бабочки
        val genitivePlural: String,   // пять цветков, пять бабочек
        val gender: Gender
    )

    // Перечисление родов
    enum class Gender {
        MASCULINE,  // мужской род
        FEMININE,   // женский род
        NEUTER      // средний род
    }

    // Полная таблица склонений для всех объектов
    private val objectDeclensions = mapOf(
        "🎈" to WordDeclension("шарик", "шарика", "шариков", Gender.MASCULINE),
        "🎁" to WordDeclension("подарок", "подарка", "подарков", Gender.MASCULINE),
        "🎂" to WordDeclension("торт", "торта", "тортов", Gender.MASCULINE),
        "🎨" to WordDeclension("краски", "красок", "красок", Gender.FEMININE),
        "🎲" to WordDeclension("кубик", "кубика", "кубиков", Gender.MASCULINE),
        "🎭" to WordDeclension("маска", "маски", "масок", Gender.FEMININE),
        "🍎" to WordDeclension("яблоко", "яблока", "яблок", Gender.NEUTER),
        "🍌" to WordDeclension("банан", "банана", "бананов", Gender.MASCULINE),
        "🍇" to WordDeclension("виноград", "винограда", "виноградов", Gender.MASCULINE),
        "🍓" to WordDeclension("клубника", "клубники", "клубники", Gender.FEMININE),
        "🍒" to WordDeclension("вишня", "вишни", "вишень", Gender.FEMININE),
        "🥕" to WordDeclension("морковка", "морковки", "морковок", Gender.FEMININE),
        "🥒" to WordDeclension("огурец", "огурца", "огурцов", Gender.MASCULINE),
        "🍅" to WordDeclension("помидор", "помидора", "помидоров", Gender.MASCULINE),
        "⚽" to WordDeclension("мяч", "мяча", "мячей", Gender.MASCULINE),
        "🏀" to WordDeclension("мяч", "мяча", "мячей", Gender.MASCULINE),
        "🎾" to WordDeclension("мячик", "мячика", "мячиков", Gender.MASCULINE),
        "🏐" to WordDeclension("мяч", "мяча", "мячей", Gender.MASCULINE),
        "🌟" to WordDeclension("звезда", "звезды", "звезд", Gender.FEMININE),
        "⭐" to WordDeclension("звездочка", "звездочки", "звездочек", Gender.FEMININE),
        "✨" to WordDeclension("искорка", "искорки", "искорок", Gender.FEMININE),
        "☀️" to WordDeclension("солнце", "солнца", "солнц", Gender.NEUTER),
        "🌙" to WordDeclension("луна", "луны", "лун", Gender.FEMININE),
        "🦋" to WordDeclension("бабочка", "бабочки", "бабочек", Gender.FEMININE),
        "🐝" to WordDeclension("пчела", "пчелы", "пчел", Gender.FEMININE),
        "🐞" to WordDeclension("божья коровка", "божьей коровки", "божьих коровок", Gender.FEMININE),
        "🐸" to WordDeclension("лягушка", "лягушки", "лягушек", Gender.FEMININE),
        "🐢" to WordDeclension("черепаха", "черепахи", "черепах", Gender.FEMININE),
        "🐠" to WordDeclension("рыбка", "рыбки", "рыбок", Gender.FEMININE),
        "🐛" to WordDeclension("гусеница", "гусеницы", "гусениц", Gender.FEMININE),
        "🚗" to WordDeclension("машина", "машины", "машин", Gender.FEMININE),
        "🚌" to WordDeclension("автобус", "автобуса", "автобусов", Gender.MASCULINE),
        "🚓" to WordDeclension("машина", "машины", "машин", Gender.FEMININE),
        "🚑" to WordDeclension("машина", "машины", "машин", Gender.FEMININE),
        "🚒" to WordDeclension("машина", "машины", "машин", Gender.FEMININE),
        "📖" to WordDeclension("книга", "книги", "книг", Gender.FEMININE),
        "⏰" to WordDeclension("часы", "часов", "часов", Gender.MASCULINE),
        "👓" to WordDeclension("очки", "очков", "очков", Gender.MASCULINE),
        "👑" to WordDeclension("корона", "короны", "корон", Gender.FEMININE),
        "🌺" to WordDeclension("цветок", "цветка", "цветков", Gender.MASCULINE),
        "🌸" to WordDeclension("цветок", "цветка", "цветков", Gender.MASCULINE),
        "🌼" to WordDeclension("ромашка", "ромашки", "ромашек", Gender.FEMININE),
        "🌻" to WordDeclension("подсолнух", "подсолнуха", "подсолнухов", Gender.MASCULINE),
        "🌹" to WordDeclension("роза", "розы", "роз", Gender.FEMININE),
        "🌷" to WordDeclension("тюльпан", "тюльпана", "тюльпанов", Gender.MASCULINE),
        "💐" to WordDeclension("букет", "букета", "букетов", Gender.MASCULINE)
    )

    // Данные для слайдов
    private val numberData = listOf(
        NumberSlideData(
            number = 0,
            objects = "",
            lesson = "Это цифра НОЛЬ. Она означает, что предметов совсем нет, ничего. Ноль - это пустота, отсутствие количества."
        ),
        NumberSlideData(
            number = 1,
            objects = "",
            lesson = "Это цифра ОДИН. Она означает один предмет, что-то одно. Посмотри - здесь {OBJECT_DESCRIPTION}."
        ),
        NumberSlideData(
            number = 2,
            objects = "",
            lesson = "Это цифра ДВА. Она означает два предмета, пару. Посмотри - здесь {OBJECT_DESCRIPTION}."
        ),
        NumberSlideData(
            number = 3,
            objects = "",
            lesson = "Это цифра ТРИ. Она означает три предмета. Посмотри - здесь {OBJECT_DESCRIPTION}."
        ),
        NumberSlideData(
            number = 4,
            objects = "",
            lesson = "Это цифра ЧЕТЫРЕ. Она означает четыре предмета. Посмотри - здесь {OBJECT_DESCRIPTION}."
        ),
        NumberSlideData(
            number = 5,
            objects = "",
            lesson = "Это цифра ПЯТЬ. Она означает пять предметов. Посмотри - здесь {OBJECT_DESCRIPTION}."
        ),
        NumberSlideData(
            number = 6,
            objects = "",
            lesson = "Это цифра ШЕСТЬ. Она означает шесть предметов. Посмотри - здесь {OBJECT_DESCRIPTION}."
        ),
        NumberSlideData(
            number = 7,
            objects = "",
            lesson = "Это цифра СЕМЬ. Она означает семь предметов. Посмотри - здесь {OBJECT_DESCRIPTION}."
        ),
        NumberSlideData(
            number = 8,
            objects = "",
            lesson = "Это цифра ВОСЕМЬ. Она означает восемь предметов. Посмотри - здесь {OBJECT_DESCRIPTION}."
        ),
        NumberSlideData(
            number = 9,
            objects = "",
            lesson = "Это цифра ДЕВЯТЬ. Она означает девять предметов. Посмотри - здесь {OBJECT_DESCRIPTION}."
        )
    )

    data class NumberSlideData(
        val number: Int,
        val objects: String,
        val lesson: String
    )

    private fun generateRandomObjects(count: Int): String {
        if (count == 0) return ""

        // Выбираем случайный тип предмета
        val objectType = objectTypes[Random.nextInt(objectTypes.size)]
        // Сохраняем выбранный тип для озвучивания
        currentObjectType = objectType

        val result = StringBuilder()

        // Логика размещения предметов в рода для лучшего отображения
        val itemsPerRow = when (count) {
            1, 2, 3, 4 -> count // 1-4 предмета в один ряд
            5, 6 -> 3 // 5-6 предметов: по 3 в ряд (2 ряда)
            7, 8, 9 -> when (count) {
                7 -> 4 // 7 предметов: 4 + 3
                8 -> 4 // 8 предметов: 4 + 4
                9 -> 5 // 9 предметов: 5 + 4
                else -> 4
            }
            else -> 4
        }

        for (i in 1..count) {
            result.append(objectType)

            // Добавляем перенос строки после нужного количества предметов (кроме последнего)
            if (i % itemsPerRow == 0 && i < count) {
                result.append("\n")
            }
        }

        return result.toString()
    }

    private fun getObjectNameWithCount(count: Int, objectEmoji: String): String {
        val objectInfo = objectDeclensions[objectEmoji] ?: WordDeclension("предмет", "предмета", "предметов", Gender.MASCULINE)
        val objectName = objectInfo.nominative
        val genitiveSingular = objectInfo.genitiveSingular
        val genitivePlural = objectInfo.genitivePlural
        val gender = objectInfo.gender

        return when (count) {
            1 -> {
                val numeral = when (gender) {
                    Gender.MASCULINE -> "один"
                    Gender.FEMININE -> "одна"
                    Gender.NEUTER -> "одно"
                }
                "$numeral $objectName"
            }
            2 -> {
                val numeral = when (gender) {
                    Gender.MASCULINE -> "два"
                    Gender.FEMININE -> "две"
                    Gender.NEUTER -> "два"
                }
                "$numeral $genitiveSingular"
            }
            3 -> {
                val numeral = "три"
                "$numeral $genitiveSingular"
            }
            4 -> {
                val numeral = "четыре"
                "$numeral $genitiveSingular"
            }
            5, 6, 7, 8, 9 -> {
                val numeral = when (count) {
                    5 -> "пять"
                    6 -> "шесть"
                    7 -> "семь"
                    8 -> "восемь"
                    9 -> "девять"
                    else -> count.toString()
                }
                "$numeral $genitivePlural"
            }
            else -> "$count $objectName"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_introduction)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tts = TextToSpeech(this, this)
        initializeViews()
        setupClickListeners()
        setupSwipeGestures()
        updateSlide()
    }

    private fun initializeViews() {
        numberDisplay = findViewById(R.id.numberDisplay)
        objectsDisplay = findViewById(R.id.objectsDisplay)
        lessonText = findViewById(R.id.lessonText)
        speakButton = findViewById(R.id.speakButton)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        slideIndicator = findViewById(R.id.slideIndicator)
    }

    private fun setupClickListeners() {
        // Кнопка "Назад"
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Кнопка озвучивания
        speakButton.setOnClickListener {
            speakCurrentLesson()
        }

        // Кнопки навигации
        prevButton.setOnClickListener {
            if (currentSlide > 0) {
                currentSlide--
                updateSlide()
                animateSlideTransition()
                speakCurrentLesson() // Озвучиваем при переходе на предыдущий слайд
            }
        }

        nextButton.setOnClickListener {
            if (currentSlide < totalSlides - 1) {
                // Переход к следующему слайду
                currentSlide++
                updateSlide()
                animateSlideTransition()
                speakCurrentLesson() // Озвучиваем при переходе на следующий слайд
            } else {
                // На последнем слайде - переход к началу (слайд 0)
                currentSlide = 0
                updateSlide()
                animateSlideTransition()
                speakCurrentLesson() // Озвучиваем при переходе к началу
            }
        }
    }

    private lateinit var gestureDetector: GestureDetectorCompat

    private fun setupSwipeGestures() {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val start = e1 ?: return false
                val diffX = e2.x - start.x
                val diffY = e2.y - start.y
                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)
                    && kotlin.math.abs(diffX) > SWIPE_THRESHOLD
                    && kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    return if (diffX > 0) {
                        // Смахивание вправо -> предыдущий слайд
                        prevButton.performClick()
                        true
                    } else {
                        // Смахивание влево -> следующий слайд
                        nextButton.performClick()
                        true
                    }
                }
                return false
            }
        }
        this.gestureDetector = GestureDetectorCompat(this, listener)
        // Вешаем слушатель на карточку с цифрой и предметами
        val swipeArea = findViewById<android.view.View>(R.id.contentCard)
        swipeArea.setOnTouchListener { v, event ->
            val consumed = gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            consumed
        }
    }

    private fun updateSlide() {
        val slideData = numberData[currentSlide]

        // Обновляем цифру
        numberDisplay.text = slideData.number.toString()

        // Обновляем объекты
        if (slideData.number == 0) {
            // Для нуля ничего не отображаем
            objectsDisplay.text = ""
        } else {
            // Для остальных чисел генерируем случайные предметы
            val randomObjects = generateRandomObjects(slideData.number)
            objectsDisplay.text = randomObjects
        }

        // Изменяем размер текста для объектов в зависимости от числа
        val textSize = if (slideData.number == 9) {
            40f // Уменьшенный размер для цифры 9
        } else {
            48f // Обычный размер для остальных цифр
        }
        objectsDisplay.textSize = textSize

        // Обновляем текст урока
        if (slideData.number == 0) {
            lessonText.text = slideData.lesson
        } else {
            val objectDescription = getObjectNameWithCount(slideData.number, currentObjectType)
            lessonText.text = slideData.lesson.replace("{OBJECT_DESCRIPTION}", objectDescription)
        }

        // Обновляем индикатор слайдов
        slideIndicator.text = "${currentSlide + 1} / $totalSlides"

        // Обновляем состояние кнопок
        prevButton.isEnabled = currentSlide > 0

        // Логика для кнопки "Далее/В начало"
        if (currentSlide < totalSlides - 1) {
            // Обычные слайды - показываем "Далее"
            nextButton.text = "Далее →"
            nextButton.isEnabled = true
            nextButton.alpha = 1.0f
        } else {
            // Последний слайд - показываем "В начало"
            nextButton.text = "В начало ↺"
            nextButton.isEnabled = true
            nextButton.alpha = 1.0f
        }

        // Меняем цвет кнопки "Назад" в зависимости от состояния
        prevButton.alpha = if (currentSlide > 0) 1.0f else 0.5f
    }

    private fun speakCurrentLesson() {
        val slideData = numberData[currentSlide]

        val textToSpeak = if (slideData.number == 0) {
            "Цифра ${slideData.number}. ${slideData.lesson}"
        } else {
            val objectDescription = getObjectNameWithCount(slideData.number, currentObjectType)
            val lessonWithObject = slideData.lesson.replace("{OBJECT_DESCRIPTION}", objectDescription)
            "Цифра ${slideData.number}. $lessonWithObject"
        }

        speakText(textToSpeak)

        // Анимация кнопки озвучивания
        animateSpeakButton()
    }

    private fun getNumberWord(number: Int): String {
        return when (number) {
            1 -> "один"
            2 -> "два"
            3 -> "три"
            4 -> "четыре"
            5 -> "пять"
            6 -> "шесть"
            7 -> "семь"
            8 -> "восемь"
            9 -> "девять"
            else -> number.toString()
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun animateSlideTransition() {
        // Анимация появления контента
        val fadeIn = ObjectAnimator.ofFloat(numberDisplay, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(numberDisplay, "scaleX", 0.8f, 1f)
        val scaleY = ObjectAnimator.ofFloat(numberDisplay, "scaleY", 0.8f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeIn, scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.start()
    }

    private fun animateSpeakButton() {
        // Анимация пульсации кнопки озвучивания
        val scaleX = ObjectAnimator.ofFloat(speakButton, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(speakButton, "scaleY", 1f, 1.2f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.start()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale("ru", "RU"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts!!.setLanguage(Locale.getDefault())
            }
            // Озвучиваем первый слайд после инициализации TTS
            speakCurrentLesson()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
