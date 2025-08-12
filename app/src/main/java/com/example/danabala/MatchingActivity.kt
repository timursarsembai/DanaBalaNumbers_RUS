package com.example.danabala

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import kotlin.random.Random

class MatchingActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

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

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matching)

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

    private fun speakText(text: String) {
        if (isTtsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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
        selectedNumberView?.setBackgroundResource(0)

        if (selectedNumberItem == item) {
            // Отменяем выделение
            clearSelections()
            return
        }

        selectedNumberItem = item
        selectedNumberView = view

        // Выделяем элемент
        view.setBackgroundResource(android.R.drawable.editbox_background)

        // Проверяем совпадение
        checkMatch()
    }

    private fun onObjectItemClick(item: MatchingItem, view: View) {
        if (item.isMatched) return

        // Убираем предыдущее выделение
        selectedObjectView?.setBackgroundResource(0)

        if (selectedObjectItem == item) {
            // Отменяем выделение
            clearSelections()
            return
        }

        selectedObjectItem = item
        selectedObjectView = view

        // Выделяем элемент
        view.setBackgroundResource(android.R.drawable.editbox_background)

        // Проверяем совпадение
        checkMatch()
    }

    private fun checkMatch() {
        val numberItem = selectedNumberItem
        val objectItem = selectedObjectItem

        if (numberItem != null && objectItem != null) {
            if (numberItem.value == objectItem.value) {
                // Правильное совпадение!
                onCorrectMatch(numberItem, objectItem)
            } else {
                // Неправильное совпадение
                onIncorrectMatch()
            }
        }
    }

    private fun onCorrectMatch(numberItem: MatchingItem, objectItem: MatchingItem) {
        // Увеличиваем счетчик правильных действий
        correctActions++

        // Озвучиваем поощрение
        val randomPhrase = MatchingFeedbackPhrases.correctPhrases[Random.nextInt(MatchingFeedbackPhrases.correctPhrases.size)]
        speakText(randomPhrase)

        // Отмечаем элементы как сопоставленные
        numberItem.isMatched = true
        objectItem.isMatched = true

        // Анимация успешного совпадения
        animateMatch(selectedNumberView!!, selectedObjectView!!) {
            // После анимации удаляем элементы с эффектом падения
            numbersAdapter.removeItem(numberItem)
            objectsAdapter.removeItem(objectItem)

            completedMatches++

            // Проверяем завершение уровня
            if (completedMatches >= 5) {
                onLevelCompleted()
            }
        }

        clearSelections()
    }

    private fun onIncorrectMatch() {
        // Увеличиваем счетчик неправильных действий
        incorrectActions++

        // Озвучиваем подбадривание
        val randomPhrase = MatchingFeedbackPhrases.incorrectPhrases[Random.nextInt(MatchingFeedbackPhrases.incorrectPhrases.size)]
        speakText(randomPhrase)

        // Анимация неправильного совпадения
        animateIncorrectMatch(selectedNumberView!!, selectedObjectView!!)
        clearSelections()
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
        // Анимация тряски для неправильного совпадения
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
        selectedNumberItem = null
        selectedObjectItem = null
        selectedNumberView?.setBackgroundResource(0)
        selectedObjectView?.setBackgroundResource(0)
        selectedNumberView = null
        selectedObjectView = null
    }

    private fun onLevelCompleted() {
        if (currentLevel < totalLevels) {
            // Убираем задержку - сразу переходим на следующий уровень
            currentLevel++
            loadLevel(currentLevel)
        } else {
            // Переход к экрану результатов без задержки
            showResults()
        }
    }

    private fun showResults() {
        // Вычисляем итоговые очки: +10 за правильные действия, -5 за ошибки
        val finalScore = (correctActions * 10) - (incorrectActions * 5)

        val intent = Intent(this, MatchingResultsActivity::class.java)
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
