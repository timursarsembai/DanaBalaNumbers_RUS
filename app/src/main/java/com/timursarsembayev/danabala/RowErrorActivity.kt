package com.timursarsembayev.danabalanumbers

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random
import java.util.Locale

class RowErrorActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var progressBar: ProgressBar
    private lateinit var questionText: TextView
    private lateinit var hintText: TextView
    private lateinit var checkButton: Button
    private lateinit var nextButton: Button

    private lateinit var container: CardView
    private lateinit var cells: List<TextView>

    private val totalQuestions = 20
    private var currentQuestion = 0
    private var totalCorrect = 0

    private var generatedSequence: MutableList<Int> = mutableListOf()
    private var wrongIndex: Int = -1
    private var selectedIndex: Int = -1

    // TextToSpeech
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_row_error)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // init TTS
        tts = TextToSpeech(this, this)

        initViews()
        generateQuestion()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale("ru"))
            isTtsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            if (isTtsReady) speak("Найди ошибку в ряду")
        }
    }

    private fun speak(text: String) {
        if (isTtsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "row_error")
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        questionText = findViewById(R.id.questionText)
        hintText = findViewById(R.id.hintText)
        checkButton = findViewById(R.id.checkButton)
        nextButton = findViewById(R.id.nextButton)
        container = findViewById(R.id.sequenceCard)

        cells = listOf(
            findViewById(R.id.cell1),
            findViewById(R.id.cell2),
            findViewById(R.id.cell3),
            findViewById(R.id.cell4),
            findViewById(R.id.cell5),
            findViewById(R.id.cell6)
        )

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        cells.forEachIndexed { index, tv ->
            tv.setOnClickListener { onCellTapped(index) }
        }

        checkButton.setOnClickListener { checkAnswer() }
        nextButton.setOnClickListener { nextQuestion() }
    }

    private fun onCellTapped(index: Int) {
        if (!checkButton.isEnabled) return
        selectedIndex = index
        cells.forEach { tv ->
            tv.background = ContextCompat.getDrawable(this, R.drawable.draggable_number_card)
        }
        cells[index].background = ContextCompat.getDrawable(this, R.drawable.number_drop_zone_highlight)
        hintText.text = "Нажми \"Проверить\""
    }

    private fun generateQuestion() {
        checkButton.isEnabled = true
        nextButton.visibility = View.GONE
        selectedIndex = -1
        hintText.text = "Найди ошибку в ряду"
        questionText.text = "Где ошибка в последовательности?"

        // reset styles
        cells.forEach { cell ->
            cell.background = ContextCompat.getDrawable(this, R.drawable.draggable_number_card)
            cell.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }

        val length = cells.size
        val maxStart = 9 - (length - 1)
        val start = Random.nextInt(0, maxStart + 1)
        val base = MutableList(length) { i -> start + i }

        wrongIndex = Random.nextInt(0, length)

        val left = if (wrongIndex > 0) base[wrongIndex - 1] else null
        val right = if (wrongIndex < length - 1) base[wrongIndex + 1] else null

        var wrongValue: Int
        do {
            wrongValue = Random.nextInt(0, 10)
            // Условия:
            // 1) не равен ожидаемому
            // 2) не равен соседя�� (чтобы не было двух одинаковых подряд)
            // 3) не образует локальную пару подряд с левым или правым (чтобы ошибка ��ыла очевиднее)
        } while (
            wrongValue == base[wrongIndex] ||
            (left != null && wrongValue == left) ||
            (right != null && wrongValue == right) ||
            (left != null && wrongValue - left == 1) ||
            (right != null && right - wrongValue == 1)
        )

        generatedSequence = base
        generatedSequence[wrongIndex] = wrongValue

        cells.forEachIndexed { i, tv ->
            tv.text = generatedSequence[i].toString()
        }

        updateProgress()
        speak("Найди неправильный кубик")
    }

    private fun checkAnswer() {
        if (selectedIndex == -1) {
            hintText.text = "Выбери кубик с ошибкой"
            speak("Выбери кубик с ошибкой")
            return
        }
        checkButton.isEnabled = false

        if (selectedIndex == wrongIndex) {
            totalCorrect++
            hintText.text = "Верно! Молодец!"
            cells[selectedIndex].background = ContextCompat.getDrawable(this, R.drawable.number_drop_zone_filled)
            speak("Верно! Молодец!")
        } else {
            hintText.text = "Неверно. Ошибка здесь"
            cells[selectedIndex].background = ContextCompat.getDrawable(this, R.drawable.number_drop_zone)
            cells[wrongIndex].background = ContextCompat.getDrawable(this, R.drawable.number_input_shown)
            speak("Неверно. Попробуем дальше")
        }

        nextButton.visibility = View.VISIBLE
    }

    private fun nextQuestion() {
        currentQuestion++
        if (currentQuestion >= totalQuestions) {
            val intent = android.content.Intent(this, RowErrorResultsActivity::class.java)
            intent.putExtra("TOTAL", totalQuestions)
            intent.putExtra("CORRECT", totalCorrect)
            startActivity(intent)
            finish()
        } else {
            generateQuestion()
        }
    }

    private fun updateProgress() {
        val progress = ((currentQuestion.toFloat() / totalQuestions) * 100).toInt()
        progressBar.progress = progress
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
