package com.mimo.draw.ui

import android.graphics.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

data class HsvColor(val hue: Float, val saturation: Float, val value: Float) {
    fun toArgb(): Int = Color.HSVToColor(floatArrayOf(hue, saturation, value))
    fun toComposeColor() = androidx.compose.ui.graphics.Color(toArgb())
}

@Composable
fun HsvPalette(
    currentColor: HsvColor,
    onColorChanged: (HsvColor) -> Unit,
    modifier: Modifier = Modifier
) {
    var hueAngle by remember { mutableStateOf(currentColor.hue) }
    var svX by remember { mutableStateOf(currentColor.saturation) }
    var svY by remember { mutableStateOf(1f - currentColor.value) }

    LaunchedEffect(currentColor) {
        hueAngle = currentColor.hue
        svX = currentColor.saturation
        svY = 1f - currentColor.value
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                }
            }
    ) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val w = size.width
            val h = size.height
            val ringRadius = minOf(w, h) * 0.32f
            val ringStroke = minOf(w, h) * 0.055f
            val centerX = w / 2f
            val centerY = h * 0.33f

            val ringPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = ringStroke
                isAntiAlias = true
            }
            val hueColors = IntArray(361) { Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f)) }
            ringPaint.shader = SweepGradient(centerX, centerY, hueColors, null)
            nativeCanvas.drawCircle(centerX, centerY, ringRadius, ringPaint)

            val indicatorAngle = Math.toRadians(hueAngle.toDouble())
            val indicatorX = centerX + (ringRadius * cos(indicatorAngle)).toFloat()
            val indicatorY = centerY + (ringRadius * sin(indicatorAngle)).toFloat()

            val indicatorPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            nativeCanvas.drawCircle(indicatorX, indicatorY, ringStroke * 0.7f, indicatorPaint)
            indicatorPaint.color = Color.HSVToColor(floatArrayOf(hueAngle, 1f, 1f))
            nativeCanvas.drawCircle(indicatorX, indicatorY, ringStroke * 0.45f, indicatorPaint)

            val svRectTop = h * 0.55f
            val svRectSize = minOf(w * 0.82f, h * 0.4f)
            val svRectLeft = (w - svRectSize) / 2f

            val svPaint = Paint().apply { isAntiAlias = true }
            val fullColor = Color.HSVToColor(floatArrayOf(hueAngle, 1f, 1f))
            val satGrad = LinearGradient(
                svRectLeft, svRectTop, svRectLeft + svRectSize, svRectTop,
                intArrayOf(Color.WHITE, fullColor), null, Shader.TileMode.CLAMP
            )
            val valGrad = LinearGradient(
                svRectLeft, svRectTop, svRectLeft, svRectTop + svRectSize,
                intArrayOf(Color.TRANSPARENT, Color.BLACK), null, Shader.TileMode.CLAMP
            )
            svPaint.shader = ComposeShader(satGrad, valGrad, PorterDuff.Mode.DARKEN)
            val rectF = RectF(svRectLeft, svRectTop, svRectLeft + svRectSize, svRectTop + svRectSize)
            nativeCanvas.drawRoundRect(rectF, 14f, 14f, svPaint)

            val svIndicatorX = svRectLeft + svX * svRectSize
            val svIndicatorY = svRectTop + svY * svRectSize
            val svOuter = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            nativeCanvas.drawCircle(svIndicatorX, svIndicatorY, 14f, svOuter)
            svOuter.style = Paint.Style.FILL
            svOuter.color = Color.HSVToColor(floatArrayOf(hueAngle, svX, (1f - svY).coerceIn(0f, 1f)))
            nativeCanvas.drawCircle(svIndicatorX, svIndicatorY, 11f, svOuter)
        }
    }
}

object HsvUtils {
    fun fromArgb(argb: Int): HsvColor {
        val hsv = FloatArray(3)
        Color.colorToHSV(argb, hsv)
        return HsvColor(hsv[0], hsv[1], hsv[2])
    }
}
