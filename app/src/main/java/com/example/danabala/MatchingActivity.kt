package com.example.danabala

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MatchingActivity : AppCompatActivity() {

    private lateinit var numbersRecyclerView: RecyclerView
    private lateinit var objectsRecyclerView: RecyclerView
    private lateinit var numbersAdapter: NumbersAdapter
    private lateinit var objectsAdapter: ObjectsAdapter
    private lateinit var levelText: TextView
    private lateinit var nextLevelButton: Button
    private lateinit var backButton: Button

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matching)

        initViews()
        initData()
        setupRecyclerViews()
        loadLevel(currentLevel)
    }

    private fun initViews() {
        numbersRecyclerView = findViewById(R.id.numbersRecyclerView)
        objectsRecyclerView = findViewById(R.id.objectsRecyclerView)
        levelText = findViewById(R.id.levelText)
        nextLevelButton = findViewById(R.id.nextLevelButton)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        nextLevelButton.setOnClickListener {
            if (currentLevel < totalLevels) {
                currentLevel++
                loadLevel(currentLevel)
                nextLevelButton.visibility = View.GONE
            } else {
                // Переход к экрану результатов
                showResults()
            }
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
        levelText.text = "$level/$totalLevels"
        completedMatches = 0

        // Перемешиваем элементы для случайного порядка
        val numbers = currentLevelData!!.pairs.map { it.number }.shuffled()
        val objects = currentLevelData!!.pairs.map { it.objects }.shuffled()

        numbersAdapter.updateItems(numbers)
        objectsAdapter.updateItems(objects)

        // Сбрасываем выделения
        clearSelections()
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
        // Отмечаем элементы как сопоставленные
        numberItem.isMatched = true
        objectItem.isMatched = true

        // Анимация успешного совпадения
        animateMatch(selectedNumberView!!, selectedObjectView!!) {
            // После анимации удаляем элементы с эффектом падения
            numbersAdapter.removeItem(numberItem)
            objectsAdapter.removeItem(objectItem)

            completedMatches++
            totalScore += 10

            // Проверяем завершение уровня
            if (completedMatches >= 5) {
                onLevelCompleted()
            }
        }

        clearSelections()
    }

    private fun onIncorrectMatch() {
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
            nextLevelButton.visibility = View.VISIBLE
            nextLevelButton.text = "Следующий уровень"
        } else {
            nextLevelButton.visibility = View.VISIBLE
            nextLevelButton.text = "Показать результаты"
        }
    }

    private fun showResults() {
        val intent = Intent(this, MatchingResultsActivity::class.java)
        intent.putExtra("total_score", totalScore)
        intent.putExtra("completed_levels", currentLevel)
        startActivity(intent)
        finish()
    }
}
