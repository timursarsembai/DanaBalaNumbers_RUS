package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

object NumberOutlineGenerator {

    fun generateOutline(number: Int, width: Int, height: Int, context: Context): Path {
        val path = Path()

        // Создаем красивые контуры: 0 — «бублик», 1–9 — по глифам шрифта
        createBeautifulOutline(number, path, width, height, context)
        // Включаем четно-нечетное правило заполнения для корректных «дырок» и колец
        path.fillType = Path.FillType.EVEN_ODD

        return path
    }

    private fun createBeautifulOutline(
        number: Int,
        path: Path,
        width: Int,
        height: Int,
        context: Context
    ) {
        // Увеличиваем область размещения цифры
        val centerX = width / 2f
        val centerY = height / 2f
        val digitWidth = width * 0.65f
        val digitHeight = height * 0.8f

        val left = centerX - digitWidth / 2
        val top = centerY - digitHeight / 2
        val right = centerX + digitWidth / 2
        val bottom = centerY + digitHeight / 2

        val strokeWidth = minOf(width, height) * 0.08f

        when (number) {
            0 -> createZero(path, left, top, right, bottom, strokeWidth)
            else -> {
                val glyph = createGlyphDigit(number, left, top, right, bottom, context)
                // Заполняем path контуром глифа
                path.addPath(glyph)
            }
        }
    }

    private fun createZero(path: Path, left: Float, top: Float, right: Float, bottom: Float, strokeWidth: Float) {
        // «Бублик» — внешний овал + внутренний овал противоположного направления
        val outerOval = RectF(
            left + strokeWidth / 2,
            top + strokeWidth / 2,
            right - strokeWidth / 2,
            bottom - strokeWidth / 2
        )
        // Чуть толще стенка
        val wallThickness = 0.15f * minOf((right - left), (bottom - top))
        val innerOval = RectF(
            outerOval.left + wallThickness,
            outerOval.top + wallThickness,
            outerOval.right - wallThickness,
            outerOval.bottom - wallThickness
        )

        path.addOval(outerOval, Path.Direction.CW)
        path.addOval(innerOval, Path.Direction.CCW)
    }

    private fun createGlyphDigit(
        number: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        context: Context
    ): Path {
        val text = number.toString()
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            // Базовый размер — большой, чтобы получить детализированный контур, далее масштабируем
            textSize = 1000f
            style = android.graphics.Paint.Style.FILL
            // Используем обычный шрифт без излишней жирности, привычный детям
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val glyphPath = Path()
        paint.getTextPath(text, 0, text.length, 0f, 0f, glyphPath)
        // Корректируем fillType, чтобы «дырки» (например, в 8, 9) были вырезаны
        glyphPath.fillType = Path.FillType.EVEN_ODD

        val bounds = RectF()
        glyphPath.computeBounds(bounds, true)

        val targetW = (right - left)
        val targetH = (bottom - top)
        // Делаем цифру крупнее
        val scale = 0.92f * minOf(targetW / bounds.width(), targetH / bounds.height())

        val matrix = Matrix().apply {
            postTranslate(-bounds.left, -bounds.top)
            postScale(scale, scale)
            val scaledW = bounds.width() * scale
            val scaledH = bounds.height() * scale
            val dx = left + (targetW - scaledW) / 2f
            val dy = top + (targetH - scaledH) / 2f
            postTranslate(dx, dy)
        }
        glyphPath.transform(matrix)

        return glyphPath
    }
}
