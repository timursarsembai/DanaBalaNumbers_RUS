package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint объекты
    private val outlinePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val drawingPaint = Paint().apply {
        color = 0xFF4CAF50.toInt()
        strokeWidth = 20f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val eraserPaint = Paint().apply {
        strokeWidth = 30f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // Bitmap для рисования
    private var drawingBitmap: Bitmap? = null
    private var drawingCanvas: Canvas? = null
    private var outlineBitmap: Bitmap? = null
    private var maskBitmap: Bitmap? = null

    // Пути
    private var currentPath = Path()
    private val drawingPaths = mutableListOf<DrawingPath>()

    // Контур текущей цифры
    private var numberOutline: Path? = null

    // Состояние
    private var isEraserMode = false
    private var isCompleted = false
    private var currentNumber = 0

    // Колбэк для прогресса
    private var onProgressChanged: ((Float) -> Unit)? = null

    // Последняя позиция касания
    private var lastX = 0f
    private var lastY = 0f

    data class DrawingPath(
        val path: Path,
        val paint: Paint
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0 && h > 0) {
            // Адаптивные толщины
            val minDim = min(w, h).toFloat()
            val outline = minDim * 0.025f   // толще обводка цифры
            val brush = minDim * 0.07f      // толще кисть
            val eraser = brush * 1.6f       // ластик толще кисти
            outlinePaint.strokeWidth = outline
            drawingPaint.strokeWidth = brush
            eraserPaint.strokeWidth = eraser

            drawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawingCanvas = Canvas(drawingBitmap!!)

            outlineBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

            // Перерисовываем контур если он был установлен
            updateNumberOutline()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем фон
        canvas.drawColor(Color.WHITE)

        // Рисуем контур цифры
        numberOutline?.let { outline ->
            canvas.drawPath(outline, outlinePaint)
        }

        // Рисуем нарисованные линии
        drawingBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }

        // Рисуем текущий путь только для кисти (не для ластика)
        if (!currentPath.isEmpty && !isEraserMode) {
            canvas.drawPath(currentPath, drawingPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.reset()
                currentPath.moveTo(x, y)
                lastX = x
                lastY = y
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - lastX)
                val dy = abs(y - lastY)
                if (dx >= 4 || dy >= 4) {
                    // Сглаживаем сегмент
                    currentPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                    lastX = x
                    lastY = y

                    if (isEraserMode) {
                        // Немедленно стираем на bitmap, чтобы не было «маски»
                        drawingCanvas?.drawPath(currentPath, eraserPaint)
                        // Сбрасываем текущий путь, начинаем новый сегмент
                        currentPath.reset()
                        currentPath.moveTo(lastX, lastY)
                    }
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val paint = if (isEraserMode) eraserPaint else drawingPaint.apply { color = drawingPaint.color }
                // Финализируем штрих: для ластика здесь почти нечего, т.к. уже применяли на MOVE
                if (!isEraserMode) {
                    drawingCanvas?.drawPath(currentPath, paint)
                }
                drawingPaths.add(DrawingPath(Path(currentPath), Paint(paint)))
                currentPath.reset()

                calculateProgress()
                invalidate()
                performClick()
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun setNumberOutline(number: Int) {
        currentNumber = number
        updateNumberOutline()
        clearDrawing()
    }

    private fun updateNumberOutline() {
        if (width <= 0 || height <= 0) return

        numberOutline = NumberOutlineGenerator.generateOutline(currentNumber, width, height, context)

        // Создаем маску для определения внутренней области
        createMask()

        invalidate()
    }

    private fun createMask() {
        maskBitmap?.let { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            numberOutline?.let { outline ->
                val fillPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawPath(outline, fillPaint)
            }
        }
    }

    private fun calculateProgress() {
        maskBitmap?.let { mask ->
            drawingBitmap?.let { drawing ->
                val totalPixels = countFilledPixels(mask)
                val filledPixels = countOverlapPixels(mask, drawing)

                val progress = if (totalPixels > 0) {
                    filledPixels.toFloat() / totalPixels.toFloat()
                } else {
                    0f
                }

                onProgressChanged?.invoke(progress.coerceIn(0f, 1f))
            }
        }
    }

    private fun countFilledPixels(bitmap: Bitmap): Int {
        var count = 0
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            if (Color.alpha(pixel) > 0) {
                count++
            }
        }
        return count
    }

    private fun countOverlapPixels(mask: Bitmap, drawing: Bitmap): Int {
        var count = 0
        val maskPixels = IntArray(mask.width * mask.height)
        val drawingPixels = IntArray(drawing.width * drawing.height)

        mask.getPixels(maskPixels, 0, mask.width, 0, 0, mask.width, mask.height)
        drawing.getPixels(drawingPixels, 0, drawing.width, 0, 0, drawing.width, drawing.height)

        for (i in maskPixels.indices) {
            if (Color.alpha(maskPixels[i]) > 0 && Color.alpha(drawingPixels[i]) > 0) {
                count++
            }
        }
        return count
    }

    fun setDrawingColor(color: Int) {
        drawingPaint.color = color
    }

    fun setEraserMode(enabled: Boolean) {
        isEraserMode = enabled
    }

    fun isEraserMode(): Boolean = isEraserMode

    fun clearDrawing() {
        drawingCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawingPaths.clear()
        currentPath.reset()
        isCompleted = false
        onProgressChanged?.invoke(0f)
        invalidate()
    }

    fun setCompleted(completed: Boolean) {
        isCompleted = completed
    }

    fun isCompleted(): Boolean = isCompleted

    fun setOnProgressChangedListener(listener: (Float) -> Unit) {
        onProgressChanged = listener
    }
}
