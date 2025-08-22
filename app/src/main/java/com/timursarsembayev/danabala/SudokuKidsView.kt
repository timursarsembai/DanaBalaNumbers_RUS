package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.random.Random

class SudokuKidsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onSolved: (() -> Unit)? = null

    var gridSize: Int = 3
        private set

    private var cellSize = 0f
    private var gridLeft = 0f
    private var gridTop = 0f

    private lateinit var board: Array<IntArray>
    private lateinit var conflictsRow: Array<BooleanArray>
    private lateinit var conflictsCol: Array<BooleanArray>

    // Анимация переворота для одной клетки за раз
    private var flipping = false
    private var flipR = -1
    private var flipC = -1
    private var flipPrevValue = 0
    private var flipProgress = 0f // 0..1
    private var flipAnimator: ValueAnimator? = null

    private val thinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#33000000")
        strokeWidth = 1f * resources.displayMetrics.density
    }
    private val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#66000000")
        strokeWidth = 3f * resources.displayMetrics.density
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(6f * resources.displayMetrics.density, BlurMaskFilter.Blur.NORMAL)
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val conflictPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E53935")
        strokeWidth = 3.5f * resources.displayMetrics.density
    }

    // Диапазон допустимых значений в текущем уровне: [valueStart .. valueEnd]
    private var valueStart = 1
    private var valueEnd = 3

    // Шаг подрешёток для жирных линий (0 — без внутренних линий)
    private var subStep = 0

    // Палитра для цифр 0..9 (жёсткое соответствие: цифра -> цвет)
    private val palette10 = intArrayOf(
        Color.parseColor("#FF5252"), // 0
        Color.parseColor("#FF9800"), // 1
        Color.parseColor("#FFEB3B"), // 2
        Color.parseColor("#4CAF50"), // 3
        Color.parseColor("#00BCD4"), // 4
        Color.parseColor("#3F51B5"), // 5
        Color.parseColor("#9C27B0"), // 6
        Color.parseColor("#795548"), // 7
        Color.parseColor("#8BC34A"), // 8
        Color.parseColor("#03A9F4")  // 9
    )

    fun startNewGame(size: Int) {
        gridSize = size
        // Выбираем диапазон подряд идущих цифр в 0..9 длиной gridSize
        val maxStart = 10 - gridSize
        valueStart = Random.nextInt(0, maxStart + 1)
        valueEnd = valueStart + gridSize - 1

        // Подрешётки: выбираем шаг
        subStep = when (gridSize) {
            3 -> 0
            4 -> 2
            6 -> if (Random.nextBoolean()) 3 else 2 // 4 блока 3x3 или 9 блоков 2x2
            8 -> if (Random.nextBoolean()) 4 else 2 // 4 блока 4x4 или 16 блоков 2x2
            9 -> 3
            else -> 0
        }

        board = Array(gridSize) { r -> IntArray(gridSize) { c -> valueStart + ((r + c) % gridSize) } }
        conflictsRow = Array(gridSize) { BooleanArray(gridSize) }
        conflictsCol = Array(gridSize) { BooleanArray(gridSize) }
        // Небольшая случайная перестановка, чтобы старт был не сразу решён
        repeat(gridSize * 2) {
            val r = (0 until gridSize).random()
            val c = (0 until gridSize).random()
            val steps = (1..gridSize).random()
            val base = board[r][c]
            val offset = (base - valueStart + steps) % gridSize
            board[r][c] = valueStart + offset
        }
        // Гарантия: стартовое поле не должно быть решённым
        var guard = 0
        while (isSolved() && guard < 100) {
            val r = (0 until gridSize).random()
            val c = (0 until gridSize).random()
            board[r][c] = if (board[r][c] < valueEnd) board[r][c] + 1 else valueStart
            guard++
        }
        flipping = false; flipAnimator?.cancel(); flipAnimator = null
        updateConflicts()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cw = w - paddingLeft - paddingRight
        val ch = h - paddingTop - paddingBottom
        cellSize = min(cw / gridSize.toFloat(), ch / gridSize.toFloat())
        val gridW = cellSize * gridSize
        val gridH = cellSize * gridSize
        gridLeft = paddingLeft + (cw - gridW) / 2f
        gridTop = paddingTop + (ch - gridH) / 2f
        textPaint.textSize = cellSize * 0.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            val l = gridLeft + c * cellSize
            val t = gridTop + r * cellSize
            val isConf = conflictsRow[r][c] || conflictsCol[r][c]
            drawCell(canvas, l, t, r, c, isConf)
        }

        // Тонкая сетка
        for (i in 0..gridSize) {
            val x = gridLeft + i * cellSize
            val y = gridTop + i * cellSize
            canvas.drawLine(gridLeft, y, gridLeft + cellSize * gridSize, y, thinPaint)
            canvas.drawLine(x, gridTop, x, gridTop + cellSize * gridSize, thinPaint)
        }

        // Внешняя рамка и жирные линии по шагу subStep
        canvas.drawRect(gridLeft, gridTop, gridLeft + gridSize * cellSize, gridTop + gridSize * cellSize, boldPaint)
        if (subStep > 0) {
            var k = subStep
            while (k < gridSize) {
                val x = gridLeft + k * cellSize
                val y = gridTop + k * cellSize
                canvas.drawLine(x, gridTop, x, gridTop + gridSize * cellSize, boldPaint)
                canvas.drawLine(gridLeft, y, gridLeft + gridSize * cellSize, y, boldPaint)
                k += subStep
            }
        }
    }

    private fun drawCell(canvas: Canvas, left: Float, top: Float, r: Int, c: Int, isConflict: Boolean) {
        val size = cellSize * 0.9f
        val round = cellSize * 0.18f
        val l = left + (cellSize - size) / 2f
        val t = top + (cellSize - size) / 2f
        val rgt = l + size
        val btm = t + size

        // Определяем показываемое значение (для переворота до середины — старое)
        val showingValue = if (flipping && r == flipR && c == flipC && flipProgress < 0.5f) flipPrevValue else board[r][c]
        val baseColor = palette10[showingValue]

        // Тень
        shadowPaint.color = baseColor
        canvas.drawRoundRect(l + 3f, t + 3f, rgt + 3f, btm + 3f, round, round, shadowPaint)

        // Эффект переворота ��о оси X (вертикальный переворот)
        val scale = if (flipping && r == flipR && c == flipC) {
            val s = abs(cos(Math.PI.toFloat() * flipProgress)).coerceAtLeast(0.06f)
            s
        } else 1f

        canvas.save()
        val cx = (l + rgt) / 2f
        val cy = (t + btm) / 2f
        canvas.translate(cx, cy)
        canvas.scale(1f, scale)
        canvas.translate(-cx, -cy)

        // Корпус «кубика» с глянцем
        fillPaint.shader = LinearGradient(
            l, t, rgt, btm,
            lighten(baseColor, 0.25f), darken(baseColor, 0.18f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(l, t, rgt, btm, round, round, fillPaint)

        highlightPaint.shader = LinearGradient(
            l, t, l, t + size * 0.45f,
            Color.argb(140, 255, 255, 255), Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        val inset = size * 0.06f
        canvas.drawRoundRect(l + inset, t + inset, rgt - inset, t + size * 0.45f, round, round, highlightPaint)

        // Цифра
        textPaint.color = Color.WHITE
        val text = showingValue.toString()
        val fm = textPaint.fontMetrics
        val textY = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, cx, textY, textPaint)

        canvas.restore()

        // Рамка конфликта (не меняем цвет ячейки, что��ы другие клетки визуально не «менялись»)
        if (isConflict) {
            canvas.drawRoundRect(l, t, rgt, btm, round, round, conflictPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                if (flipping) return true // во время анимации игнорируем клики
                val c = ((event.x - gridLeft) / cellSize).toInt()
                val r = ((event.y - gridTop) / cellSize).toInt()
                if (r in 0 until gridSize && c in 0 until gridSize) {
                    startFlip(r, c)
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick(); return true
    }

    private fun startFlip(r: Int, c: Int) {
        flipping = true
        flipR = r; flipC = c
        flipPrevValue = board[r][c]
        flipProgress = 0f
        flipAnimator?.cancel()
        flipAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                val p = (va.animatedValue as Float).coerceIn(0f, 1f)
                val midCrossed = flipProgress < 0.5f && p >= 0.5f
                flipProgress = p
                if (midCrossed) {
                    // На середине переворота меняем значение клетки в пределах [valueStart..valueEnd]
                    val cur = board[r][c]
                    board[r][c] = if (cur < valueEnd) cur + 1 else valueStart
                    updateConflicts()
                }
                invalidate()
            }
            // withEndAction недоступен для ValueAnimator; используем doOnEnd
            doOnEnd {
                flipping = false
                if (isSolved()) onSolved?.invoke()
            }
            start()
        }
    }

    private fun updateConflicts() {
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            conflictsRow[r][c] = false
            conflictsCol[r][c] = false
        }
        // строки
        for (r in 0 until gridSize) {
            val counts = HashMap<Int, MutableList<Int>>()
            for (c in 0 until gridSize) {
                counts.getOrPut(board[r][c]) { mutableListOf() }.add(c)
            }
            for ((_, cols) in counts) if (cols.size > 1) for (c in cols) conflictsRow[r][c] = true
        }
        // столбцы
        for (c in 0 until gridSize) {
            val counts = HashMap<Int, MutableList<Int>>()
            for (r in 0 until gridSize) {
                counts.getOrPut(board[r][c]) { mutableListOf() }.add(r)
            }
            for ((_, rows) in counts) if (rows.size > 1) for (r in rows) conflictsCol[r][c] = true
        }
    }

    private fun isSolved(): Boolean {
        val target = (valueStart..valueEnd).toSet()
        for (r in 0 until gridSize) {
            val rowSet = HashSet<Int>()
            for (c in 0 until gridSize) rowSet.add(board[r][c])
            if (rowSet != target) return false
        }
        for (c in 0 until gridSize) {
            val colSet = HashSet<Int>()
            for (r in 0 until gridSize) colSet.add(board[r][c])
            if (colSet != target) return false
        }
        return true
    }

    private fun lighten(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val nr = (r + (255 - r) * factor).roundToInt().coerceIn(0, 255)
        val ng = (g + (255 - g) * factor).roundToInt().coerceIn(0, 255)
        val nb = (b + (255 - b) * factor).roundToInt().coerceIn(0, 255)
        return Color.argb(a, nr, ng, nb)
    }

    private fun darken(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * (1f - factor)).roundToInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - factor)).roundToInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - factor)).roundToInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
}
