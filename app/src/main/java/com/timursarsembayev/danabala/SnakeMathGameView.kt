package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class SnakeMathGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onLevelChanged: ((level: Int) -> Unit)? = null
    var onGameOver: ((levelsCompleted: Int) -> Unit)? = null
    // Новые колбэки озвучки
    var onCorrectPickup: ((digit: Int) -> Unit)? = null
    var onWrongPickup: ((digit: Int) -> Unit)? = null
    // Новый колбэк победы
    var onWin: ((levelsCompleted: Int) -> Unit)? = null

    // Grid size
    private val cols = 10
    private val rows = 10
    private var cellSize = 0f
    private var gridLeft = 0f
    private var gridTop = 0f

    // Game loop
    private var running = false
    private val choreographer = Choreographer.getInstance()
    private var lastFrameNs = 0L
    private val frameCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(now: Long) {
            if (!running) return
            if (lastFrameNs == 0L) lastFrameNs = now
            val dt = (now - lastFrameNs) / 1_000_000_000f
            lastFrameNs = now
            update(dt)
            invalidate()
            choreographer.postFrameCallback(this)
        }
    }

    // Movement timing (ручной режим: авто-движение отключено)
    private var moveInterval = 0.42f
    private var moveTimer = 0f
    private var autoMove = false

    // Leveling
    private var level = 1
    private var levelsCompleted = 0

    // Snake model
    private enum class Dir { UP, DOWN, LEFT, RIGHT }
    private var dir = Dir.RIGHT
    private var nextDir = Dir.RIGHT
    private val snake = ArrayDeque<Pair<Int, Int>>() // head at first
    private val occupied = HashSet<Pair<Int, Int>>()

    // Digits 0..9 positions
    private val digits = mutableMapOf<Int, Pair<Int, Int>>()
    private var targetDigit = 0

    // Input gesture
    private var downX = 0f
    private var downY = 0f
    private val swipeThreshold = 24f * resources.displayMetrics.density
    // Свайп по голове — флаг, что касание началось на клетке головы
    private var headSwipeStart = false

    // Paints
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(25, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }
    private val snakePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val snakeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = Color.argb(40, 0, 0, 0)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val glossyShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(6f * resources.displayMetrics.density, BlurMaskFilter.Blur.NORMAL)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val digitColors = intArrayOf(
        Color.parseColor("#FF5252"),
        Color.parseColor("#FF9800"),
        Color.parseColor("#FFEB3B"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#00BCD4"),
        Color.parseColor("#3F51B5"),
        Color.parseColor("#9C27B0"),
        Color.parseColor("#795548"),
        Color.parseColor("#8BC34A"),
        Color.parseColor("#03A9F4")
    )

    init {
        isClickable = true
        resetGame()
    }

    fun pause() {
        running = false
        lastFrameNs = 0L
        choreographer.removeFrameCallback(frameCallback)
    }

    fun resume() {
        if (!running) {
            running = true
            lastFrameNs = 0L
            choreographer.postFrameCallback(frameCallback)
        }
    }

    fun resetGame() {
        snake.clear(); occupied.clear(); digits.clear()
        level = 1; levelsCompleted = 0
        targetDigit = 0
        dir = Dir.RIGHT; nextDir = Dir.RIGHT
        // стартовая змейка 3 клетки по центру: хвост слева, голова справа
        val startC = cols / 2
        val startR = rows / 2
        for (i in 2 downTo 0) {
            val p = Pair(startC - i, startR)
            snake.addFirst(p)
            occupied.add(p)
        }
        updateMoveInterval()
        placeAllDigits()
        onLevelChanged?.invoke(level)
    }

    private fun updateMoveInterval() {
        // Ускоряемся с уровнем, но стартовая скорость и минимум замедлены в 1.5 раза
        // было: max(0.10, 0.28 - 0.02*(level-1))
        moveInterval = maxOf(0.15f, 0.42f - (level - 1) * 0.03f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val contentW = w - paddingLeft - paddingRight
        val contentH = h - paddingTop - paddingBottom
        // предварительный размер клетки для оценки высоты прогресс-бара
        val cell0 = min(contentW / cols.toFloat(), contentH / rows.toFloat())
        val barExtra = cell0 * 0.9f + 16f * resources.displayMetrics.density
        cellSize = min(contentW / cols.toFloat(), (contentH - barExtra) / rows.toFloat())
        val gridW = cellSize * cols
        val gridH = cellSize * rows
        gridLeft = paddingLeft + (contentW - gridW) / 2f
        // Отступ сверху под прогресс-бар + центрирование о��тавшегося пространства
        gridTop = paddingTop + barExtra + (contentH - barExtra - gridH) / 2f
        textPaint.textSize = cellSize * 0.6f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Сетка
        for (r in 0..rows) {
            val y = gridTop + r * cellSize
            canvas.drawLine(gridLeft, y, gridLeft + cols * cellSize, y, gridPaint)
        }
        for (c in 0..cols) {
            val x = gridLeft + c * cellSize
            canvas.drawLine(x, gridTop, x, gridTop + rows * cellSize, gridPaint)
        }
        canvas.drawRect(gridLeft, gridTop, gridLeft + cols * cellSize, gridTop + rows * cellSize, borderPaint)

        // Рисуем кубики цифр
        for ((digit, pos) in digits) {
            drawDigitCube(canvas, pos.first, pos.second, digit)
        }

        // Рисуем змейку
        drawSnake(canvas)

        // Верхняя полоска прогресса прямо над сеткой
        drawProgressBar(canvas)
    }

    private fun drawProgressBar(canvas: Canvas) {
        // 10 слотов над полем — фиксированный размер слота, адаптивные отст��пы
        val slotSize = cellSize * 0.8f
        val gaps = 9
        val gridWidth = cols * cellSize
        val slotsW = slotSize * 10
        val barMargin = kotlin.math.max(0f, (gridWidth - slotsW) / gaps)
        val totalW = slotsW + barMargin * gaps
        val startX = gridLeft + (gridWidth - totalW) / 2f
        val y = gridTop - slotSize - 10f * resources.displayMetrics.density
        val round = slotSize * 0.2f
        val slotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val slotStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * resources.displayMetrics.density
            color = Color.argb(60, 0, 0, 0)
        }
        val filledText = Paint(textPaint).apply { color = Color.WHITE; textSize = slotSize * 0.6f }
        for (i in 0 until 10) {
            val left = startX + i * (slotSize + barMargin)
            val top = y
            val right = left + slotSize
            val bottom = top + slotSize
            if (i < targetDigit) {
                val color = digitColors[i % digitColors.size]
                glossyShadow.color = color
                canvas.drawRoundRect(left + 2f, top + 2f, right + 2f, bottom + 2f, round, round, glossyShadow)
                fillPaint.shader = LinearGradient(left, top, right, bottom, lighten(color, 0.25f), darken(color, 0.15f), Shader.TileMode.CLAMP)
                canvas.drawRoundRect(left, top, right, bottom, round, round, fillPaint)
                highlightPaint.shader = LinearGradient(left, top, left, top + slotSize * 0.45f, Color.argb(120, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                val inset = slotSize * 0.06f
                canvas.drawRoundRect(left + inset, top + inset, right - inset, top + slotSize * 0.45f, round, round, highlightPaint)
                val fm = filledText.fontMetrics
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val ty = cy - (fm.ascent + fm.descent) / 2f
                canvas.drawText("$i", cx, ty, filledText)
            } else {
                slotPaint.color = Color.argb(40, 255, 255, 255)
                canvas.drawRoundRect(left, top, right, bottom, round, round, slotPaint)
                canvas.drawRoundRect(left, top, right, bottom, round, round, slotStroke)
            }
        }
    }

    private fun drawSnake(canvas: Canvas) {
        if (snake.isEmpty()) return
        val head = snake.first()
        val headRect = cellRect(head.first, head.second)
        // Голова — красная, тело — зелёное
        val headColor = Color.parseColor("#E53935")
        drawRoundedCell(canvas, headRect, headColor)
        val bodyColor = Color.parseColor("#27AE60")
        for ((index, p) in snake.withIndex()) {
            if (index == 0) continue
            val rect = cellRect(p.first, p.second)
            drawRoundedCell(canvas, rect, bodyColor)
        }
    }

    private fun drawDigitCube(canvas: Canvas, c: Int, r: Int, digit: Int) {
        val rect = cellRect(c, r)
        val color = digitColors[digit % digitColors.size]
        val round = cellSize * 0.22f
        // Тень
        glossyShadow.color = color
        canvas.drawRoundRect(rect.left + 3f, rect.top + 3f, rect.right + 3f, rect.bottom + 3f, round, round, glossyShadow)
        // Градиент корпуса
        fillPaint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, lighten(color, 0.25f), darken(color, 0.15f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, round, round, fillPaint)
        // Блик
        highlightPaint.shader = LinearGradient(rect.left, rect.top, rect.left, rect.top + rect.height() * 0.45f, Color.argb(120, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        val inset = rect.width() * 0.06f
        canvas.drawRoundRect(RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.top + rect.height() * 0.45f), round, round, highlightPaint)
        // Цифра
        textPaint.color = Color.WHITE
        val fm = textPaint.fontMetrics
        val cx = rect.centerX()
        val cy = rect.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText("$digit", cx, cy, textPaint)
    }

    private fun drawRoundedCell(canvas: Canvas, rect: RectF, color: Int) {
        val round = cellSize * 0.25f
        // Лёгкая тень
        snakePaint.shader = null
        glossyShadow.color = color
        canvas.drawRoundRect(rect.left + 2f, rect.top + 2f, rect.right + 2f, rect.bottom + 2f, round, round, glossyShadow)
        // Тело с небольшим градиентом
        snakePaint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, lighten(color, 0.18f), darken(color, 0.1f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, round, round, snakePaint)
        canvas.drawRoundRect(rect, round, round, snakeStroke)
    }

    // Координаты клетки в прямоугольник на канвасе
    private fun cellRect(c: Int, r: Int): RectF {
        val l = gridLeft + c * cellSize
        val t = gridTop + r * cellSize
        return RectF(l, t, l + cellSize, t + cellSize)
    }

    // Преобразование координат касания в координаты клетки сетки, либо null, если вне поля
    private fun pointToCell(x: Float, y: Float): Pair<Int, Int>? {
        if (x < gridLeft || y < gridTop) return null
        val c = ((x - gridLeft) / cellSize).toInt()
        val r = ((y - gridTop) / cellSize).toInt()
        if (c !in 0 until cols || r !in 0 until rows) return null
        return c to r
    }

    private fun update(dt: Float) {
        if (autoMove) {
            moveTimer += dt
            if (moveTimer >= moveInterval) {
                moveTimer -= moveInterval
                stepWithDirection(null)
            }
        }
    }

    private fun stepWithDirection(dirOverride: Dir?) {
        // Если передан override и он противоположен текущему — ничего не делаем
        if (dirOverride != null && isOpposite(dir, dirOverride)) {
            return
        }
        // применить новое направление, если не противоположно
        if (dirOverride != null && !isOpposite(dir, dirOverride)) {
            dir = dirOverride
        } else if (!isOpposite(dir, nextDir)) {
            dir = nextDir
        }
        val head = snake.first()
        var nc = head.first
        var nr = head.second
        when (dir) {
            Dir.UP -> nr -= 1
            Dir.DOWN -> nr += 1
            Dir.LEFT -> nc -= 1
            Dir.RIGHT -> nc += 1
        }
        // Обёртка
        if (nc < 0) nc = cols - 1
        if (nc >= cols) nc = 0
        if (nr < 0) nr = rows - 1
        if (nr >= rows) nr = 0
        val newHead = Pair(nc, nr)
        // Проверка самопересечения
        if (occupied.contains(newHead) && newHead != snake.last()) {
            gameOver(); return
        }
        snake.addFirst(newHead)
        occupied.add(newHead)

        var grow = false
        var shrink = false
        val foundDigit = digits.entries.firstOrNull { it.value == newHead }?.key
        if (foundDigit != null) {
            if (foundDigit == targetDigit) {
                grow = true
                digits.remove(foundDigit)
                onCorrectPickup?.invoke(foundDigit)
                targetDigit++
                if (targetDigit >= 10) {
                    levelsCompleted++
                    level++
                    targetDigit = 0
                    updateMoveInterval()
                    placeAllDigits()
                    onLevelChanged?.invoke(level)
                }
            } else {
                shrink = true
                onWrongPickup?.invoke(foundDigit)
            }
        }
        if (!grow) {
            val tail = snake.removeLast()
            occupied.remove(tail)
        }
        if (shrink && snake.size > 1) {
            val tail = snake.removeLast()
            occupied.remove(tail)
        }

        // П��оверка условия победы: длина >= половины поля + 3 стартовые
        val targetWinLen = (cols * rows) / 2 + 3
        if (snake.size >= targetWinLen) {
            pause()
            onWin?.invoke(levelsCompleted)
            return
        }
    }

    private fun gameOver() {
        pause()
        onGameOver?.invoke(levelsCompleted)
    }

    private fun placeAllDigits() {
        digits.clear()
        // Кандидаты: все свободные клетки
        val free = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until rows) for (c in 0 until cols) {
            val p = c to r
            if (!occupied.contains(p)) free.add(p)
        }
        // Несколько попыток случайной раскладки, чтобы соблюсти дистанцию между цифрами
        val rnd = free.toMutableList()
        var success = false
        repeat(200) {
            rnd.shuffle()
            val placed = mutableListOf<Pair<Int, Int>>()
            var idx = 0
            var d = 0
            while (d <= 9 && idx < rnd.size) {
                val pos = rnd[idx++]
                if (isFarFromAll(pos, placed)) {
                    placed.add(pos)
                    d++
                }
            }
            if (placed.size == 10) {
                // Заполняем digits 0..9 в порядке возрастания
                for (i in 0..9) digits[i] = placed[i]
                success = true
                return@repeat
            }
        }
        if (!success) {
            // Фолбэк: ослабляем случай — берём через клетку по сетке, чтобы точно разнести
            val placed = mutableListOf<Pair<Int, Int>>()
            outer@ for (r in 0 until rows step 2) {
                for (c in 0 until cols step 2) {
                    val p = c to r
                    if (occupied.contains(p)) continue
                    if (isFarFromAll(p, placed)) {
                        placed.add(p)
                        if (placed.size == 10) break@outer
                    }
                }
            }
            for (i in 0 until placed.size) digits[i] = placed[i]
        }
    }

    private fun isFarFromAll(pos: Pair<Int, Int>, placed: List<Pair<Int, Int>>): Boolean {
        val (c, r) = pos
        for ((pc, pr) in placed) {
            val dc = kotlin.math.abs(pc - c)
            val dr = kotlin.math.abs(pr - r)
            // запрещаем соседство по горизонтали/вертикали/диагонали (Chebyshev дистанция <= 1)
            if (dc <= 1 && dr <= 1) return false
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y
                // Определяем, началось ли касание на голове змейки
                headSwipeStart = false
                if (snake.isNotEmpty()) {
                    val head = snake.first()
                    val headRect = cellRect(head.first, head.second)
                    if (headRect.contains(event.x, event.y)) {
                        headSwipeStart = true
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                val isHorizontal = abs(dx) > abs(dy)
                val isSwipe = if (isHorizontal) abs(dx) > swipeThreshold else abs(dy) > swipeThreshold
                if (isSwipe) {
                    val swipeDir = if (isHorizontal) {
                        if (dx > 0) Dir.RIGHT else Dir.LEFT
                    } else {
                        if (dy > 0) Dir.DOWN else Dir.UP
                    }
                    if (headSwipeStart) {
                        // Свайп начался на голове — делаем шаг в сторону свайпа
                        stepWithDirection(swipeDir)
                    } else {
                        // Обычный свайп по полю — только меняем курс
                        setNextDir(swipeDir)
                    }
                    performClick()
                    return true
                }
                // Иначе — это короткий тап. Пытаемся сдвинуться в соседнюю к��етку (пустую, цифра или хвост).
                val tappedCell = pointToCell(event.x, event.y)
                if (tappedCell != null && snake.isNotEmpty()) {
                    val head = snake.first()
                    val dc = tappedCell.first - head.first
                    val dr = tappedCell.second - head.second
                    // Соседство по Манхэттену строго на 1 клетку (без диагоналей и без обёртки)
                    if (abs(dc) + abs(dr) == 1) {
                        val dirToCell = when {
                            dc == 1 -> Dir.RIGHT
                            dc == -1 -> Dir.LEFT
                            dr == 1 -> Dir.DOWN
                            else -> Dir.UP
                        }
                        // Запрещаем шаг только в тело (кроме хвоста). Цифры разрешены для поедания.
                        val isBlockedByBody = occupied.contains(tappedCell) && tappedCell != snake.last()
                        if (!isBlockedByBody && !isOpposite(dir, dirToCell)) {
                            stepWithDirection(dirToCell)
                            performClick()
                            return true
                        }
                    }
                }
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    // Публичные методы для одношагового движения по нажатию кнопок
    fun moveOnceUp() { stepWithDirection(Dir.UP) }
    fun moveOnceDown() { stepWithDirection(Dir.DOWN) }
    fun moveOnceLeft() { stepWithDirection(Dir.LEFT) }
    fun moveOnceRight() { stepWithDirection(Dir.RIGHT) }

    // Сохраняем методы input* (свайпы), но они те��ерь лишь меняют курс
    fun inputUp() { setNextDir(Dir.UP) }
    fun inputDown() { setNextDir(Dir.DOWN) }
    fun inputLeft() { setNextDir(Dir.LEFT) }
    fun inputRight() { setNextDir(Dir.RIGHT) }

    private fun setNextDir(d: Dir) {
        if (!isOpposite(dir, d)) {
            nextDir = d
        }
    }

    private fun isOpposite(a: Dir, b: Dir): Boolean {
        return (a == Dir.UP && b == Dir.DOWN) || (a == Dir.DOWN && b == Dir.UP) ||
                (a == Dir.LEFT && b == Dir.RIGHT) || (a == Dir.RIGHT && b == Dir.LEFT)
    }

    private fun lighten(color: Int, amount: Float): Int {
        val a = Color.alpha(color)
        var r = Color.red(color)
        var g = Color.green(color)
        var b = Color.blue(color)
        r = (r + (255 - r) * amount).toInt()
        g = (g + (255 - g) * amount).toInt()
        b = (b + (255 - b) * amount).toInt()
        return Color.argb(a, r, g, b)
    }

    private fun darken(color: Int, amount: Float): Int {
        val a = Color.alpha(color)
        var r = Color.red(color)
        var g = Color.green(color)
        var b = Color.blue(color)
        r = (r * (1f - amount)).toInt()
        g = (g * (1f - amount)).toInt()
        b = (b * (1f - amount)).toInt()
        return Color.argb(a, r, g, b)
    }
}
