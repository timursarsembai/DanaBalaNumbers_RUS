package com.example.danabala

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*
import kotlin.random.Random

class ResultsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val fireworkViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация TTS
        tts = TextToSpeech(this, this)

        // Получаем результаты
        val score = intent.getIntExtra("score", 0)
        val totalQuestions = intent.getIntExtra("total", 20)

        setupResults(score, totalQuestions)
        setupButtons()

        // Запускаем анимации с задержкой
        findViewById<View>(R.id.main).postDelayed({
            startFireworksAnimation()
        }, 500)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.forLanguageTag("ru-RU")

            // Озвучиваем поздравление
            val score = intent.getIntExtra("score", 0)
            val total = intent.getIntExtra("total", 20)
            val percentage = (score * 100) / total

            val message = when {
                percentage == 100 -> "Отлично! Ты справился идеально!"
                percentage >= 90 -> "Отлично! Ты справился почти идеально!"
                percentage >= 80 -> "Молодец! Очень хороший результат!"
                percentage >= 70 -> "Молодец! Неплохой результат!"
                percentage >= 50 -> "Хорошо! Продолжай тренироваться!"
                else -> "Неплохо! В следующий раз получится лучше!"
            }

            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "congratulation")
        }
    }

    private fun setupResults(score: Int, total: Int) {
        val percentage = (score * 100) / total
        val stars = calculateStars(percentage)

        // Обновляем статистику - теперь показываем правильные ответы с первого раза
        findViewById<TextView>(R.id.scoreText).text = "Правильных ответов: $score из $total"
        findViewById<TextView>(R.id.percentageText).text = "$percentage%"

        // Настраиваем звездочки
        setupStars(stars)

        // Обновляем сообщение в зависимости от количества звезд
        val messageText = when {
            stars == 5 -> "Превосходно! 🏆"
            stars == 4 -> "Отлично! ⭐"
            stars == 3 -> "Хорошо! 👍"
            stars == 2 -> "Неплохо! 😊"
            else -> "Попробуй ещё! 💪"
        }
        findViewById<TextView>(R.id.congratulationText).text = messageText
    }

    private fun calculateStars(percentage: Int): Int {
        return when {
            percentage >= 90 -> 5  // 90% и выше - 5 звезд
            percentage >= 75 -> 4  // 75-89% - 4 звезды
            percentage >= 60 -> 3  // 60-74% - 3 звезды
            percentage >= 45 -> 2  // 45-59% - 2 звезды
            else -> 1              // менее 45% - 1 звезда
        }
    }

    private fun setupStars(earnedStars: Int) {
        val starViews = listOf(
            findViewById<ImageView>(R.id.star1),
            findViewById<ImageView>(R.id.star2),
            findViewById<ImageView>(R.id.star3),
            findViewById<ImageView>(R.id.star4),
            findViewById<ImageView>(R.id.star5)
        )

        // Анимируем появление звезд по одной
        starViews.forEachIndexed { index, star ->
            star.alpha = 0f
            star.scaleX = 0f
            star.scaleY = 0f

            if (index < earnedStars) {
                // Золотая звезда
                star.setImageResource(android.R.drawable.btn_star_big_on)

                star.postDelayed({
                    animateStarAppearance(star)
                }, (index * 200L) + 1000)
            } else {
                // Серая звезда
                star.setImageResource(android.R.drawable.btn_star_big_off)

                star.postDelayed({
                    star.animate()
                        .alpha(0.3f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }, (index * 200L) + 1000)
            }
        }
    }

    private fun animateStarAppearance(star: ImageView) {
        val scaleX = ObjectAnimator.ofFloat(star, "scaleX", 0f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(star, "scaleY", 0f, 1.2f, 1f)
        val alpha = ObjectAnimator.ofFloat(star, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 400
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun setupButtons() {
        findViewById<CardView>(R.id.retryButton).setOnClickListener {
            // Теперь всегда запускаем математические упражнения
            val intent = Intent(this, NumberRecognitionActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<CardView>(R.id.backToMenuButton).setOnClickListener {
            // Получаем информацию о родительском разделе
            val parentSection = intent.getStringExtra("parentSection") ?: "math"

            val intent = Intent(this, MathExercisesActivity::class.java) // теперь всегда возвращаемся к математике
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun startFireworksAnimation() {
        val rootView = findViewById<View>(R.id.main)

        // Создаем 15 фейерверков
        repeat(15) {
            createFirework(rootView)
        }
    }

    private fun createFirework(parent: View) {
        val firework = View(this)
        firework.setBackgroundResource(android.R.drawable.star_big_on)

        val size = Random.nextInt(20, 40)
        firework.layoutParams = android.widget.FrameLayout.LayoutParams(size, size)

        // Случайная позиция
        val startX = Random.nextFloat() * parent.width
        val startY = parent.height.toFloat()
        val endY = Random.nextFloat() * (parent.height * 0.6f)

        firework.x = startX
        firework.y = startY
        firework.alpha = 0f

        (parent as android.widget.FrameLayout).addView(firework)
        fireworkViews.add(firework)

        // Анимация полета вверх
        val translateY = ObjectAnimator.ofFloat(firework, "y", startY, endY)
        val alpha = ObjectAnimator.ofFloat(firework, "alpha", 0f, 1f, 0f)
        val scaleX = ObjectAnimator.ofFloat(firework, "scaleX", 0.5f, 1.5f, 0f)
        val scaleY = ObjectAnimator.ofFloat(firework, "scaleY", 0.5f, 1.5f, 0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(translateY, alpha, scaleX, scaleY)
        animatorSet.duration = Random.nextLong(1500, 2500)
        animatorSet.startDelay = Random.nextLong(0, 2000)

        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                (parent as android.widget.FrameLayout).removeView(firework)
                fireworkViews.remove(firework)
            }
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        animatorSet.start()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()

        // Очищаем фейерверки
        fireworkViews.clear()

        super.onDestroy()
    }
}
