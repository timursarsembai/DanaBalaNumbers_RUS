package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class BlockMatchGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Callbacks
    var onScoreLevelChanged: ((score: Int, level: Int) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null

    // Grid
    private val cols = 5
    private val rows = 7
    private var cellSize = 0f
    private var gridLeft = 0f
    private var gridTop = 0f

    // Timing / loop
    private var running = false
    private var lastFrameNs = 0L
    private val choreographer: Choreographer = Choreographer.getInstance()
    private val frameCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(now: Long) {
            if (!running) return
            if (lastFrameNs == 0L) lastFrameNs = now
            val dt = (now - lastFrameNs) / 1_000_000_000f // seconds
            lastFrameNs = now
            update(dt)
            invalidate()
            choreographer.postFrameCallback(this)
        }
    }

    // Spawn and speeds
    private var spawnIntervalMs = 1200L
    private var spawnTimerMs = 0f
    private var fallSpeedCellsPerSec = 3f // было 3f, изменится в updateSpeed
    private val minSpawnInterval = 300L
    private val baseSpawnMs = 3840L // было 1920L, в 2 раза реже спавн
    private val spawnStepMs = 384L  // было 192L, в 2 раза реже ускорение спавна

    // Game state
    private data class Tile(
        var digit: Int,
        var colorIdx: Int,
        var textLight: Boolean,
        // Animations
        var fallOffsetY: Float = 0f, // px offset relative to its grid cell
        var removing: Boolean = false,
        var removeProgress: Float = 0f
    )
    private val grid: Array<Array<Tile?>> = Array(rows) { arrayOfNulls<Tile?>(cols) }
    private var score = 0
    private var level = 1
    private var nextLevelScoreTarget = 50 // стартовый порог победы/уровня

    // Interaction / selection & swap
    private var selC = -1
    private var selR = -1
    private var selectionPulse = 0f // radians
    private var inputLocked = false

    private var downX = 0f
    private var downY = 0f
    private val clickSlop = 12f * resources.displayMetrics.density

    // Swap animation between two neighbors
    private var swapping = false
    private var swapA_r = -1; private var swapA_c = -1
    private var swapB_r = -1; private var swapB_c = -1
    private var swapProgress = 0f
    private val swapDuration = 0.15f // seconds

    // Позиции тайлов, которые сместились вниз со времени последнего стабильного состояния
    private val movedDownSinceLastStable = mutableSetOf<Pair<Int, Int>>()

    // Позиции тайлов, которые в этом кадре полностью приземлились (fallOffsetY стал 0)
    private val landedThisFrame = mutableListOf<Pair<Int, Int>>()

    // Paints
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = 0x22000000
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

    init {
        isClickable = true
        resetGame()
    }

    fun resetGame() {
        for (r in 0 until rows) for (c in 0 until cols) grid[r][c] = null
        score = 0; level = 1
        nextLevelScoreTarget = 50
        movedDownSinceLastStable.clear()
        landedThisFrame.clear()
        updateSpeed()
        spawnInitial()
        spawnTimerMs = 0f
        onScoreLevelChanged?.invoke(score, level)
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        running = false
        lastFrameNs = 0L
        choreographer.removeFrameCallback(frameCallback)
    }

    private fun spawnInitial() {
        for (r in 0 until 3) for (c in 0 until cols) grid[r][c] = randomTile()
    }

    private fun randomTile(): Tile {
        val digit = Random.nextInt(0, 10)
        val colorIdx = Random.nextInt(0, blockColors.size)
        val textLight = Random.nextBoolean()
        return Tile(digit, colorIdx, textLight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val contentW = w - paddingLeft - paddingRight
        val contentH = h - paddingTop - paddingBottom
        cellSize = min(contentW / cols.toFloat(), contentH / rows.toFloat())
        val gridW = cellSize * cols
        val gridH = cellSize * rows
        gridLeft = paddingLeft + (contentW - gridW) / 2f
        gridTop = paddingTop + (contentH - gridH) / 2f
        textPaint.textSize = cellSize * 0.55f
        updateFallSpeedPx()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Background grid
        for (r in 0 until rows) for (c in 0 until cols) {
            val l = gridLeft + c * cellSize
            val t = gridTop + r * cellSize
            bgPaint.color = 0x10FFFFFF
            canvas.drawRect(l, t, l + cellSize, t + cellSize, bgPaint)
            canvas.drawRect(l, t, l + cellSize, t + cellSize, borderPaint)
        }
        // Draw tiles with offsets/animations
        for (r in 0 until rows) for (c in 0 until cols) {
            val tile = grid[r][c] ?: continue
            drawTile(canvas, c, r, tile)
        }
    }

    private fun drawTile(canvas: Canvas, c: Int, r: Int, tile: Tile) {
        // Swap offsets for A/B while animating swap
        var extraX = 0f; var extraY = tile.fallOffsetY
        if (swapping) {
            val dx = (swapB_c - swapA_c) * cellSize
            val dy = (swapB_r - swapA_r) * cellSize
            val t = easeOutCubic(swapProgress)
            if (r == swapA_r && c == swapA_c) { extraX += dx * t; extraY += dy * t }
            if (r == swapB_r && c == swapB_c) { extraX -= dx * t; extraY -= dy * t }
        }
        // Selection pulse scaling
        val isSelected = (r == selR && c == selC && !swapping)
        val scale = if (isSelected) 1f + 0.06f * sin(selectionPulse) else 1f

        val cx = gridLeft + c * cellSize + cellSize / 2f + extraX
        val cy = gridTop + r * cellSize + cellSize / 2f + extraY
        val baseSize = cellSize * (if (tile.removing) (1f - 0.6f * tile.removeProgress) else 1f)
        val size = baseSize * scale
        val half = size / 2f
        val left = cx - half
        val top = cy - half
        val right = cx + half
        val bottom = cy + half

        val color = blockColors[tile.colorIdx]
        val round = cellSize * 0.18f

        // Shadow
        glossyShadow.color = color
        canvas.drawRoundRect(left + 3f, top + 3f, right + 3f, bottom + 3f, round, round, glossyShadow)

        // Gradient body (переиспользуем Paint, меняем shader)
        fillPaint.shader = LinearGradient(
            left, top, right, bottom,
            lighten(color, 0.25f), darken(color, 0.15f), Shader.TileMode.CLAMP
        )

        val save = canvas.save()
        if (tile.removing) {
            val rot = 360f * tile.removeProgress
            canvas.rotate(rot, cx, cy)
        }
        canvas.drawRoundRect(left, top, right, bottom, round, round, fillPaint)

        // Highlight (переиспользуем Paint, меняем shader)
        highlightPaint.shader = LinearGradient(
            left, top, left, top + size * 0.45f,
            Color.argb(120, 255, 255, 255), Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        val inset = size * 0.06f
        canvas.drawRoundRect(left + inset, top + inset, right - inset, top + size * 0.45f, round, round, highlightPaint)

        // Digit
        textPaint.color = if (tile.textLight) Color.WHITE else Color.parseColor("#212121")
        val text = tile.digit.toString()
        val fm = textPaint.fontMetrics
        val textY = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, cx, textY, textPaint)

        canvas.restoreToCount(save)
    }

    private fun update(dt: Float) {
        // Единственное условие поражения — полный столбец
        if (running && isAnyColumnFull()) {
            running = false
            onGameOver?.invoke()
            return
        }

        // Update selection pulse
        if (selC >= 0 && selR >= 0) selectionPulse = (selectionPulse + dt * 2f * Math.PI).toFloat()

        // Update removal animations
        var removingActive = false
        if (advanceRemoval(dt)) removingActive = true

        // Apply gravity smoothly (не двигаем, если сейчас идёт своп/перемещение)
        landedThisFrame.clear()
        val fallingActive = if (!swapping) applyGravitySmooth(dt) else false

        // Немедленная проверка пар для только что приземлившихся тайлов
        if (landedThisFrame.isNotEmpty()) {
            val pairs = mutableListOf<List<Pair<Int, Int>>>()
            val used = mutableSetOf<Pair<Int, Int>>()
            for ((r, c) in landedThisFrame) {
                val t = grid.getOrNull(r)?.getOrNull(c) ?: continue
                if (t.removing || t.fallOffsetY != 0f) continue
                // левый
                val lc = c - 1
                if (lc >= 0) {
                    val lt = grid[r][lc]
                    if (lt != null && !lt.removing && lt.fallOffsetY == 0f && lt.digit == t.digit) {
                        val p1 = r to c; val p2 = r to lc
                        if (p1 !in used && p2 !in used) {
                            pairs.add(listOf(p1, p2))
                            used.add(p1); used.add(p2)
                            continue
                        }
                    }
                }
                // правый
                val rc = c + 1
                if (rc < cols) {
                    val rt = grid[r][rc]
                    if (rt != null && !rt.removing && rt.fallOffsetY == 0f && rt.digit == t.digit) {
                        val p1 = r to c; val p2 = r to rc
                        if (p1 !in used && p2 !in used) {
                            pairs.add(listOf(p1, p2))
                            used.add(p1); used.add(p2)
                        }
                    }
                }
            }
            if (pairs.isNotEmpty()) startRemoving(pairs)
        }

        // Убрано авто-уничтожение падающих пар и общий поиск совпадений

        // Update swap animation
        if (swapping) {
            swapProgress += dt / swapDuration
            if (swapProgress >= 1f) {
                // finalize swap
                swapProgress = 1f
                doSwapFinalize()
                swapping = false
                inputLocked = false
                // После завершения любого свопа проверяем пары, которые могли образоваться вручную рядом по горизонтали
                handleManualAdjacencyPairRemoval()
            }
        }

        // Spawn independently from input
        if (!removingActive && !swapping) {
            spawnTimerMs += dt * 1000f
            if (spawnTimerMs >= spawnIntervalMs) {
                spawnTimerMs -= spawnIntervalMs
                trySpawn()
            }
        }

        // Когда система стабилизировалась (нет падения/свопа/удаления),
        // проверяем пары, образованные из-за вертикального смещения сверху
        if (!fallingActive && !swapping && !removingActive) {
            if (movedDownSinceLastStable.isNotEmpty()) {
                val pairs = mutableListOf<List<Pair<Int, Int>>>()
                val seen = mutableSetOf<Pair<Int, Int>>()
                for ((r, c) in movedDownSinceLastStable) {
                    val t = grid.getOrNull(r)?.getOrNull(c) ?: continue
                    if (t.removing) continue
                    // Проверяем левого соседа
                    val lc = c - 1
                    if (lc >= 0) {
                        val lt = grid[r][lc]
                        if (lt != null && !lt.removing && lt.digit == t.digit) {
                            val p1 = r to c
                            val p2 = r to lc
                            if (p1 !in seen && p2 !in seen) {
                                pairs.add(listOf(p1, p2))
                                seen.add(p1); seen.add(p2)
                            }
                        }
                    }
                    // Проверяем правого соседа
                    val rc = c + 1
                    if (rc < cols) {
                        val rt = grid[r][rc]
                        if (rt != null && !rt.removing && rt.digit == t.digit) {
                            val p1 = r to c
                            val p2 = r to rc
                            if (p1 !in seen && p2 !in seen) {
                                pairs.add(listOf(p1, p2))
                                seen.add(p1); seen.add(p2)
                            }
                        }
                    }
                }
                movedDownSinceLastStable.clear()
                if (pairs.isNotEmpty()) startRemoving(pairs)
            }
        }
    }

    private fun isAnyColumnFull(): Boolean {
        for (c in 0 until cols) {
            var full = true
            for (r in 0 until rows) {
                if (grid[r][c] == null) { full = false; break }
            }
            if (full) return true
        }
        return false
    }

    private fun advanceRemoval(dt: Float): Boolean {
        var any = false
        var finishedAll = true
        for (r in 0 until rows) for (c in 0 until cols) {
            val t = grid[r][c] ?: continue
            if (t.removing) {
                any = true
                t.removeProgress += dt * 8f // fast
                if (t.removeProgress >= 1f) {
                    grid[r][c] = null
                } else finishedAll = false
            }
        }
        if (any && finishedAll) {
            score += pendingScoreGain
            pendingScoreGain = 0
            updateLevel()
            // Снятие блокировки ввода: удаление завершено
            inputLocked = false
        }
        return any
    }

    private fun applyGravitySmooth(dt: Float): Boolean {
        var anyFalling = false
        val fallPxPerSec = fallSpeedCellsPerSec * cellSize
        for (c in 0 until cols) {
            // from bottom-2 to top to pull down chain
            for (r in rows - 2 downTo 0) {
                val t = grid[r][c] ?: continue
                if (t.removing) continue
                var belowR = r + 1
                if (grid[belowR][c] == null) {
                    anyFalling = true
                    t.fallOffsetY += fallPxPerSec * dt
                    var movedDown = false
                    while (t.fallOffsetY >= cellSize && belowR <= rows - 1) {
                        // move down by one cell
                        t.fallOffsetY -= cellSize
                        grid[belowR][c] = t
                        grid[belowR - 1][c] = null
                        movedDown = true
                        belowR++
                        if (belowR > rows - 1 || grid[belowR][c] != null) break
                    }
                    if (movedDown) {
                        val finalR = belowR - 1
                        movedDownSinceLastStable.add(finalR to c)
                        // Если приземлились на дно, считаем посадку завершённой в этом кадре
                        if (finalR == rows - 1) {
                            if (t.fallOffsetY != 0f) {
                                t.fallOffsetY = 0f
                                landedThisFrame.add(finalR to c)
                            }
                        }
                    }
                } else {
                    // Snap if offset exists
                    val wasOffset = t.fallOffsetY
                    if (t.fallOffsetY != 0f) anyFalling = true
                    t.fallOffsetY = max(0f, t.fallOffsetY - fallPxPerSec * dt)
                    if (wasOffset > 0f && t.fallOffsetY == 0f) {
                        // тайл только что приземлился
                        landedThisFrame.add(r to c)
                    }
                }
            }
        }
        return anyFalling
    }

    private var pendingScoreGain = 0
    private fun startRemoving(matches: List<List<Pair<Int, Int>>>) {
        pendingScoreGain = 0
        for (chain in matches) for ((r, c) in chain) {
            val t = grid[r][c]
            if (t != null && !t.removing) {
                t.removing = true
                t.removeProgress = 0f
                pendingScoreGain += 1
            }
        }
        inputLocked = true
        // Deselect during removal
        selC = -1; selR = -1
    }

    private fun handleManualAdjacencyPairRemoval() {
        // Проверяем горизонтальных соседей для обеих конечных позиций свопа
        val candidates = listOf(swapA_r to swapA_c, swapB_r to swapB_c)
        val pairs = mutableListOf<List<Pair<Int, Int>>>()
        val used = mutableSetOf<Pair<Int, Int>>()
        for ((r, c) in candidates) {
            if (r !in 0 until rows || c !in 0 until cols) continue
            val t = grid[r][c] ?: continue
            if ((r to c) in used || t.removing) continue
            // сначала левый сосед
            val lc = c - 1
            if (lc >= 0) {
                val lt = grid[r][lc]
                if (lt != null && !lt.removing && lt.digit == t.digit && (r to lc) !in used) {
                    pairs.add(listOf(r to c, r to lc))
                    used.add(r to c); used.add(r to lc)
                    continue
                }
            }
            // затем правый сосед
            val rc = c + 1
            if (rc < cols) {
                val rt = grid[r][rc]
                if (rt != null && !rt.removing && rt.digit == t.digit && (r to rc) !in used) {
                    pairs.add(listOf(r to c, r to rc))
                    used.add(r to c); used.add(r to rc)
                }
            }
        }
        if (pairs.isNotEmpty()) startRemoving(pairs)
    }

    private fun doSwapStart(r1: Int, c1: Int, r2: Int, c2: Int) {
        if (swapping) return
        swapping = true
        inputLocked = true
        swapA_r = r1; swapA_c = c1
        swapB_r = r2; swapB_c = c2
        swapProgress = 0f
    }

    private fun doSwapFinalize() {
        // Exchange tiles in grid
        val a = grid[swapA_r][swapA_c]
        val b = grid[swapB_r][swapB_c]
        grid[swapA_r][swapA_c] = b
        grid[swapB_r][swapB_c] = a
        selR = swapB_r; selC = swapB_c // keep selection on moved tile
        selectionPulse = 0f
    }

    private fun trySpawn(): Boolean {
        val freeCols = (0 until cols).filter { grid[0][it] == null }
        if (freeCols.isEmpty()) {
            // Нет места для спавна — пропускаем этот тик
            return false
        }
        val col = freeCols.random()
        val tile = randomTile()
        tile.fallOffsetY = -cellSize // start above the cell for a nicer drop-in
        grid[0][col] = tile
        return true
    }

    private fun updateLevel() {
        var levelChanged = false
        while (score >= nextLevelScoreTarget) {
            level += 1
            // Следующий порог увеличивается на 10% и округляется вверх
            nextLevelScoreTarget = ceil(nextLevelScoreTarget * 1.1).toInt()
            levelChanged = true
        }
        if (levelChanged) {
            updateSpeed()
        }
        onScoreLevelChanged?.invoke(score, level)
    }

    private fun updateSpeed() {
        // Ещё реже спавн (в 2 раза):
        spawnIntervalMs = (baseSpawnMs - (level - 1) * spawnStepMs).coerceAtLeast(minSpawnInterval)
        // Падение в 2 раза медленнее: базу и шаг делим на 2
        fallSpeedCellsPerSec = 1.5f + (level - 1) * 0.4f
        updateFallSpeedPx()
    }

    private fun updateFallSpeedPx() {
        // no-op placeholder if later we add pixel-speed cache
    }

    // Input handling: select, tap neighbor to swap, drag to swap
    private var movedThisGesture = false

    // Вычисление дальнего доступного столбца по горизонтали в направлении dirX
    // Если соседняя ячейка пуста — двигаем только на одну клетку в сторону.
    // Если соседняя занята — выполняем обычный своп с соседом.
    private fun findHorizontalDestination(row: Int, col: Int, dirX: Int): Int {
        if (dirX == 0) return col
        val next = col + dirX
        return if (next in 0 until cols) next else col
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!running) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (inputLocked) return true
                val (c, r) = cellAt(event.x, event.y)
                movedThisGesture = false
                if (c in 0 until cols && r in 0 until rows && grid[r][c] != null) {
                    selC = c; selR = r; selectionPulse = 0f
                    downX = event.x; downY = event.y
                    return true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (inputLocked) return true
                if (movedThisGesture) return true // уже сделали действие в этом жесте
                if (selC !in 0 until cols || selR !in 0 until rows) return true
                val dx = event.x - downX
                val dy = event.y - downY
                val absDx = abs(dx); val absDy = abs(dy)
                if (max(absDx, absDy) >= clickSlop) {
                    if (absDx > absDy) {
                        // Горизонтальное перетягивание: разрешаем движение через пустые ячейки
                        val dirX = if (dx > 0) 1 else -1
                        val destC = findHorizontalDestination(selR, selC, dirX)
                        if (destC != selC) {
                            movedThisGesture = true
                            doSwapStart(selR, selC, selR, destC)
                        }
                    } else {
                        // Вертикальное движение: оставляем только своп с занятым соседом
                        val dirY = if (dy > 0) 1 else -1
                        val nc = selC
                        val nr = selR + dirY
                        if (nc in 0 until cols && nr in 0 until rows && grid[nr][nc] != null) {
                            movedThisGesture = true
                            doSwapStart(selR, selC, nr, nc)
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (inputLocked) return true
                if (!movedThisGesture) {
                    // Поддержка свопа по тапу соседа (если не было движения)
                    val (uc, ur) = cellAt(event.x, event.y)
                    if (selC in 0 until cols && selR in 0 until rows && uc in 0 until cols && ur in 0 until rows) {
                        val dc = abs(uc - selC); val dr = abs(ur - selR)
                        if (dc + dr == 1 && grid[ur][uc] != null) {
                            movedThisGesture = true
                            doSwapStart(selR, selC, ur, uc)
                        }
                    }
                }
                performClick()
                // Завершение жеста, следующий своп возможен только в новом DOWN
                movedThisGesture = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick(); return true
    }

    private fun cellAt(x: Float, y: Float): Pair<Int, Int> {
        val c = floor((x - gridLeft) / cellSize).toInt()
        val r = floor((y - gridTop) / cellSize).toInt()
        return if (c in 0 until cols && r in 0 until rows) c to r else -1 to -1
    }

    private fun lighten(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * (1f + amount)).coerceAtMost(1f)
        return Color.HSVToColor(hsv)
    }

    private fun darken(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * (1f - amount)).coerceAtLeast(0f)
        return Color.HSVToColor(hsv)
    }

    private fun easeOutCubic(t: Float): Float = 1f - (1f - t).pow(3)
}
