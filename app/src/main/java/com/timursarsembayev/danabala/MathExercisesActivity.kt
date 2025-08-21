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

    private fun setupExerciseCards() {
        findViewById<CardView>(R.id.cardNumberIntroduction).setOnClickListener {
            val intent = Intent(this, NumberIntroductionActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardNumbers).setOnClickListener {
            val intent = Intent(this, NumberRecognitionActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardCounting).setOnClickListener {
            val intent = Intent(this, CountingActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardObjectCounting).setOnClickListener {
            val intent = Intent(this, ObjectCountingActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardMatching).setOnClickListener {
            val intent = Intent(this, MatchingActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardAudioMatching).setOnClickListener {
            val intent = Intent(this, AudioMatchingActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardNumberDrawing).setOnClickListener {
            val intent = Intent(this, NumberDrawingActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardAscendingSequence).setOnClickListener {
            val intent = Intent(this, AscendingSequenceActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardDescendingSequence).setOnClickListener {
            val intent = Intent(this, DescendingSequenceActivity::class.java)
            startActivity(intent)
        }

        // Новая детская тренировка сравнения
        findViewById<CardView>(R.id.cardKidsComparison).setOnClickListener {
            startActivity(Intent(this, KidsComparisonActivity::class.java))
        }

        findViewById<CardView>(R.id.cardNumberComparison).setOnClickListener {
            val intent = Intent(this, NumberComparisonActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardBubbleCatch).setOnClickListener {
            val intent = Intent(this, BubbleCatchActivity::class.java)
            startActivity(intent)
        }

        // Новая игра: Цифровой ряд
        findViewById<CardView>(R.id.cardBlockMatch)?.setOnClickListener {
            startActivity(Intent(this, BlockMatchActivity::class.java))
        }
    }
}
