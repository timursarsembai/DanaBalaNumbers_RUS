package com.timursarsembayev.danabalanumbers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.GestureDetectorCompat
import android.view.GestureDetector
import android.view.MotionEvent
import java.util.*
import kotlin.random.Random

class NumberIntroductionActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    private var tts: TextToSpeech? = null
    private var currentSlide = 0
    private val totalSlides = 10 // Цифры от 0 до 9
    private var currentObjectType = "" // Текущий тип предмета для озвучивания
    private var isTtsEnabled: Boolean = true

    // UI элементы
    private lateinit var numberDisplay: TextView
    private lateinit var objectsDisplay: TextView
    private lateinit var lessonText: TextView
    private lateinit var speakButton: ImageButton
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var slideIndicator: TextView

    // Данные из ресурсов (локализованные)
    private lateinit var emojiList: Array<String>
    private lateinit var nominativeList: Array<String>
    private lateinit var genSingularList: Array<String>
    private lateinit var genPluralList: Array<String>
    private lateinit var genderList: Array<String>
    private lateinit var emojiToDeclension: Map<String, WordDeclension>

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

    // Структура данных слайда
    data class NumberSlideData(
        val number: Int,
        val lesson: String
    )

    private lateinit var numberData: List<NumberSlideData>

    private fun buildNumberData(): List<NumberSlideData> {
        return listOf(
            NumberSlideData(0, getString(R.string.training_number_intro_lesson_0)),
            NumberSlideData(1, getString(R.string.training_number_intro_lesson_1)),
            NumberSlideData(2, getString(R.string.training_number_intro_lesson_2)),
            NumberSlideData(3, getString(R.string.training_number_intro_lesson_3)),
            NumberSlideData(4, getString(R.string.training_number_intro_lesson_4)),
            NumberSlideData(5, getString(R.string.training_number_intro_lesson_5)),
            NumberSlideData(6, getString(R.string.training_number_intro_lesson_6)),
            NumberSlideData(7, getString(R.string.training_number_intro_lesson_7)),
            NumberSlideData(8, getString(R.string.training_number_intro_lesson_8)),
            NumberSlideData(9, getString(R.string.training_number_intro_lesson_9)),
        )
    }

    private fun randomEmoji(): String {
        if (emojiList.isEmpty()) return ""
        return emojiList[Random.nextInt(emojiList.size)]
    }

    private fun generateRandomObjects(count: Int): String {
        if (count == 0) return ""
        val objectType = randomEmoji()
        currentObjectType = objectType
        val result = StringBuilder()
        val itemsPerRow = when (count) {
            1, 2, 3, 4 -> count
            5, 6 -> 3
            7 -> 4
            8 -> 4
            9 -> 5
            else -> 4
        }
        for (i in 1..count) {
            result.append(objectType)
            if (i % itemsPerRow == 0 && i < count) result.append("\n")
        }
        return result.toString()
    }

    private fun getObjectNameWithCount(count: Int, objectEmoji: String): String {
        val cfgLocales = resources.configuration.locales
        val lang = if (cfgLocales.isEmpty) Locale.getDefault().language else cfgLocales[0].language
        val info = emojiToDeclension[objectEmoji] ?: WordDeclension("предмет", "предмета", "предметов", Gender.MASCULINE)

        return when (lang) {
            // Русский: 1 — именит.; 2-4 — род. ед.; 5-9 — род. мн.; с учётом рода у «один/одна/одно»
            "ru" -> when (count) {
                1 -> {
                    val numeral = when (info.gender) {
                        Gender.MASCULINE -> getString(R.string.number_name_1_masc)
                        Gender.FEMININE -> getString(R.string.number_name_1_fem)
                        Gender.NEUTER -> getString(R.string.number_name_1_neut)
                    }
                    "$numeral ${info.nominative}"
                }
                2 -> getString(R.string.number_name_2) + " " + info.genitiveSingular
                3 -> getString(R.string.number_name_3) + " " + info.genitiveSingular
                4 -> getString(R.string.number_name_4) + " " + info.genitiveSingular
                5 -> getString(R.string.number_name_5) + " " + info.genitivePlural
                6 -> getString(R.string.number_name_6) + " " + info.genitivePlural
                7 -> getString(R.string.number_name_7) + " " + info.genitivePlural
                8 -> getString(R.string.number_name_8) + " " + info.genitivePlural
                9 -> getString(R.string.number_name_9) + " " + info.genitivePlural
                else -> "$count ${info.nominative}"
            }
            // Казахский: после числительных существительное обычно в ед. числе
            "kk" -> {
                val numeral = when (count) {
                    1 -> getString(R.string.number_name_1)
                    2 -> getString(R.string.number_name_2)
                    3 -> getString(R.string.number_name_3)
                    4 -> getString(R.string.number_name_4)
                    5 -> getString(R.string.number_name_5)
                    6 -> getString(R.string.number_name_6)
                    7 -> getString(R.string.number_name_7)
                    8 -> getString(R.string.number_name_8)
                    9 -> getString(R.string.number_name_9)
                    else -> count.toString()
                }
                "$numeral ${info.nominative}"
            }
            // Английский: 1 — ед. число; 2-9 — множественное
            else -> {
                val numeral = when (count) {
                    1 -> getString(R.string.number_name_1)
                    2 -> getString(R.string.number_name_2)
                    3 -> getString(R.string.number_name_3)
                    4 -> getString(R.string.number_name_4)
                    5 -> getString(R.string.number_name_5)
                    6 -> getString(R.string.number_name_6)
                    7 -> getString(R.string.number_name_7)
                    8 -> getString(R.string.number_name_8)
                    9 -> getString(R.string.number_name_9)
                    else -> count.toString()
                }
                val noun = if (count == 1) info.nominative else info.genitivePlural
                "$numeral $noun"
            }
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

        initializeViews()

        // Загружаем локализованные данные объектов
        loadNumberIntroResources()

        // Определяем язык приложения и отключаем TTS для казахского
        val cfgLocales = resources.configuration.locales
        val appLang = if (cfgLocales.isEmpty) Locale.getDefault().language else cfgLocales[0].language
        isTtsEnabled = appLang != "kk"
        if (isTtsEnabled) {
            tts = TextToSpeech(this, this)
        } else {
            // скрываем кнопку озвучивания, чтобы не вводить пользователя в заблуждение
            speakButton.visibility = View.GONE
        }

        numberData = buildNumberData()
        setupClickListeners()
        setupSwipeGestures()
        updateSlide()
    }

    private fun loadNumberIntroResources() {
        emojiList = resources.getStringArray(R.array.number_intro_object_emojis)
        nominativeList = resources.getStringArray(R.array.number_intro_object_nominative)
        // Для en/kk массивы gen_* используются как: singular=единств. форма, plural=множеств. форма
        genSingularList = resources.getStringArray(R.array.number_intro_object_gen_singular)
        genPluralList = resources.getStringArray(R.array.number_intro_object_gen_plural)
        // Гендер нужен только для RU
        genderList = try { resources.getStringArray(R.array.number_intro_object_genders) } catch (_: Exception) { Array(nominativeList.size) { "neuter" } }

        val map = HashMap<String, WordDeclension>(emojiList.size)
        for (i in emojiList.indices) {
            val gender = when (genderList.getOrNull(i)?.lowercase(Locale.ROOT)) {
                "masculine", "m", "masc" -> Gender.MASCULINE
                "feminine", "f", "fem" -> Gender.FEMININE
                else -> Gender.NEUTER
            }
            map[emojiList[i]] = WordDeclension(
                nominative = nominativeList.getOrNull(i) ?: "",
                genitiveSingular = genSingularList.getOrNull(i) ?: "",
                genitivePlural = genPluralList.getOrNull(i) ?: "",
                gender = gender
            )
        }
        emojiToDeclension = map
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
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        speakButton.setOnClickListener {
            if (isTtsEnabled) speakCurrentLesson()
        }
        prevButton.setOnClickListener {
            if (currentSlide > 0) {
                currentSlide--
                updateSlide()
                animateSlideTransition()
                if (isTtsEnabled) speakCurrentLesson()
            }
        }
        nextButton.setOnClickListener {
            if (currentSlide < totalSlides - 1) {
                currentSlide++
                updateSlide()
                animateSlideTransition()
                if (isTtsEnabled) speakCurrentLesson()
            } else {
                currentSlide = 0
                updateSlide()
                animateSlideTransition()
                if (isTtsEnabled) speakCurrentLesson()
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
        val swipeArea = findViewById<View>(R.id.contentCard)
        swipeArea.setOnTouchListener { v, event ->
            val consumed = gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            consumed
        }
    }

    private fun updateSlide() {
        val slideData = numberData[currentSlide]
        numberDisplay.text = slideData.number.toString()
        if (slideData.number == 0) {
            objectsDisplay.text = ""
        } else {
            val randomObjects = generateRandomObjects(slideData.number)
            objectsDisplay.text = randomObjects
        }
        val textSize = if (slideData.number == 9) { 40f } else { 48f }
        objectsDisplay.textSize = textSize
        if (slideData.number == 0) {
            lessonText.text = slideData.lesson
        } else {
            val objectDescription = getObjectNameWithCount(slideData.number, currentObjectType)
            lessonText.text = slideData.lesson.replace("{OBJECT_DESCRIPTION}", objectDescription)
        }
        // Обновляем индикатор слайдов через строковый ресурс
        slideIndicator.text = getString(R.string.slide_indicator_format, currentSlide + 1, totalSlides)
        prevButton.isEnabled = currentSlide > 0
        if (currentSlide < totalSlides - 1) {
            nextButton.text = getString(R.string.training_next_with_arrow)
            nextButton.isEnabled = true
            nextButton.alpha = 1.0f
        } else {
            nextButton.text = getString(R.string.training_to_start_with_arrow)
            nextButton.isEnabled = true
            nextButton.alpha = 1.0f
        }
        prevButton.alpha = if (currentSlide > 0) 1.0f else 0.5f
    }

    private fun speakCurrentLesson() {
        if (!isTtsEnabled) return
        val slideData = numberData[currentSlide]
        val numberNameRes = when (slideData.number) {
            0 -> R.string.number_name_0
            1 -> R.string.number_name_1
            2 -> R.string.number_name_2
            3 -> R.string.number_name_3
            4 -> R.string.number_name_4
            5 -> R.string.number_name_5
            6 -> R.string.number_name_6
            7 -> R.string.number_name_7
            8 -> R.string.number_name_8
            9 -> R.string.number_name_9
            else -> R.string.number_name_0
        }
        val digitIntro = getString(R.string.digit_phrase, getString(numberNameRes))
        val textToSpeak = if (slideData.number == 0) {
            "$digitIntro. ${slideData.lesson}"
        } else {
            val objectDescription = getObjectNameWithCount(slideData.number, currentObjectType)
            val lessonWithObject = slideData.lesson.replace("{OBJECT_DESCRIPTION}", objectDescription)
            "$digitIntro. $lessonWithObject"
        }
        speakText(textToSpeak)
        animateSpeakButton()
    }

    private fun speakText(text: String) {
        if (!isTtsEnabled || tts == null) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun animateSlideTransition() {
        val fadeIn = ObjectAnimator.ofFloat(numberDisplay, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(numberDisplay, "scaleX", 0.8f, 1f)
        val scaleY = ObjectAnimator.ofFloat(numberDisplay, "scaleY", 0.8f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeIn, scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.start()
    }

    private fun animateSpeakButton() {
        val scaleX = ObjectAnimator.ofFloat(speakButton, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(speakButton, "scaleY", 1f, 1.2f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.start()
    }

    private fun chooseBestAvailableLocale(candidates: List<Locale>): Locale? {
        var best: Locale? = null
        var bestScore = 0
        candidates.forEach { loc ->
            val availability = tts?.isLanguageAvailable(loc) ?: TextToSpeech.LANG_NOT_SUPPORTED
            val score = when (availability) {
                TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> 3
                TextToSpeech.LANG_COUNTRY_AVAILABLE -> 2
                TextToSpeech.LANG_AVAILABLE -> 1
                else -> 0
            }
            if (score > bestScore) {
                bestScore = score
                best = loc
            }
        }
        return best
    }

    private fun setTtsLanguageForApp() {
        val cfgLocales = resources.configuration.locales
        val appLocale: Locale = if (cfgLocales.isEmpty) Locale.getDefault() else cfgLocales[0]
        val lang = appLocale.language
        val candidates: List<Locale> = when (lang) {
            "kk" -> listOf(
                Locale.forLanguageTag("kk-KZ"),
                Locale.forLanguageTag("kk"),
                Locale.forLanguageTag("ru-RU")
            )
            "ru" -> listOf(
                Locale.forLanguageTag("ru-RU"),
                Locale.forLanguageTag("ru")
            )
            "en" -> listOf(
                Locale.forLanguageTag("en-US"),
                Locale.forLanguageTag("en-GB"),
                Locale.forLanguageTag("en")
            )
            else -> listOf(appLocale, Locale.getDefault(), Locale.forLanguageTag("en-US"))
        }
        val best = chooseBestAvailableLocale(candidates)
        if (best != null) {
            tts?.language = best
        } else {
            tts?.language = Locale.getDefault()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && isTtsEnabled) {
            setTtsLanguageForApp()
            speakCurrentLesson()
        }
    }

    override fun onDestroy() {
        if (isTtsEnabled) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }
}
