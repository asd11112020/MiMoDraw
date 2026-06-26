package com.mimo.draw.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.mimo.draw.DrawPath
import com.mimo.draw.Tool

class EnhancedDrawingRenderer(
    private val bitmapPool: BitmapPool,
    private val layerManager: LayerBitmapManager
) {
    private val pressureEngine = PressureEngine()
    private val bezierInterpolater = BezierInterpolater()
    private val paintCache = HashMap<String, Paint>(32)
    private var canvasWidth = 0
    private var canvasHeight = 0

    fun initialize(width: Int, height: Int) {
        canvasWidth = width
        canvasHeight = height
    }

    fun renderStroke(
        layerId: String,
        points: List<Offset>,
        pressures: List<Float>,
        color: Color,
        strokeWidth: Float,
        tool: Tool,
        alpha: Float = 1f
    ) {
        val canvas = layerManager.getCanvas(layerId)
        val paint = getPaint(color, strokeWidth, tool, alpha)

        if (points.size < 2) return

        bezierInterpolater.clear()
        for (i in points.indices) {
            val pressure = if (i < pressures.size) pressures[i] else 0.5f
            bezierInterpolater.addPoint(points[i], pressure, System.nanoTime() + i)
        }

        val interpolated = bezierInterpolater.interpolate()

        when (tool) {
            Tool.SPRAY -> renderSpray(canvas, interpolated, paint)
            Tool.BRUSH -> renderSmoothBrush(canvas, interpolated, paint)
            else -> renderStandardStroke(canvas, interpolated, paint)
        }

        layerManager.markDirty(layerId)
    }

    fun renderStrokeIncremental(
        layerId: String,
        point: Offset,
        pressure: Float,
        color: Color,
        strokeWidth: Float,
        tool: Tool,
        alpha: Float = 1f
    ) {
        val canvas = layerManager.getCanvas(layerId)
        val paint = getPaint(color, strokeWidth, tool, alpha)

        bezierInterpolater.addPoint(point, pressure, System.nanoTime())

        val interpolated = bezierInterpolater.interpolateIncremental()

        when (tool) {
            Tool.SPRAY -> renderSpray(canvas, interpolated, paint)
            Tool.BRUSH -> renderSmoothBrush(canvas, interpolated, paint)
            else -> renderStandardStroke(canvas, interpolated, paint)
        }

        layerManager.markDirty(layerId)
    }

    private fun renderStandardStroke(
        canvas: Canvas,
        interpolated: BezierInterpolater.InterpolatedSegment,
        paint: Paint
    ) {
        val points = interpolated.points
        val widths = interpolated.widths

        if (points.size < 2) return

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val width = widths.getOrElse(i) { widths.last() }

            val segPaint = Paint(paint).apply {
                strokeWidth = width
            }

            canvas.drawLine(prev.x, prev.y, curr.x, curr.y, segPaint)
        }
    }

    private fun renderSmoothBrush(
        canvas: Canvas,
        interpolated: BezierInterpolater.InterpolatedSegment,
        paint: Paint
    ) {
        val points = interpolated.points
        val widths = interpolated.widths

        if (points.size < 2) return

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val width = widths.getOrElse(i) { widths.last() }

            val segPaint = Paint(paint).apply {
                strokeWidth = width
                strokeCap = Paint.Cap.ROUND
            }

            canvas.drawLine(prev.x, prev.y, curr.x, curr.y, segPaint)
        }
    }

    private fun renderSpray(
        canvas: Canvas,
        interpolated: BezierInterpolater.InterpolatedSegment,
        paint: Paint
    ) {
        val points = interpolated.points
        val sprayPaint = Paint(paint).apply {
            alpha = 40
        }

        for (point in points) {
            for (j in 0..30) {
                val angle = (Math.random() * 360).toFloat()
                val radius = (Math.random() * 40f)
                val offsetX = (kotlin.math.cos(angle.toDouble()) * radius).toFloat()
                val offsetY = (kotlin.math.sin(angle.toDouble()) * radius).toFloat()
                canvas.drawCircle(
                    point.x + offsetX,
                    point.y + offsetY,
                    1.5f,
                    sprayPaint
                )
            }
        }
    }

    fun startNewStroke() {
        bezierInterpolater.clear()
    }

    private fun getPaint(
        color: Color,
        strokeWidth: Float,
        tool: Tool,
        alpha: Float
    ): Paint {
        val key = "${color.hashCode()}_${strokeWidth}_${tool}_${alpha}"

        return paintCache.getOrPut(key) {
            Paint().apply {
                this.color = android.graphics.Color.argb(
                    (alpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                )
                this.strokeWidth = when (tool) {
                    Tool.SPRAY -> strokeWidth * 0.5f
                    Tool.ERASER -> strokeWidth * 2f
                    else -> strokeWidth
                }
                style = Paint.Style.STROKE
                strokeCap = when (tool) {
                    Tool.CALLIGRAPHY -> Paint.Cap.SQUARE
                    else -> Paint.Cap.ROUND
                }
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
        }
    }

    fun resetPaintCache() {
        paintCache.clear()
    }

    fun getPressureEngine(): PressureEngine = pressureEngine

    fun getBezierInterpolater(): BezierInterpolater = bezierInterpolater
}
