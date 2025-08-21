package com.timursarsembayev.danabalanumbers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class BubbleCatchActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var gameField: FrameLayout
    private lateinit var timerText: TextView
    private lateinit var scoreText: TextView
    private lateinit var targetText: TextView
    private lateinit var progressBar: ProgressBar

    private var score = 0
    private val totalLevels = 10
    private var currentLevel = 0
    private val digitsOrder = (0..9).shuffled().toMutableList()
    private var targetDigit = 0

    private var tts: TextToSpeech? = null
    private var levelTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var spawnRunnable: Runnable? = null
    private val scheduledIncrements = mutableListOf<Runnable>()

    // Градиентный фон
    private lateinit var rootView: View
    private val gradientHandler = Handler(Looper.getMainLooper())
    private var gradientRunnable: Runnable? = null
    private var gradientIndex = 0
    private val gradientIds = intArrayOf(
        R.drawable.child_gradient_yellow,
        R.drawable.child_gradient_blue,
        R.drawable.child_gradient_purple
    )

    // Параметры игры
    private var currentConcurrentLimit: Int = 10
    private val maxConcurrentCapBase: Int = 15

    // Вероятность появления целевой цифры
    private val targetDigitProbability = 0.35f

    // Словесные названия чисел для TTS
    private val numberWords = arrayOf(
        "ноль", "один", "два", "три", "четыре", "пять",
        "шесть", "семь", "восемь", "девять"
    )

    // Короткие фразы для озвучивания событий
    private val correctHitPhrases = listOf(
        "Отлично!", "Верно!", "Правильно!", "Так держать!", "Молодец!"
    )
    private val wrongHitPhrases = listOf(
        "Не та цифра.", "Промах.", "Неверно.", "Попробуй ещё.", "Это не она."
    )
    private val missedPhrases = listOf(
        "Ты упустил нужный шарик.", "Нужный шарик улетел.", "Цель улетела.", "Будь внимательнее."
    )

    // Список активных шариков и апдейт-цикл
    private data class Bubble(
        val view: View,
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val radius: Float,
        val value: Int,
        var clickable: Boolean = true
    )
    private val bubbles = mutableListOf<Bubble>()
    private var updateRunnable: Runnable? = null
    private var lastUpdateMs: Long = 0L

    // Интервал спавна (уменьшается каждые 10 секунд уровня)
    private val baseSpawnIntervalMs = 900L
    private var currentSpawnIntervalMs = baseSpawnIntervalMs
    private val minSpawnIntervalMs = 300L
    private val scheduledSpawnReductions = mutableListOf<Runnable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bubble_catch)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        rootView = findViewById(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        startGradientAnimation()

        tts = TextToSpeech(this, this)
        setupViews()
        setupBackButton()
    }

    // Градиент фона
    private fun startGradientAnimation() {
        stopGradientAnimation()
        fun nextIndex(i: Int) = (i + 1) % gradientIds.size
        gradientRunnable = object : Runnable {
            override fun run() {
                val curr = ContextCompat.getDrawable(this@BubbleCatchActivity, gradientIds[gradientIndex])
                val next = ContextCompat.getDrawable(this@BubbleCatchActivity, gradientIds[nextIndex(gradientIndex)])
                if (curr != null && next != null) {
                    val td = TransitionDrawable(arrayOf(curr, next))
                    rootView.background = td
                    td.isCrossFadeEnabled = true
                    td.startTransition(6000)
                }
                gradientIndex = nextIndex(gradientIndex)
                gradientHandler.postDelayed(this, 6000L)
            }
        }
        gradientHandler.post(gradientRunnable!!)
    }
    private fun stopGradientAnimation() {
        gradientRunnable?.let { gradientHandler.removeCallbacks(it) }
        gradientRunnable = null
    }

    private fun setupViews() {
        gameField = findViewById(R.id.gameField)
        timerText = findViewById(R.id.timerText)
        scoreText = findViewById(R.id.scoreText)
        targetText = findViewById(R.id.targetText)
        progressBar = findViewById(R.id.levelProgressBar)

        scoreText.text = score.toString()
        progressBar.max = LEVEL_DURATION_MS.toInt()
        progressBar.progress = LEVEL_DURATION_MS.toInt()

        findViewById<ImageView>(R.id.speakerButton).setOnClickListener { speakTarget() }
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.forLanguageTag("ru-RU")
            startNextLevel()
        }
    }

    private fun startNextLevel() {
        if (currentLevel >= totalLevels) { finishGame(); return }

        // Настройка цели уровня
        targetDigit = digitsOrder[currentLevel]
        targetText.text = targetDigit.toString()
        speakTarget()

        // Таймер уровня
        levelTimer?.cancel()
        levelTimer = object : CountDownTimer(LEVEL_DURATION_MS, 100L) {
            override fun onTick(millisUntilFinished: Long) { updateTimerUi(millisUntilFinished) }
            override fun onFinish() {
                updateTimerUi(0)
                stopSpawning()
                clearAllBubbles()
                currentLevel++
                gameField.postDelayed({ startNextLevel() }, 600)
            }
        }.start()

        // Динамика одновременных шариков на уровне: стартуем с 10
        currentConcurrentLimit = 10
        scheduleConcurrencyIncrements()

        // Сброс и планирование уменьшения интервала спавна каждые 3 секунды
        currentSpawnIntervalMs = baseSpawnIntervalMs
        scheduleSpawnIntervalReductions()

        // Старт апдейт-цикла и спавна
        startUpdateLoop()
        startSpawning()
    }

    private fun updateTimerUi(millisUntilFinished: Long) {
        val seconds = (millisUntilFinished / 1000).toInt()
        timerText.text = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
        progressBar.progress = millisUntilFinished.toInt()
    }

    private fun scheduleConcurrencyIncrements() {
        cancelScheduledIncrements()
        val stepMs = 10_000L
        var delay = stepMs
        repeat(10) {
            val r = Runnable { currentConcurrentLimit = min(currentConcurrentLimit + 1, maxConcurrentCapBase) }
            scheduledIncrements += r
            handler.postDelayed(r, delay)
            delay += stepMs
        }
    }
    private fun cancelScheduledIncrements() {
        scheduledIncrements.forEach { handler.removeCallbacks(it) }
        scheduledIncrements.clear()
    }

    private fun scheduleSpawnIntervalReductions() {
        cancelScheduledSpawnReductions()
        var delay = 3_000L
        while (delay < LEVEL_DURATION_MS) {
            val r = Runnable {
                currentSpawnIntervalMs = (currentSpawnIntervalMs - 100L).coerceAtLeast(minSpawnIntervalMs)
            }
            scheduledSpawnReductions += r
            handler.postDelayed(r, delay)
            delay += 3_000L
        }
    }
    private fun cancelScheduledSpawnReductions() {
        if (scheduledSpawnReductions.isNotEmpty()) {
            scheduledSpawnReductions.forEach { handler.removeCallbacks(it) }
            scheduledSpawnReductions.clear()
        }
    }

    private fun startSpawning() {
        stopSpawning()
        spawnRunnable = object : Runnable {
            override fun run() {
                val active = bubbles.size
                if (active < currentConcurrentLimit) {
                    val deficit = (currentConcurrentLimit - active).coerceAtLeast(0)
                    if (deficit > 0) spawnBubbles(deficit)
                }
                handler.postDelayed(this, currentSpawnIntervalMs)
            }
        }
        handler.post(spawnRunnable!!)
    }
    private fun stopSpawning() {
        spawnRunnable?.let { handler.removeCallbacks(it) }
        spawnRunnable = null
        cancelScheduledIncrements()
        cancelScheduledSpawnReductions()
    }

    private fun startUpdateLoop() {
        if (updateRunnable != null) return
        lastUpdateMs = SystemClock.uptimeMillis()
        updateRunnable = object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                val dt = ((now - lastUpdateMs).coerceAtMost(50)).toFloat() / 1000f
                lastUpdateMs = now
                stepPhysics(dt)
                handler.postDelayed(this, 16L)
            }
        }
        handler.post(updateRunnable!!)
    }
    private fun stopUpdateLoop() { updateRunnable?.let { handler.removeCallbacks(it) }; updateRunnable = null }

    private fun stepPhysics(dt: Float) {
        if (gameField.width == 0 || gameField.height == 0) return
        val width = gameField.width

        // Обновление позиций
        for (b in bubbles) {
            b.x += b.vx * dt
            b.y += b.vy * dt
            // столкновения со стенами (по X), небольшое затухание
            if (b.x < 0f) { b.x = 0f; b.vx = -b.vx * 0.9f }
            val maxX = (width - b.radius * 2)
            if (b.x > maxX) { b.x = maxX; b.vx = -b.vx * 0.9f }
        }

        // Столкновения между шарами (упругие, почти)
        for (i in 0 until bubbles.size) {
            val a = bubbles[i]
            for (j in i + 1 until bubbles.size) {
                val c = bubbles[j]
                val ax = a.x + a.radius
                val ay = a.y + a.radius
                val cx = c.x + c.radius
                val cy = c.y + c.radius
                val dx = cx - ax
                val dy = cy - ay
                val dist = hypot(dx, dy)
                val minDist = a.radius + c.radius + dpf(6f) // небольшой зазор
                if (dist < minDist && dist > 0f) {
                    // Раздвигаем на половины перекрытия
                    val overlap = (minDist - dist)
                    val nx = dx / dist
                    val ny = dy / dist
                    a.x -= nx * (overlap / 2f)
                    a.y -= ny * (overlap / 2f)
                    c.x += nx * (overlap / 2f)
                    c.y += ny * (overlap / 2f)
                    // Импульс по ��ормали (равные массы)
                    val rvx = c.vx - a.vx
                    val rvy = c.vy - a.vy
                    val vn = rvx * nx + rvy * ny
                    if (vn < 0f) {
                        val e = 0.8f // коэффициент реституции
                        val jImp = -(1 + e) * vn / 2f
                        val jx = jImp * nx
                        val jy = jImp * ny
                        a.vx -= jx; a.vy -= jy
                        c.vx += jx; c.vy += jy
                    }
                }
            }
        }

        // Удаление улетевших и обновление view
        val iterator = bubbles.iterator()
        while (iterator.hasNext()) {
            val b = iterator.next()
            if (b.y + b.radius * 2 < 0) {
                if (b.clickable && b.value == targetDigit) {
                    score -= 1
                    if (score < 0) {
                        score = 0
                        scoreText.text = "0"
                        tts?.speak(missedPhrases.random(), TextToSpeech.QUEUE_ADD, null, "missed")
                        triggerGameOver()
                        return
                    } else {
                        scoreText.text = score.toString()
                        tts?.speak(missedPhrases.random(), TextToSpeech.QUEUE_ADD, null, "missed")
                    }
                }
                (b.view.parent as? ViewGroup)?.removeView(b.view)
                iterator.remove()
                continue
            }
            b.view.x = b.x
            b.view.y = b.y
        }
    }

    private fun spawnBubbles(count: Int) {
        val sizes = IntArray(count) { dp(Random.nextInt(84, 126)) }
        gameField.post {
            val startYBase = gameField.height

            // Колонки по ширине
            val maxSizePx = dp(126)
            val laneGap = dp(12) // увеличенный зазор
            val laneWidth = max(maxSizePx + laneGap, 1)
            val lanes = floor(gameField.width.toFloat() / laneWidth).toInt().coerceAtLeast(1)
            val centers = FloatArray(lanes) { i -> (laneWidth / 2f) + i * laneWidth }

            repeat(count) { idx ->
                val size = sizes[idx]
                val startY = startYBase + size
                val endY = -size

                // Подбор X: по свободным колонкам, затем fallback
                val order = (0 until lanes).shuffled()
                var chosenX: Float? = null
                for (li in order) {
                    val cx = centers[li]
                    val xCand = (cx - size / 2f).coerceIn(0f, (gameField.width - size).toFloat())
                    if (!willOverlapStrict(xCand, startY.toFloat(), size)) { chosenX = xCand; break }
                }
                val x = chosenX ?: findFreeX(size, startY)

                // Создание view и модели
                val bubbleView = FrameLayout(this).apply {
                    layoutParams = FrameLayout.LayoutParams(size, size)
                    this.x = x
                    this.y = startY.toFloat()
                    background = createGlossyBubbleDrawable(randomBubbleColor(), size)
                }
                val value = if (Random.nextFloat() < targetDigitProbability) targetDigit else {
                    var v: Int; do { v = Random.nextInt(0, 10) } while (v == targetDigit); v
                }
                val text = TextView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setShadowLayer(4f, 0f, 2f, Color.parseColor("#66000000"))
                    text = value.toString()
                }
                bubbleView.addView(text)

                val radius = size / 2f
                // Базовая скорость по вертикали (px/с) ~ путь / 6с, с разбросом
                val travelDist = (startY - endY).toFloat()
                val baseVy = -travelDist / 6f
                val vy = baseVy * Random.nextDouble(0.85, 1.25).toFloat()
                val vx = dp(8) * Random.nextDouble(-0.6, 0.6).toFloat() // небольшой боковой дрейф

                val model = Bubble(bubbleView, x, startY.toFloat(), vx, vy, radius, value, true)
                bubbleView.isClickable = true
                bubbleView.setOnClickListener {
                    if (!model.clickable) return@setOnClickListener
                    if (model.value == targetDigit) {
                        score += 2
                        scoreText.text = score.toString()
                        tts?.speak(correctHitPhrases.random(), TextToSpeech.QUEUE_ADD, null, "hit_ok")
                        model.clickable = false
                        bubbles.remove(model)
                        animateCorrectBubbleAndRemove(bubbleView)
                    } else {
                        score -= 1
                        if (score < 0) {
                            score = 0
                            scoreText.text = "0"
                            bubbleView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            animateWrongBubble(bubbleView)
                            tts?.speak(wrongHitPhrases.random(), TextToSpeech.QUEUE_ADD, null, "hit_bad")
                            triggerGameOver()
                        } else {
                            scoreText.text = score.toString()
                            bubbleView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            animateWrongBubble(bubbleView)
                            tts?.speak(wrongHitPhrases.random(), TextToSpeech.QUEUE_ADD, null, "hit_bad")
                        }
                    }
                }

                gameField.addView(bubbleView)
                bubbles.add(model)
            }
        }
    }

    // Строгая проверка перекрытий: радиусы + гориз./верт. з��зоры
    private fun willOverlapStrict(xTopLeft: Float, yTopLeft: Float, size: Int): Boolean {
        val cx = xTopLeft + size / 2f
        val cy = yTopLeft + size / 2f
        val r = size / 2f
        val gap = dpf(12f)
        val minH = dpf(10f)
        val minV = dpf(10f)
        for (b in bubbles) {
            val bx = b.x + b.radius
            val by = b.y + b.radius
            val dx = abs(cx - bx)
            val dy = abs(cy - by)
            val centerDist = hypot(dx, dy)
            val radiusSum = r + b.radius + gap
            if (centerDist < radiusSum) return true
            if (dx < minH && dy < minV) return true
        }
        return false
    }

    private fun findFreeX(sizePx: Int, startY: Int): Float {
        val maxX = (gameField.width - sizePx).coerceAtLeast(0)
        repeat(16) {
            val candidate = Random.nextInt(0, maxX + 1)
            if (!willOverlapStrict(candidate.toFloat(), startY.toFloat(), sizePx)) return candidate.toFloat()
        }
        var x = 0
        while (x <= maxX) {
            if (!willOverlapStrict(x.toFloat(), startY.toFloat(), sizePx)) return x.toFloat()
            x += (sizePx / 2).coerceAtLeast(1)
        }
        return Random.nextInt(0, maxX + 1).toFloat()
    }

    private fun animateCorrectBubbleAndRemove(bubble: View) {
        bubble.isClickable = false
        val rotate = ObjectAnimator.ofFloat(bubble, View.ROTATION, 0f, 360f)
        val scaleX = ObjectAnimator.ofFloat(bubble, View.SCALE_X, 1f, 0f)
        val scaleY = ObjectAnimator.ofFloat(bubble, View.SCALE_Y, 1f, 0f)
        AnimatorSet().apply {
            duration = 400
            interpolator = AccelerateInterpolator()
            playTogether(rotate, scaleX, scaleY)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { (bubble.parent as? ViewGroup)?.removeView(bubble) }
            })
            start()
        }
    }

    private fun animateWrongBubble(bubble: View) {
        val sx = ObjectAnimator.ofFloat(bubble, View.SCALE_X, 1f, 0.9f, 1.06f, 1f)
        val sy = ObjectAnimator.ofFloat(bubble, View.SCALE_Y, 1f, 0.9f, 1.06f, 1f)
        AnimatorSet().apply {
            duration = 220
            interpolator = AccelerateDecelerateInterpolator()
            playTogether(sx, sy)
            start()
        }
    }

    private fun clearAllBubbles() {
        bubbles.clear()
        gameField.removeAllViews()
    }

    private fun speakTarget() {
        tts?.speak("Лови цифру ${numberWords[targetDigit]}", TextToSpeech.QUEUE_FLUSH, null, "catch_target")
    }

    private fun randomBubbleColor(): Int {
        val palette = listOf(
            Color.parseColor("#EF5350"),
            Color.parseColor("#AB47BC"),
            Color.parseColor("#5C6BC0"),
            Color.parseColor("#29B6F6"),
            Color.parseColor("#26A69A"),
            Color.parseColor("#66BB6A"),
            Color.parseColor("#FFCA28"),
            Color.parseColor("#FFA726"),
            Color.parseColor("#EC407A")
        )
        return palette[Random.nextInt(palette.size)]
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).roundToInt()
    private fun dpf(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )

    private fun finishGame() {
        val intent = android.content.Intent(this, BubbleCatchResultsActivity::class.java)
        intent.putExtra("SCORE", score)
        startActivity(intent)
        finish()
    }

    private fun triggerGameOver() {
        stopSpawning()
        stopUpdateLoop()
        clearAllBubbles()
        val intent = android.content.Intent(this, BubbleCatchResultsActivity::class.java)
        intent.putExtra("SCORE", score)
        intent.putExtra("GAME_OVER", true)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSpawning()
        stopUpdateLoop()
        levelTimer?.cancel()
        stopGradientAnimation()
        tts?.stop(); tts?.shutdown()
    }

    private fun createGlossyBubbleDrawable(baseColor: Int, sizePx: Int): Drawable {
        return GlossyBalloonDrawable(this, baseColor)
    }

    // Кастомный Drawable: сохраняет форму шара с хвостиком (ic_balloon) и накладывает объёмный градиент и блик
    private class GlossyBalloonDrawable(
        private val context: android.content.Context,
        private val baseColor: Int
    ) : Drawable() {
        private val rectF = RectF()
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
        private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
        private val xferAtop = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)

        override fun draw(canvas: android.graphics.Canvas) {
            val b = bounds
            if (b.isEmpty) return
            rectF.set(b)

            // Подложка: исходный balloon с хвостиком, затонированный базовым цветом
            val balloon = ContextCompat.getDrawable(context, R.drawable.ic_balloon)?.mutate() ?: return
            balloon.setTint(baseColor)
            balloon.bounds = b

            // Градиент тела (радиальный, свет сверху-слева)
            val cx = b.left + b.width() * 0.35f
            val cy = b.top + b.height() * 0.35f
            val radius = max(b.width(), b.height()) * 0.75f
            bodyPaint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(lighten(baseColor, 0.28f), baseColor, darken(baseColor, 0.22f)),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )

            // Блик сверху (линейный градиент)
            highlightPaint.shader = LinearGradient(
                b.left.toFloat(), b.top.toFloat(), b.left.toFloat(), b.top + b.height() * 0.7f,
                intArrayOf(Color.argb(160, 255, 255, 255), Color.argb(40, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )

            // Слой: рисуем форму, затем накладываем градиенты по SRC_ATOP, чтобы сохранить силуэт (включая хвостик)
            val save = canvas.saveLayer(rectF, null)
            balloon.draw(canvas)

            bodyPaint.xfermode = xferAtop
            canvas.drawRect(rectF, bodyPaint)
            bodyPaint.xfermode = null

            highlightPaint.xfermode = xferAtop
            canvas.drawRect(rectF, highlightPaint)
            highlightPaint.xfermode = null

            canvas.restoreToCount(save)
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

        private fun lighten(color: Int, amount: Float): Int {
            val a = Color.alpha(color)
            val r = (Color.red(color) + (255 - Color.red(color)) * amount).toInt().coerceIn(0, 255)
            val g = (Color.green(color) + (255 - Color.green(color)) * amount).toInt().coerceIn(0, 255)
            val b = (Color.blue(color) + (255 - Color.blue(color)) * amount).toInt().coerceIn(0, 255)
            return Color.argb(a, r, g, b)
        }
        private fun darken(color: Int, amount: Float): Int {
            val a = Color.alpha(color)
            val r = (Color.red(color) * (1f - amount)).toInt().coerceIn(0, 255)
            val g = (Color.green(color) * (1f - amount)).toInt().coerceIn(0, 255)
            val b = (Color.blue(color) * (1f - amount)).toInt().coerceIn(0, 255)
            return Color.argb(a, r, g, b)
        }
    }

    companion object { private const val LEVEL_DURATION_MS = 60_000L }
}
