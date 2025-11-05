package com.timursarsembayev.danabalanumbers

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayout

class MathExercisesActivity : BaseActivity() {
    private val billing by lazy { (application as DanaBalApplication).billing }
    override val adUnitId: String? get() = getString(R.string.admob_banner_id)

    @SuppressLint("UnknownIdInLayout", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_math_exercises)

        // Пытаемся найти контейнер по имени, чтобы не зависеть от наличия константы R.id.ad_container
        val containerId = resources.getIdentifier("ad_container", "id", packageName)
        var container: ViewGroup? = if (containerId != 0) findViewById(containerId) else null
        if (container == null) {
            // Фолбэк: создаём контейнер программно и добавляем в конец корневого LinearLayout
            val root = findViewById<LinearLayout>(R.id.main)
            val newContainer = FrameLayout(this).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (4 * resources.displayMetrics.density).toInt()
                    bottomMargin = (4 * resources.displayMetrics.density).toInt()
                }
            }
            root.addView(newContainer)
            container = newContainer
        }
        attachBannerIfPossible(container)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTabs()
        setupExerciseCards()
        setupLanguageButton()
    }

    override fun attachBaseContext(newBase: android.content.Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onResume() {
        super.onResume()
        // Обновляем иконку флага при возврате на экран
        updateLanguageButtonFlag()
    }

    private fun setupTabs() {
        val tabs = findViewById<TabLayout>(R.id.tabs)
        val trainings = findViewById<View>(R.id.trainingsContainer)
        val games = findViewById<View>(R.id.gamesContainer)

        // Добавляем вкладки
        val tabTrainings = tabs.newTab().setText(getString(R.string.tab_trainings))
        val tabGames = tabs.newTab().setText(getString(R.string.tab_games))
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
        // Разблокировано: всегда открываем целевую активити, без перехода на Paywall
        val intent = Intent(this, clazz)
        startActivity(intent)
    }

    private fun setupExerciseCards() {
        // Скрываем аудио-сопоставление для казахского языка
        val audioCard = findViewById<CardView>(R.id.cardAudioMatching)
        val currentLang = LocaleManager.getCurrentLanguage(this)
        audioCard.visibility = if (currentLang == LocaleManager.LANGUAGE_KAZAKH) View.GONE else View.VISIBLE

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

        // Остальные тренировки — теперь тоже доступны бесплатно
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

        // Игры: теперь все доступны бесплатно
        findViewById<CardView>(R.id.cardNumberDrawing).setOnClickListener {
            startActivity(Intent(this, NumberDrawingActivity::class.java))
        }
        findViewById<CardView>(R.id.cardBubbleCatch)?.setOnClickListener {
            startActivity(Intent(this, BubbleCatchActivity::class.java))
        }
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

    private fun setupLanguageButton() {
        // Кнопка настроек языка в правом верхнем углу
        val languageButton = findViewById<ImageButton>(R.id.languageButton)
        updateLanguageButtonFlag()
        languageButton?.setOnClickListener {
            val intent = Intent(this, LanguageSettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateLanguageButtonFlag() {
        val languageButton = findViewById<ImageButton>(R.id.languageButton)
        languageButton?.let {
            val currentLang = LocaleManager.getCurrentLanguage(this)
            val flagRes = LocaleManager.getLanguageFlagRes(currentLang)
            it.setImageResource(flagRes)
            it.contentDescription = LocaleManager.getLanguageDisplayName(currentLang)
        }
    }
}
