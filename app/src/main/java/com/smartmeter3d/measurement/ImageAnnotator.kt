package com.smartmeter3d.measurement

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** Noktaları, çizgileri ve sonuç bilgisini fotoğrafın bir kopyasının üzerine çizer. */
object ImageAnnotator {
    private val REF_COLOR = 0xFFFF9800.toInt()
    private val TARGET_COLOR = 0xFF00C853.toInt()

    fun render(
        base: Bitmap,
        refPoints: List<Point2>,
        targetPoints: List<Point2>,
        closeShapes: Boolean,
        caption: List<String>
    ): Bitmap {
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val s = maxOf(out.width, out.height) / 1000f

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f * s }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f * s
            setShadowLayer(3f * s, 0f, 0f, Color.BLACK)
        }

        fun draw(points: List<Point2>, color: Int) {
            line.color = color
            for (i in 0 until points.size - 1) {
                canvas.drawLine(points[i].x.toFloat(), points[i].y.toFloat(), points[i + 1].x.toFloat(), points[i + 1].y.toFloat(), line)
            }
            if (closeShapes && points.size == 4) {
                canvas.drawLine(points[3].x.toFloat(), points[3].y.toFloat(), points[0].x.toFloat(), points[0].y.toFloat(), line)
            }
            points.forEachIndexed { i, p ->
                val x = p.x.toFloat()
                val y = p.y.toFloat()
                fill.color = Color.WHITE
                canvas.drawCircle(x, y, 9f * s, fill)
                fill.color = color
                canvas.drawCircle(x, y, 6f * s, fill)
                canvas.drawText("${i + 1}", x + 10f * s, y - 10f * s, label)
            }
        }
        draw(refPoints, REF_COLOR)
        draw(targetPoints, TARGET_COLOR)

        if (caption.isNotEmpty()) {
            val lineH = 22f * s
            val pad = 8f * s
            val boxH = pad * 2 + lineH * caption.size
            fill.color = 0xB3000000.toInt()
            canvas.drawRect(0f, out.height - boxH, out.width.toFloat(), out.height.toFloat(), fill)
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 16f * s }
            caption.forEachIndexed { i, t ->
                canvas.drawText(t, pad, out.height - boxH + pad + lineH * (i + 1) - 4f * s, text)
            }
        }
        return out
    }
}
