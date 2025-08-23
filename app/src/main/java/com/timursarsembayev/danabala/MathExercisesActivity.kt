package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayout

class MathExercisesActivity : AppCompatActivity() {
    private val billing by lazy { (application as DanaBalApplication).billing }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_math_exercises)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTabs()
        setupExerciseCards()
    }

    private fun setupTabs() {
        val tabs = findViewById<TabLayout>(R.id.tabs)
        val trainings = findViewById<View>(R.id.trainingsContainer)
        val games = findViewById<View>(R.id.gamesContainer)

        // Добавляем вкладки
        val tabTrainings = tabs.newTab().setText("Тренировки")
        val tabGames = tabs.newTab().setText("Игры")
        tabs.addTab(tabTrainings)
        tabs.addTab(tabGames)

        // Начальное состояние: показываем тренировки
        trainings.visibility = View.VISIBLE
        games.visibility = View.GONE

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val showGames = tab.position == 1
                if (showGames) {
                    animateSwitch(show = games, hide = trainings, fromRight = true)
                } else {
                    animateSwitch(show = trainings, hide = games, fromRight = false)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun animateSwitch(show: View, hide: View, fromRight: Boolean) {
        if (show === hide) return
        val distance = 32f * resources.displayMetrics.density
        val duration = 250L

        hide.clearAnimation()
        show.clearAnimation()

        show.alpha = 0f
        show.translationX = if (fromRight) distance else -distance
        show.visibility = View.VISIBLE

        show.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(duration)
            .start()

        hide.animate()
            .alpha(0f)
            .translationX(if (fromRight) -distance else distance)
            .setDuration(duration)
            .withEndAction {
                hide.visibility = View.GONE
                hide.alpha = 1f
                hide.translationX = 0f
            }
            .start()
    }

    private fun openOrPaywall(clazz: Class<*>) {
        val intent = if (billing.isPremium()) Intent(this, clazz) else Intent(this, PaywallActivity::class.java)
        startActivity(intent)
    }

    private fun setupExerciseCards() {
        // Бесплатно: первые 4 тренировки
        findViewById<CardView>(R.id.cardNumberIntroduction).setOnClickListener {
            startActivity(Intent(this, NumberIntroductionActivity::class.java))
        }
        findViewById<CardView>(R.id.cardNumbers).setOnClickListener {
            startActivity(Intent(this, NumberRecognitionActivity::class.java))
        }
        findViewById<CardView>(R.id.cardCounting).setOnClickListener {
            startActivity(Intent(this, CountingActivity::class.java))
        }
        findViewById<CardView>(R.id.cardObjectCounting).setOnClickListener {
            startActivity(Intent(this, ObjectCountingActivity::class.java))
        }

        // Остальные тренировки — только в полной версии
        findViewById<CardView>(R.id.cardMatching).setOnClickListener {
            openOrPaywall(MatchingActivity::class.java)
        }
        findViewById<CardView>(R.id.cardAudioMatching).setOnClickListener {
            openOrPaywall(AudioMatchingActivity::class.java)
        }
        findViewById<CardView>(R.id.cardAscendingSequence).setOnClickListener {
            openOrPaywall(AscendingSequenceActivity::class.java)
        }
        findViewById<CardView>(R.id.cardDescendingSequence).setOnClickListener {
            openOrPaywall(DescendingSequenceActivity::class.java)
        }
        findViewById<CardView>(R.id.cardKidsComparison).setOnClickListener {
            openOrPaywall(KidsComparisonActivity::class.java)
        }
        findViewById<CardView>(R.id.cardNumberComparison).setOnClickListener {
            openOrPaywall(NumberComparisonActivity::class.java)
        }

        // Игры: бесплатно первые 2 (Рисование цифр, Шарики с цифрами)
        findViewById<CardView>(R.id.cardNumberDrawing).setOnClickListener {
            startActivity(Intent(this, NumberDrawingActivity::class.java))
        }
        findViewById<CardView>(R.id.cardBubbleCatch)?.setOnClickListener {
            startActivity(Intent(this, BubbleCatchActivity::class.java))
        }

        // Остальные игры — только в полной версии
        findViewById<CardView>(R.id.cardBlockMatch)?.setOnClickListener {
            openOrPaywall(BlockMatchActivity::class.java)
        }
        findViewById<CardView>(R.id.cardSchulteNumbers)?.setOnClickListener {
            openOrPaywall(SchulteNumbersActivity::class.java)
        }
        findViewById<CardView>(R.id.cardRowError)?.setOnClickListener {
            openOrPaywall(RowErrorActivity::class.java)
        }
        findViewById<CardView>(R.id.cardSnakeMath)?.setOnClickListener {
            openOrPaywall(SnakeMathActivity::class.java)
        }
        findViewById<CardView>(R.id.cardSorter)?.setOnClickListener {
            openOrPaywall(SorterActivity::class.java)
        }
        findViewById<CardView>(R.id.cardSudokuKids)?.setOnClickListener {
            openOrPaywall(SudokuKidsActivity::class.java)
        }
    }
}
