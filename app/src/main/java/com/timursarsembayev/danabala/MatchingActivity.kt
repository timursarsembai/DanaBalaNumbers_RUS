package com.timursarsembayev.danabalanumbers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import kotlin.random.Random

class MatchingActivity : BaseActivity(), TextToSpeech.OnInitListener {

    override val adUnitId: String? get() = getString(R.string.admob_banner_id)

    private lateinit var numbersRecyclerView: RecyclerView
    private lateinit var objectsRecyclerView: RecyclerView
    private lateinit var numbersAdapter: NumbersAdapter
    private lateinit var objectsAdapter: ObjectsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: ImageView

    private var currentLevel = 1
    private var totalLevels = 10
    private var levels = mutableListOf<MatchingLevel>()
    private var currentLevelData: MatchingLevel? = null

    private var selectedNumberItem: MatchingItem? = null
    private var selectedObjectItem: MatchingItem? = null
    private var selectedNumberView: View? = null
    private var selectedObjectView: View? = null

    private var completedMatches = 0
    private var totalScore = 0
    private var correctActions = 0
    private var incorrectActions = 0

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var allowTts = true

    private lateinit var correctPhrases: Array<String>
    private lateinit var incorrectPhrases: Array<String>

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matching)

        // Подключение баннера
        val bannerContainer = findViewById<ViewGroup?>(R.id.ad_container)
        attachBannerIfPossible(bannerContainer)

        // Определяем текущий язык и отключаем TTS для казахского
        val currentLang = LocaleManager.getCurrentLanguage(this).lowercase(Locale.ROOT)
        allowTts = currentLang != LocaleManager.LANGUAGE_KAZAKH

        initTTS()
        initViews()
        initData()
        setupRecyclerViews()

        // Инициализация локализованных фраз из ресурсов
        correctPhrases = resources.getStringArray(R.array.matching_correct_phrases)
        incorrectPhrases = resources.getStringArray(R.array.matching_incorrect_phrases)

        loadLevel(currentLevel)
    }

    private fun initTTS() {
        if (allowTts) {
            tts = TextToSpeech(this, this)
        }
    }

    override fun onInit(status: Int) {
        if (!allowTts) return
        if (status == TextToSpeech.SUCCESS) {
            val langCode = LocaleManager.getCurrentLanguage(this)
            val locale = when (langCode) {
                LocaleManager.LANGUAGE_RUSSIAN -> Locale.forLanguageTag("ru-RU")
                LocaleManager.LANGUAGE_ENGLISH -> Locale.forLanguageTag("en-US")
                // Для казахского TTS отключен по allowTts
                else -> Locale.forLanguageTag("en-US")
            }
            val result = tts?.setLanguage(locale)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun speakText(text: String) {
        if (allowTts && isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun initViews() {
        numbersRecyclerView = findViewById(R.id.numbersRecyclerView)
        objectsRecyclerView = findViewById(R.id.objectsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun initData() {
        levels = MatchingGameData.generateAllLevels().toMutableList()
    }

    private fun setupRecyclerViews() {
        numbersAdapter = NumbersAdapter(mutableListOf()) { item, view ->
            onNumberItemClick(item, view)
        }

        objectsAdapter = ObjectsAdapter(mutableListOf()) { item, view ->
            onObjectItemClick(item, view)
        }

        numbersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MatchingActivity)
            adapter = numbersAdapter
        }

        objectsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MatchingActivity)
            adapter = objectsAdapter
        }
    }

    private fun loadLevel(level: Int) {
        currentLevelData = levels[level - 1]
        updateProgressBar()
        completedMatches = 0

        // Перемешиваем элементы для случайного порядка
        val numbers = currentLevelData!!.pairs.map { it.number }.shuffled()
        val objects = currentLevelData!!.pairs.map { it.objects }.shuffled()

        numbersAdapter.updateItems(numbers)
        objectsAdapter.updateItems(objects)

        // Сбрасываем выделения
        clearSelections()

        // Сбрасываем прозрачность всех view после анимаций
        resetViewsAlpha()
    }

    private fun updateProgressBar() {
        val progress = (currentLevel * 100) / totalLevels
        progressBar.progress = progress
    }

    private fun resetViewsAlpha() {
        // Сбрасываем прозрачность для всех элементов RecyclerView
        numbersRecyclerView.post {
            for (i in 0 until numbersRecyclerView.childCount) {
                val child = numbersRecyclerView.getChildAt(i)
                child.alpha = 1.0f
                child.scaleX = 1.0f
                child.scaleY = 1.0f
            }
        }

        objectsRecyclerView.post {
            for (i in 0 until objectsRecyclerView.childCount) {
                val child = objectsRecyclerView.getChildAt(i)
                child.alpha = 1.0f
                child.scaleX = 1.0f
                child.scaleY = 1.0f
            }
        }
    }

    private fun onNumberItemClick(item: MatchingItem, view: View) {
        if (item.isMatched) return

        // Убираем предыдущее выделение
        selectedNumberView?.let { prevView ->
            val numberText = prevView.findViewById<TextView>(R.id.numberText)
            numberText?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.button_color))
        }

        if (selectedNumberItem == item) {
            // Отменяем выделение
            clearSelections()
            return
        }

        selectedNumberItem = item
        selectedNumberView = view

        // Выделяем элемент СРАЗУ зеленым цветом
        val numberText = view.findViewById<TextView>(R.id.numberText)
        numberText?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_light))

        // Проверяем совпадение только если выбраны обе карточки
        checkMatch()
    }

    private fun onObjectItemClick(item: MatchingItem, view: View) {
        if (item.isMatched) return

        // Убираем предыдущее выделение
        selectedObjectView?.let { prevView ->
            val linearLayout = prevView.findViewById<LinearLayout>(R.id.objectsLayout)
            linearLayout?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))
        }

        if (selectedObjectItem == item) {
            // Отменяем выделение
            clearSelections()
            return
        }

        selectedObjectItem = item
        selectedObjectView = view

        // Выделяем элемент СРАЗУ зеленым цветом
        val linearLayout = view.findViewById<LinearLayout>(R.id.objectsLayout)
        linearLayout?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_light))

        // Проверяем совпадение только если выбраны обе карточки
        checkMatch()
    }

    private fun checkMatch() {
        val numberItem = selectedNumberItem
        val objectItem = selectedObjectItem

        if (numberItem != null && objectItem != null) {
            if (numberItem.value == objectItem.value) {
                onCorrectMatch(numberItem, objectItem)
            } else {
                onIncorrectMatch()
            }
        }
    }

    private fun onCorrectMatch(numberItem: MatchingItem, objectItem: MatchingItem) {
        correctActions++

        // Озвучиваем поощрение (локализовано)
        val randomPhrase = correctPhrases[Random.nextInt(correctPhrases.size)]
        speakText(randomPhrase)

        numberItem.isMatched = true
        objectItem.isMatched = true

        // Анимация успешного совпадения
        animateMatch(selectedNumberView!!, selectedObjectView!!) {
            // После анимации удаляем элементы с эффектом падения
            numbersAdapter.removeItem(numberItem)
            objectsAdapter.removeItem(objectItem)

            completedMatches++
            totalScore += 10

            if (completedMatches >= currentLevelData!!.pairs.size) {
                // Переходим на следующий уровень или завершаем тренировку
                currentLevel++
                if (currentLevel > totalLevels) {
                    openResults()
                } else {
                    loadLevel(currentLevel)
                }
            }

            // Сбрасываем выборы после совпадения
            clearSelections()
        }
    }

    private fun onIncorrectMatch() {
        incorrectActions++
        // Проигрываем отрицательную анимацию и/или подсказки при желании
        animateShake(selectedNumberView)
        animateShake(selectedObjectView)

        // Озвучиваем поддерживающую фразу (локализовано)
        val randomPhrase = incorrectPhrases[Random.nextInt(incorrectPhrases.size)]
        speakText(randomPhrase)

        // Сбрасываем выделения, чтобы пользователь выбрал заново
        clearSelections()
    }

    private fun animateMatch(numberView: View, objectView: View, onEnd: () -> Unit) {
        val duration = 300L

        val numberAnimator = ValueAnimator.ofFloat(1f, 0f)
        numberAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            numberView.alpha = value
            numberView.scaleX = value
            numberView.scaleY = value
        }

        val objectAnimator = ValueAnimator.ofFloat(1f, 0f)
        objectAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            objectView.alpha = value
            objectView.scaleX = value
            objectView.scaleY = value
        }

        numberAnimator.duration = duration
        objectAnimator.duration = duration

        objectAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd()
            }
        })

        numberAnimator.start()
        objectAnimator.start()
    }

    private fun animateShake(view: View?) {
        view ?: return
        val animator = ValueAnimator.ofFloat(0f, 20f, -20f, 10f, -10f, 0f)
        animator.duration = 400
        animator.addUpdateListener { animation ->
            view.translationX = animation.animatedValue as Float
        }
        animator.start()
    }

    private fun clearSelections() {
        selectedNumberItem = null
        selectedObjectItem = null

        selectedNumberView?.let { view ->
            val numberText = view.findViewById<TextView>(R.id.numberText)
            numberText?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.button_color))
        }

        selectedObjectView?.let { view ->
            val linearLayout = view.findViewById<LinearLayout>(R.id.objectsLayout)
            linearLayout?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))
        }

        selectedNumberView = null
        selectedObjectView = null
    }

    private fun openResults() {
        val intent = Intent(this, MatchingResultsActivity::class.java)
        intent.putExtra("TOTAL_SCORE", totalScore)
        intent.putExtra("CORRECT_ACTIONS", correctActions)
        intent.putExtra("INCORRECT_ACTIONS", incorrectActions)
        startActivity(intent)
        finish()
    }
}
