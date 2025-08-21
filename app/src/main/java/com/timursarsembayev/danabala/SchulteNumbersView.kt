package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.*
import kotlin.random.Random

class SchulteNumbersView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onTileTap: ((digit: Int) -> Unit)? = null

    private val cols = 5
    private val rows = 7

    private var cellSize = 0f
    private var gridLeft = 0f
    private var gridTop = 0f

    private data class Tile(
        var digit: Int,
        var color: Int,
        var lightText: Boolean
    )

    // Текущая сетка
    private val tiles: Array<Array<Tile>> = Array(rows) { Array(cols) { Tile(0, Color.GRAY, true) } }

    // Следующая сетка для смены на полпути «переворота»
    private var nextDigits: Array<IntArray>? = null
    private var nextColors: IntArray? = null
    private var nextLight: Array<BooleanArray>? = null

    // Пейнты в стиле BlockMatch
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.WHITE
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = 0x22000000
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glossyShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(6f * resources.displayMetrics.density, BlurMaskFilter.Blur.NORMAL)
    }

    private val blockColors = intArrayOf(
        Color.parseColor("#FF5252"),
        Color.parseColor("#FF9800"),
        Color.parseColor("#FFEB3B"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#00BCD4"),
        Color.parseColor("#3F51B5"),
        Color.parseColor("#9C27B0"),
        Color.parseColor("#795548")
    )

    // Анимация «переворота» всей сетки
    private var flipAnimator: ValueAnimator? = null
    private var flipProgress = 0f // 0..1, масштаб X = |cos(pi * p)|
    private var swappedMid = false

    private enum class FlipDir { LR, RL, TB, BT }
    private val flipDirs: Array<Array<FlipDir>> = Array(rows) { Array(cols) { FlipDir.LR } }

    fun resetGrid(targetDigit: Int) {
        // Наполнить все ячейки случайными цифрами, одна — цель
        val total = cols * rows
        val targetIndex = Random.nextInt(total)
        var k = 0
        val nextColorsLocal = IntArray(total) { blockColors[Random.nextInt(blockColors.size)] }
        for (r in 0 until rows) for (c in 0 until cols) {
            val color = nextColorsLocal[k]
            val lightText = Random.nextBoolean()
            val digit = if (k == targetIndex) targetDigit else randomDigitExcept(targetDigit)
            tiles[r][c].apply {
                this.digit = digit
                this.color = color
                this.lightText = lightText
            }
            k++
        }
        invalidate()
    }

    fun flipToNext(targetDigit: Int) {
        // Подготовим будущие значения
        val total = cols * rows
        val targetIndex = Random.nextInt(total)
        val nDigits = Array(rows) { IntArray(cols) }
        val nLight = Array(rows) { BooleanArray(cols) }
        val nColorsFlat = IntArray(total) { blockColors[Random.nextInt(blockColors.size)] }
        var k = 0
        for (r in 0 until rows) for (c in 0 until cols) {
            nDigits[r][c] = if (k == targetIndex) targetDigit else randomDigitExcept(targetDigit)
            nLight[r][c] = Random.nextBoolean()
            k++
        }
        nextDigits = nDigits
        nextLight = nLight
        nextColors = nColorsFlat

        // Расставляем случайные направления для каждой плитки
        for (r in 0 until rows) for (c in 0 until cols) {
            flipDirs[r][c] = when (Random.nextInt(4)) {
                0 -> FlipDir.LR
                1 -> FlipDir.RL
                2 -> FlipDir.TB
                else -> FlipDir.BT
            }
        }

        // Запуск анимации переворота
        flipAnimator?.cancel()
        swappedMid = false
        flipAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                flipProgress = (va.animatedValue as Float).coerceIn(0f, 1f)
                // На середине меняем содержимое
                if (!swappedMid && flipProgress >= 0.5f) {
                    applyNextSet()
                    swappedMid = true
                }
                invalidate()
            }
            start()
        }
    }

    private fun applyNextSet() {
        val nDigits = nextDigits ?: return
        val nLight = nextLight ?: return
        val nColorsFlat = nextColors ?: return
        var k = 0
        for (r in 0 until rows) for (c in 0 until cols) {
            val t = tiles[r][c]
            t.digit = nDigits[r][c]
            t.lightText = nLight[r][c]
            t.color = nColorsFlat[k]
            k++
        }
        nextDigits = null
        nextLight = null
        nextColors = null
    }

    private fun randomDigitExcept(except: Int): Int {
        var d: Int
        do { d = Random.nextInt(0, 10) } while (d == except)
        return d
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cw = w - paddingLeft - paddingRight
        val ch = h - paddingTop - paddingBottom
        cellSize = min(cw / cols.toFloat(), ch / rows.toFloat())
        val gridW = cellSize * cols
        val gridH = cellSize * rows
        gridLeft = paddingLeft + (cw - gridW) / 2f
        gridTop = paddingTop + (ch - gridH) / 2f
        textPaint.textSize = cellSize * 0.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (r in 0 until rows) for (c in 0 until cols) {
            val l = gridLeft + c * cellSize
            val t = gridTop + r * cellSize
            // Фон клетки (тонкая сетка)
            val gridBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x10FFFFFF }
            canvas.drawRect(l, t, l + cellSize, t + cellSize, gridBg)
            canvas.drawRect(l, t, l + cellSize, t + cellSize, borderPaint)

            val tile = tiles[r][c]
            drawBlock(canvas, l, t, tile, flipDirs[r][c])
        }
    }

    private fun drawBlock(canvas: Canvas, left: Float, top: Float, tile: Tile, dir: FlipDir) {
        val baseSize = cellSize * 0.92f
        val round = cellSize * 0.18f
        val minScale = 0.02f

        // Масштабы для осей по косинусу прогресса
        val scaleVal = abs(cos(Math.PI.toFloat() * flipProgress)).coerceAtLeast(minScale)

        // Базовые границы без анимации
        val baseLeft = left + (cellSize - baseSize) / 2f
        val baseTop = top + (cellSize - baseSize) / 2f
        val baseRight = baseLeft + baseSize
        val baseBottom = baseTop + baseSize

        // Вычисляем прямоугольник с нужным якорем
        var l = baseLeft
        var t = baseTop
        var r = baseRight
        var b = baseBottom
        when (dir) {
            FlipDir.LR -> { // фиксируем левую грань, сжимаем по ширине вправо
                val w = baseSize * scaleVal
                r = l + w
            }
            FlipDir.RL -> { // фиксируем правую грань, сжимаем по ширине влево
                val w = baseSize * scaleVal
                l = r - w
            }
            FlipDir.TB -> { // фиксируем верх, сжимаем вниз
                val h = baseSize * scaleVal
                b = t + h
            }
            FlipDir.BT -> { // фиксируем низ, сжимаем вверх
                val h = baseSize * scaleVal
                t = b - h
            }
        }

        // Тень/блик и тело плитки (как в BlockMatch)
        glossyShadow.color = tile.color
        canvas.drawRoundRect(l + 3f, t + 3f, r + 3f, b + 3f, round, round, glossyShadow)

        fillPaint.shader = LinearGradient(
            l, t, r, b,
            lighten(tile.color, 0.25f), darken(tile.color, 0.15f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(l, t, r, b, round, round, fillPaint)

        highlightPaint.shader = LinearGradient(
            l, t, l, t + (b - t) * 0.45f,
            Color.argb(120, 255, 255, 255), Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        val inset = baseSize * 0.06f
        canvas.drawRoundRect(l + inset, t + inset, r - inset, t + (b - t) * 0.45f, round, round, highlightPaint)

        // Рисуем цифру, только если «раскрыто» достаточно по активной оси
        val openEnough = when (dir) {
            FlipDir.LR, FlipDir.RL -> scaleVal > 0.12f
            FlipDir.TB, FlipDir.BT -> scaleVal > 0.12f
        }
        if (openEnough) {
            val cx = (l + r) / 2f
            val cy = (t + b) / 2f
            textPaint.color = if (tile.lightText) Color.WHITE else Color.parseColor("#212121")
            val fm = textPaint.fontMetrics
            val textY = cy - (fm.ascent + fm.descent) / 2f
            canvas.drawText(tile.digit.toString(), cx, textY, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                if (flipAnimator?.isRunning == true) return true
                val c = ((event.x - gridLeft) / cellSize).toInt()
                val r = ((event.y - gridTop) / cellSize).toInt()
                if (c in 0 until cols && r in 0 until rows) {
                    onTileTap?.invoke(tiles[r][c].digit)
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
