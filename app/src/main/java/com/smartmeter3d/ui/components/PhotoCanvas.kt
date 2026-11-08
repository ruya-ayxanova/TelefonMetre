package com.smartmeter3d.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmeter3d.measurement.Point2
import kotlin.math.hypot

private val RefColor = Color(0xFFFF9800)
private val TargetColor = Color(0xFF00C853)

/**
 * Fotoğrafı gösterir; dokunarak nokta ekler, mevcut noktayı sürükleyerek taşır.
 * Noktalar fotoğrafın piksel koordinatlarındadır. [onMove] dizini = referans noktaları + hedef noktaları birleşik listesi.
 */
@Composable
fun PhotoCanvas(
    bitmap: Bitmap,
    refPoints: List<Point2>,
    targetPoints: List<Point2>,
    closeShapes: Boolean,
    onTap: (Point2) -> Unit,
    onMove: (Int, Point2) -> Unit,
    modifier: Modifier = Modifier
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val bw = bitmap.width.toFloat()
    val bh = bitmap.height.toFloat()
    val currentRef by rememberUpdatedState(refPoints)
    val currentTarget by rememberUpdatedState(targetPoints)
    val tap by rememberUpdatedState(onTap)
    val move by rememberUpdatedState(onMove)
    val textPx = with(LocalDensity.current) { 16.sp.toPx() }
    val labelPaint = remember(textPx) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textSize = textPx
            setShadowLayer(4f, 0f, 0f, Color.Black.toArgb())
        }
    }

    Box(modifier.fillMaxWidth().aspectRatio(bw / bh)) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(bitmap) {
                    val hitRadius = 36.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val scale = size.width / bw
                        val all = currentRef + currentTarget
                        val hit = all.indices
                            .minByOrNull { i ->
                                hypot(all[i].x * scale - down.position.x, all[i].y * scale - down.position.y)
                            }
                            ?.takeIf { i ->
                                hypot(all[i].x * scale - down.position.x, all[i].y * scale - down.position.y) <= hitRadius
                            }
                        if (hit != null) {
                            down.consume()
                            drag(down.id) { change ->
                                change.consume()
                                move(hit, Point2((change.position.x / scale).toDouble(), (change.position.y / scale).toDouble()))
                            }
                        } else {
                            val up = waitForUpOrCancellation()
                            if (up != null) {
                                tap(Point2((up.position.x / scale).toDouble(), (up.position.y / scale).toDouble()))
                            }
                        }
                    }
                }
        ) {
            val scale = size.width / bw
            drawShape(refPoints, RefColor, scale, closeShapes, labelPaint)
            drawShape(targetPoints, TargetColor, scale, closeShapes, labelPaint)
        }
    }
}

private fun DrawScope.drawShape(
    points: List<Point2>,
    color: Color,
    scale: Float,
    closeShape: Boolean,
    labelPaint: Paint
) {
    val offsets = points.map { Offset((it.x * scale).toFloat(), (it.y * scale).toFloat()) }
    val stroke = 3.dp.toPx()
    for (i in 0 until offsets.size - 1) drawLine(color, offsets[i], offsets[i + 1], strokeWidth = stroke)
    if (closeShape && offsets.size == 4) drawLine(color, offsets[3], offsets[0], strokeWidth = stroke)
    offsets.forEachIndexed { i, o ->
        drawCircle(Color.White, 10.dp.toPx(), o)
        drawCircle(color, 7.dp.toPx(), o)
        drawContext.canvas.nativeCanvas.drawText("${i + 1}", o.x + 12.dp.toPx(), o.y - 12.dp.toPx(), labelPaint)
    }
}
