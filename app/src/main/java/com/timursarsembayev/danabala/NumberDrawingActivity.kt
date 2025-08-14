package com.timursarsembayev.danabalanumbers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class NumberDrawingActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var currentNumber = 0

    // UI элементы
    private lateinit var drawingView: DrawingView
    private lateinit var numberDisplay: TextView
    private lateinit var instructionText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var colorPalette: RecyclerView
    private lateinit var eraserButton: ImageButton
    private lateinit var eraserSelectionRing: ImageView
    private lateinit var doneButton: Button

    private lateinit var colorAdapter: ColorPaletteAdapter
    private var selectedColor = 0xFF4CAF50.toInt() // Зеленый по умолчанию

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_drawing)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupTextToSpeech()
        setupColorPalette()
        setupDrawingView()
        setupButtons()

        findViewById<ImageView>(R.id.backButton)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Начинаем с цифры 0
        setCurrentNumber(0)
    }

    private fun initializeViews() {
        drawingView = findViewById(R.id.drawingView)
        numberDisplay = findViewById(R.id.numberDisplay)
        instructionText = findViewById(R.id.instructionText)
        progressBar = findViewById(R.id.progressBar)
        colorPalette = findViewById(R.id.colorPalette)
        eraserButton = findViewById(R.id.eraserButton)
        eraserSelectionRing = findViewById(R.id.eraserSelectionRing)
        doneButton = findViewById(R.id.doneButton)
    }

    private fun setupTextToSpeech() {
        tts = TextToSpeech(this, this)
    }

    private fun setupColorPalette() {
        val colors = listOf(
            0xFF4CAF50.toInt(), // Зеленый
            0xFF2196F3.toInt(), // Синий
            0xFFFF9800.toInt(), // Оранжевый
            0xFFE91E63.toInt(), // Розовый
            0xFF9C27B0.toInt(), // Фиолетовый
            0xFFFFEB3B.toInt(), // Желтый
            0xFFFF5722.toInt(), // Красно-оранжевый
            0xFF607D8B.toInt()  // Серо-синий
        )

        colorAdapter = ColorPaletteAdapter(colors) { color ->
            // При выборе цвета — выключаем ластик и переноcим выделение
            drawingView.setEraserMode(false)
            eraserButton.isSelected = false
            eraserSelectionRing.visibility = android.view.View.GONE

            selectedColor = color
            drawingView.setDrawingColor(color)
        }

        colorPalette.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        colorPalette.adapter = colorAdapter

        // Устанавливаем первый цвет как выбранный
        colorAdapter.setSelectedColor(selectedColor)
        drawingView.setDrawingColor(selectedColor)
    }

    private fun setupDrawingView() {
        drawingView.setOnProgressChangedListener { progress ->
            progressBar.progress = (progress * 100).toInt()
            if (progress >= 0.8f && !drawingView.isCompleted()) {
                drawingView.setCompleted(true)
                showCompletionAnimation()
            }
        }
    }

    private fun updateDoneButtonStyle(isNext: Boolean) {
        if (isNext) {
            doneButton.text = getString(R.string.next)
            doneButton.background = AppCompatResources.getDrawable(this, R.drawable.button_success)
        } else {
            doneButton.text = getString(R.string.done)
            doneButton.background = AppCompatResources.getDrawable(this, R.drawable.button_primary)
        }
    }

    private fun animateDoneToNextTransition() {
        // Анимируем кнопку: уменьшаем и делаем прозрачной
        val fadeOut = ObjectAnimator.ofFloat(doneButton, "alpha", 1f, 0f).setDuration(120)
        val scaleOutX = ObjectAnimator.ofFloat(doneButton, "scaleX", 1f, 0.95f).setDuration(120)
        val scaleOutY = ObjectAnimator.ofFloat(doneButton, "scaleY", 1f, 0.95f).setDuration(120)

        // После скрытия меняем текст/фон на «Далее»
        fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                updateDoneButtonStyle(true)
            }
        })

        // Появление с лёгким пружинящим эффектом
        val fadeIn = ObjectAnimator.ofFloat(doneButton, "alpha", 0f, 1f).setDuration(160)
        val scaleInX = ObjectAnimator.ofFloat(doneButton, "scaleX", 0.95f, 1f).setDuration(200)
        val scaleInY = ObjectAnimator.ofFloat(doneButton, "scaleY", 0.95f, 1f).setDuration(200)

        val outSet = AnimatorSet().apply { playTogether(fadeOut, scaleOutX, scaleOutY) }
        val inSet = AnimatorSet().apply { playTogether(fadeIn, scaleInX, scaleInY) }

        AnimatorSet().apply {
            playSequentially(outSet, inSet)
            start()
        }
    }

    private fun setupButtons() {
        eraserButton.setOnClickListener {
            val newMode = !drawingView.isEraserMode()
            drawingView.setEraserMode(newMode)
            eraserButton.isSelected = newMode
            if (newMode) {
                // Переносим «кружок» выбора на ластик
                colorAdapter.clearSelection()
                eraserSelectionRing.visibility = android.view.View.VISIBLE
            } else {
                eraserSelectionRing.visibility = android.view.View.GONE
            }
        }

        doneButton.setOnClickListener {
            if (drawingView.isCompleted()) {
                if (doneButton.text.toString() == "Готово") {
                    // Принятие результата: голосовое поздравление + анимация перехода к «Далее»
                    showSuccessMessage()
                    animateDoneToNextTransition()
                } else {
                    goNextNumberOrFinish()
                }
            } else {
                showEncouragementMessage()
            }
        }
    }

    private fun goNextNumberOrFinish() {
        if (currentNumber < 9) {
            setCurrentNumber(currentNumber + 1)
            updateDoneButtonStyle(false)
            drawingView.setEraserMode(false)
            eraserButton.isSelected = false
            eraserSelectionRing.visibility = android.view.View.GONE
            colorAdapter.setSelectedColor(selectedColor)
        } else {
            // Переходим на экран результатов
            startActivity(Intent(this, NumberDrawingResultsActivity::class.java))
            finish()
        }
    }

    private fun setCurrentNumber(number: Int) {
        currentNumber = number
        numberDisplay.text = number.toString()
        instructionText.text = getString(R.string.draw_number, number)

        drawingView.setNumberOutline(number)
        drawingView.clearDrawing()
        progressBar.progress = 0

        speakNumber(number)
        updateDoneButtonStyle(false)
    }

    private fun speakNumber(number: Int) {
        val numberNames = arrayOf(
            "ноль", "один", "два", "три", "четыре",
            "пять", "шесть", "семь", "восемь", "девять"
        )
        tts?.speak("Цифра ${numberNames[number]}", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun showCompletionAnimation() { /* опционально */ }

    private fun showSuccessMessage() {
        val phrases = arrayOf(
            "Отлично! Очень хорошо!",
            "Замечательно! Молодец!",
            "Превосходно! Продолжай!",
            "Хорошая работа!"
        )
        tts?.speak(phrases.random(), TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun showEncouragementMessage() {
        val phrases = arrayOf(
            "Нужно ещё немного закрасить",
            "Продолжай!",
            "Почти готово!"
        )
        tts?.speak(phrases.random(), TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.forLanguageTag("ru-RU"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
