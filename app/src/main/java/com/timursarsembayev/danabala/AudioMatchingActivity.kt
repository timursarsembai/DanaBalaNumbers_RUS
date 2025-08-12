package com.timursarsembayev.danabalanumbers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import kotlin.random.Random

class AudioMatchingActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var audioButtonsRecyclerView: RecyclerView
    private lateinit var numbersRecyclerView: RecyclerView
    private lateinit var audioButtonsAdapter: AudioButtonsAdapter
    private lateinit var numbersAdapter: NumbersAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: ImageView

    private var currentLevel = 1
    private var totalLevels = 10
    private var levels = mutableListOf<AudioMatchingLevel>()
    private var currentLevelData: AudioMatchingLevel? = null

    private var selectedAudioItem: MatchingItem? = null
    private var selectedNumberItem: MatchingItem? = null
    private var selectedAudioView: View? = null
    private var selectedNumberView: View? = null

    private var completedMatches = 0
    private var correctActions = 0
    private var incorrectActions = 0

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_matching)

        initTTS()
        initViews()
        initData()
        setupRecyclerViews()
        loadLevel(currentLevel)
    }

    private fun initTTS() {
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("ru"))
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun speakNumber(number: Int) {
        if (isTtsReady) {
            val numberText = when (number) {
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
            tts.speak(numberText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun speakText(text: String) {
        if (isTtsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun initViews() {
        audioButtonsRecyclerView = findViewById(R.id.audioButtonsRecyclerView)
        numbersRecyclerView = findViewById(R.id.numbersRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun initData() {
        levels = AudioMatchingGameData.generateAllLevels().toMutableList()
    }

    private fun setupRecyclerViews() {
        audioButtonsAdapter = AudioButtonsAdapter(mutableListOf()) { item, view ->
            onAudioButtonClick(item, view)
        }

        numbersAdapter = NumbersAdapter(mutableListOf()) { item, view ->
            onNumberItemClick(item, view)
        }

        audioButtonsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AudioMatchingActivity)
            adapter = audioButtonsAdapter
        }

        numbersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AudioMatchingActivity)
            adapter = numbersAdapter
        }
    }

    private fun loadLevel(level: Int) {
        currentLevelData = levels[level - 1]
        updateProgressBar()
        completedMatches = 0

        // Перемешиваем элементы для случайного порядка
        val audioButtons = currentLevelData!!.pairs.map { it.number }.shuffled()
        val numbers = currentLevelData!!.pairs.map { it.number }.shuffled()

        audioButtonsAdapter.updateItems(audioButtons)
        numbersAdapter.updateItems(numbers)

        clearSelections()
        resetViewsAlpha()
    }

    private fun updateProgressBar() {
        val progress = (currentLevel * 100) / totalLevels
        progressBar.progress = progress
    }

    private fun resetViewsAlpha() {
        audioButtonsRecyclerView.post {
            for (i in 0 until audioButtonsRecyclerView.childCount) {
                val child = audioButtonsRecyclerView.getChildAt(i)
                child.alpha = 1.0f
                child.scaleX = 1.0f
                child.scaleY = 1.0f
            }
        }

        numbersRecyclerView.post {
            for (i in 0 until numbersRecyclerView.childCount) {
                val child = numbersRecyclerView.getChildAt(i)
                child.alpha = 1.0f
                child.scaleX = 1.0f
                child.scaleY = 1.0f
            }
        }
    }

    private fun onAudioButtonClick(item: MatchingItem, view: View) {
        if (item.isMatched) return

        // Озвучиваем число
        speakNumber(item.value)

        // Убираем предыдущее выделение
        selectedAudioView?.let { prevView ->
            val linearLayout = prevView.findViewById<LinearLayout>(R.id.audioButtonLayout)
            linearLayout?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))
        }

        if (selectedAudioItem == item) {
            clearSelections()
            return
        }

        selectedAudioItem = item
        selectedAudioView = view

        // Выделяем элемент зеленым цветом
        val linearLayout = view.findViewById<LinearLayout>(R.id.audioButtonLayout)
        linearLayout?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_light))

        checkMatch()
    }

    private fun onNumberItemClick(item: MatchingItem, view: View) {
        if (item.isMatched) return

        // Убираем предыдущее выделение
        selectedNumberView?.let { prevView ->
            val numberText = prevView.findViewById<TextView>(R.id.numberText)
            numberText?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.button_color))
        }

        if (selectedNumberItem == item) {
            clearSelections()
            return
        }

        selectedNumberItem = item
        selectedNumberView = view

        // Выделяем элемент зеленым цветом
        val numberText = view.findViewById<TextView>(R.id.numberText)
        numberText?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_light))

        checkMatch()
    }

    private fun checkMatch() {
        val audioItem = selectedAudioItem
        val numberItem = selectedNumberItem

        if (audioItem != null && numberItem != null) {
            if (audioItem.value == numberItem.value) {
                onCorrectMatch(audioItem, numberItem)
            } else {
                onIncorrectMatch()
            }
        }
    }

    private fun onCorrectMatch(audioItem: MatchingItem, numberItem: MatchingItem) {
        correctActions++

        val randomPhrase = MatchingFeedbackPhrases.correctPhrases[Random.nextInt(MatchingFeedbackPhrases.correctPhrases.size)]
        speakText(randomPhrase)

        audioItem.isMatched = true
        numberItem.isMatched = true

        animateMatch(selectedAudioView!!, selectedNumberView!!) {
            audioButtonsAdapter.removeItem(audioItem)
            numbersAdapter.removeItem(numberItem)

            completedMatches++

            if (completedMatches >= 5) {
                onLevelCompleted()
            }
        }

        clearSelections()
    }

    private fun onIncorrectMatch() {
        incorrectActions++

        val randomPhrase = MatchingFeedbackPhrases.incorrectPhrases[Random.nextInt(MatchingFeedbackPhrases.incorrectPhrases.size)]
        speakText(randomPhrase)

        // Выделяем обе карточки красным цветом на полсекунды
        selectedAudioView?.let { view ->
            val linearLayout = view.findViewById<LinearLayout>(R.id.audioButtonLayout)
            linearLayout?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_red_light))
        }

        selectedNumberView?.let { view ->
            val numberText = view.findViewById<TextView>(R.id.numberText)
            numberText?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_red_light))
        }

        // Через 500ms возвращаем зеленое выделение
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            selectedAudioView?.let { view ->
                val linearLayout = view.findViewById<LinearLayout>(R.id.audioButtonLayout)
                linearLayout?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_light))
            }

            selectedNumberView?.let { view ->
                val numberText = view.findViewById<TextView>(R.id.numberText)
                numberText?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_light))
            }
        }, 500)

        animateIncorrectMatch(selectedAudioView!!, selectedNumberView!!)
    }

    private fun animateMatch(view1: View, view2: View, onComplete: () -> Unit) {
        val animator1 = ValueAnimator.ofFloat(1f, 0f)
        val animator2 = ValueAnimator.ofFloat(1f, 0f)

        animator1.duration = 300
        animator2.duration = 300

        animator1.addUpdateListener { animation ->
            val alpha = animation.animatedValue as Float
            view1.alpha = alpha
            view1.scaleX = alpha
            view1.scaleY = alpha
        }

        animator2.addUpdateListener { animation ->
            val alpha = animation.animatedValue as Float
            view2.alpha = alpha
            view2.scaleX = alpha
            view2.scaleY = alpha
        }

        animator1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onComplete()
            }
        })

        animator1.start()
        animator2.start()
    }

    private fun animateIncorrectMatch(view1: View, view2: View) {
        val shake = ValueAnimator.ofFloat(0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        shake.duration = 600

        shake.addUpdateListener { animation ->
            val translateX = animation.animatedValue as Float
            view1.translationX = translateX
            view2.translationX = translateX
        }

        shake.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view1.translationX = 0f
                view2.translationX = 0f
            }
        })

        shake.start()
    }

    private fun clearSelections() {
        selectedAudioItem = null
        selectedNumberItem = null

        selectedAudioView?.let { view ->
            val linearLayout = view.findViewById<LinearLayout>(R.id.audioButtonLayout)
            linearLayout?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))
        }

        selectedNumberView?.let { view ->
            val numberText = view.findViewById<TextView>(R.id.numberText)
            numberText?.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.button_color))
        }

        selectedAudioView = null
        selectedNumberView = null
    }

    private fun onLevelCompleted() {
        if (currentLevel < totalLevels) {
            currentLevel++
            loadLevel(currentLevel)
        } else {
            showResults()
        }
    }

    private fun showResults() {
        val finalScore = (correctActions * 10) - (incorrectActions * 5)

        val intent = Intent(this, AudioMatchingResultsActivity::class.java)
        intent.putExtra("completed_levels", currentLevel)
        intent.putExtra("correct_actions", correctActions)
        intent.putExtra("incorrect_actions", incorrectActions)
        intent.putExtra("final_score", finalScore)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
