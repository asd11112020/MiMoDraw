package com.mimo.draw

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.StrokeCap
import androidx.compose.ui.graphics.drawscope.StrokeJoin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DrawingCanvas(
    state: DrawingState,
    onTouchStart: (Offset) -> Unit,
    onTouchMove: (Offset) -> Unit,
    onTouchEnd: () -> Unit,
    onSizeChanged: (Size) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(state.canvasBackground)
            .pointerInput(state.selectedTool, state.zoom) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (state.selectedTool == Tool.MOVE) {
                        onSizeChanged(Size.Zero)
                    }
                }
            }
            .pointerInput(state.selectedTool) {
                detectDragGestures(
                    onDragStart = { offset -> onTouchStart(offset) },
                    onDrag = { change, _ ->
                        change.consume()
                        onTouchMove(change.position)
                    },
                    onDragEnd = { onTouchEnd() },
                    onDragCancel = { onTouchEnd() }
                )
            }
    ) {
        onSizeChanged(size)

        if (state.showGrid) {
            drawGrid(state.gridSize, state.canvasBackground)
        }

        val visibleLayers = state.layers.filter { it.isVisible }.map { it.id }

        state.shapes.filter { it.layerId in visibleLayers }.forEach { shapePath ->
            drawShape(shapePath)
        }

        state.paths.filter { it.layerId in visibleLayers }.forEach { drawPath ->
            drawPath(drawPath)
        }

        state.texts.filter { it.layerId in visibleLayers }.forEach { text ->
            drawText(text)
        }

        state.currentPath?.let { drawPath(it) }
        state.currentShape?.let { drawShape(it) }

        if (state.isSymmetryEnabled) {
            drawSymmetryGuide(state.symmetryType, size)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(gridSize: Float, backgroundColor: Color) {
    val gridColor = backgroundColor.compositeOver(Color.LightGray).copy(alpha = 0.3f)
    val width = size.width
    val height = size.height

    var x = 0f
    while (x <= width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += gridSize
    }

    var y = 0f
    while (y <= height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += gridSize
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSymmetryGuide(type: SymmetryType, canvasSize: Size) {
    val guideColor = Color(0x44FF4081)

    when (type) {
        SymmetryType.VERTICAL -> {
            drawLine(
                color = guideColor,
                start = Offset(canvasSize.width / 2, 0f),
                end = Offset(canvasSize.width / 2, canvasSize.height),
                strokeWidth = 2f
            )
        }
        SymmetryType.HORIZONTAL -> {
            drawLine(
                color = guideColor,
                start = Offset(0f, canvasSize.height / 2),
                end = Offset(canvasSize.width, canvasSize.height / 2),
                strokeWidth = 2f
            )
        }
        SymmetryType.QUADRANT -> {
            drawLine(
                color = guideColor,
                start = Offset(canvasSize.width / 2, 0f),
                end = Offset(canvasSize.width / 2, canvasSize.height),
                strokeWidth = 2f
            )
            drawLine(
                color = guideColor,
                start = Offset(0f, canvasSize.height / 2),
                end = Offset(canvasSize.width, canvasSize.height / 2),
                strokeWidth = 2f
            )
        }
        SymmetryType.RADIAL -> {
            val centerX = canvasSize.width / 2
            val centerY = canvasSize.height / 2
            val radius = minOf(centerX, centerY)

            for (angle in 0 until 360 step 45) {
                val rad = Math.toRadians(angle.toDouble())
                drawLine(
                    color = guideColor,
                    start = Offset(centerX, centerY),
                    end = Offset(
                        centerX + (radius * cos(rad)).toFloat(),
                        centerY + (radius * sin(rad)).toFloat()
                    ),
                    strokeWidth = 1f
                )
            }

            drawCircle(
                color = guideColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
        }
    }
}

private fun Color.compositeOver(background: Color): Color {
    val alpha = this.alpha
    return Color(
        red = this.red * alpha + background.red * (1 - alpha),
        green = this.green * alpha + background.green * (1 - alpha),
        blue = this.blue * alpha + background.blue * (1 - alpha),
        alpha = 1f
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPath(drawPath: DrawPath) {
    if (drawPath.points.size < 2) return

    val path = Path().apply {
        moveTo(drawPath.points[0].x, drawPath.points[0].y)

        when (drawPath.tool) {
            Tool.BRUSH -> {
                for (i in 1 until drawPath.points.size) {
                    val prev = drawPath.points[i - 1]
                    val curr = drawPath.points[i]
                    val midX = (prev.x + curr.x) / 2
                    val midY = (prev.y + curr.y) / 2
                    quadraticBezierTo(prev.x, prev.y, midX, midY)
                }
            }
            Tool.CALLIGRAPHY -> {
                for (i in 1 until drawPath.points.size) {
                    lineTo(drawPath.points[i].x, drawPath.points[i].y)
                }
            }
            Tool.SPRAY -> {
                for (i in 1 until drawPath.points.size) {
                    lineTo(drawPath.points[i].x, drawPath.points[i].y)
                }
            }
            else -> {
                for (i in 1 until drawPath.points.size) {
                    lineTo(drawPath.points[i].x, drawPath.points[i].y)
                }
            }
        }
    }

    val strokeCap = when (drawPath.tool) {
        Tool.CALLIGRAPHY -> StrokeCap.Square
        Tool.BRUSH -> StrokeCap.Round
        else -> StrokeCap.Round
    }

    val strokeWidth = when (drawPath.tool) {
        Tool.SPRAY -> drawPath.strokeWidth * 0.5f
        Tool.ERASER -> drawPath.strokeWidth * 2f
        else -> drawPath.strokeWidth
    }

    if (drawPath.tool == Tool.SPRAY) {
        drawPath.points.forEach { point ->
            val sprayRadius = drawPath.strokeWidth * 2
            for (i in 0..20) {
                val angle = (Math.random() * 360).toFloat()
                val radius = (Math.random() * sprayRadius).toFloat()
                val offsetX = (kotlin.math.cos(angle.toDouble()) * radius).toFloat()
                val offsetY = (kotlin.math.sin(angle.toDouble()) * radius).toFloat()
                drawCircle(
                    color = drawPath.color.copy(alpha = drawPath.alpha * 0.3f),
                    radius = 1.5f,
                    center = Offset(point.x + offsetX, point.y + offsetY)
                )
            }
        }
    } else {
        drawPath(
            path = path,
            color = drawPath.color.copy(alpha = drawPath.alpha),
            style = Stroke(
                width = strokeWidth,
                cap = strokeCap,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShape(shapePath: ShapePath) {
    val start = shapePath.start
    val end = shapePath.end

    when (shapePath.shape) {
        Shape.LINE -> {
            drawLine(
                color = shapePath.color.copy(alpha = shapePath.alpha),
                start = start,
                end = end,
                strokeWidth = shapePath.strokeWidth,
                cap = StrokeCap.Round
            )
        }
        Shape.RECTANGLE -> {
            val left = minOf(start.x, end.x)
            val top = minOf(start.y, end.y)
            val right = maxOf(start.x, end.x)
            val bottom = maxOf(start.y, end.y)

            if (shapePath.filled) {
                drawRect(
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top)
                )
            } else {
                drawRect(
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = shapePath.strokeWidth)
                )
            }
        }
        Shape.ROUNDED_RECTANGLE -> {
            val left = minOf(start.x, end.x)
            val top = minOf(start.y, end.y)
            val right = maxOf(start.x, end.x)
            val bottom = maxOf(start.y, end.y)

            val roundRectPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(shapePath.cornerRadius)
                    )
                )
            }

            if (shapePath.filled) {
                drawPath(
                    path = roundRectPath,
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    style = Fill
                )
            } else {
                drawPath(
                    path = roundRectPath,
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    style = Stroke(width = shapePath.strokeWidth)
                )
            }
        }
        Shape.CIRCLE -> {
            val centerX = (start.x + end.x) / 2
            val centerY = (start.y + end.y) / 2
            val radius = kotlin.math.sqrt(
                (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
            ) / 2

            if (shapePath.filled) {
                drawCircle(
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
            } else {
                drawCircle(
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = shapePath.strokeWidth)
                )
            }
        }
        Shape.TRIANGLE -> {
            val midX = (start.x + end.x) / 2
            val path = Path().apply {
                moveTo(midX, start.y)
                lineTo(end.x, end.y)
                lineTo(start.x, end.y)
                close()
            }

            if (shapePath.filled) {
                drawPath(
                    path = path,
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    style = Fill
                )
            } else {
                drawPath(
                    path = path,
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    style = Stroke(width = shapePath.strokeWidth)
                )
            }
        }
        Shape.STAR -> {
            val centerX = (start.x + end.x) / 2
            val centerY = (start.y + end.y) / 2
            val outerRadius = kotlin.math.sqrt(
                (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
            ) / 2
            val innerRadius = outerRadius * 0.4f

            val starPath = Path().apply {
                for (i in 0 until 10) {
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val angle = Math.toRadians((i * 36 - 90).toDouble())
                    val x = centerX + (radius * cos(angle)).toFloat()
                    val y = centerY + (radius * sin(angle)).toFloat()

                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }

            if (shapePath.filled) {
                drawPath(
                    path = starPath,
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    style = Fill
                )
            } else {
                drawPath(
                    path = starPath,
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    style = Stroke(width = shapePath.strokeWidth)
                )
            }
        }
        Shape.HEART -> {
            val centerX = (start.x + end.x) / 2
            val width = kotlin.math.abs(end.x - start.x)
            val height = kotlin.math.abs(end.y - start.y)

            val heartPath = Path().apply {
                moveTo(centerX, start.y + height * 0.3f)
                cubicTo(
                    start.x, start.y,
                    start.x, start.y + height * 0.6f,
                    centerX, start.y + height
                )
                cubicTo(
                    end.x, start.y + height * 0.6f,
                    end.x, start.y,
                    centerX, start.y + height * 0.3f
                )
            }

            if (shapePath.filled) {
                drawPath(
                    path = heartPath,
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    style = Fill
                )
            } else {
                drawPath(
                    path = heartPath,
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    style = Stroke(width = shapePath.strokeWidth)
                )
            }
        }
        Shape.ARROW -> {
            val path = Path().apply {
                moveTo(start.x, end.y)
                lineTo(start.x, (start.y + end.y) / 2)
                lineTo((start.x * 2 + end.x) / 3, (start.y + end.y) / 2)
                lineTo((start.x * 2 + end.x) / 3, start.y)
                lineTo(end.x, start.y)
                lineTo(end.x, (start.y + end.y) / 2)
                lineTo(start.x + (end.x - start.x) * 2 / 3, (start.y + end.y) / 2)
                lineTo(start.x + (end.x - start.x) * 2 / 3, end.y)
                close()
            }

            if (shapePath.filled) {
                drawPath(
                    path = path,
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    style = Fill
                )
            } else {
                drawPath(
                    path = path,
                    color = shapePath.color.copy(alpha = shapePath.alpha),
                    style = Stroke(width = shapePath.strokeWidth)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawText(textElement: TextElement) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = textElement.color.copy(alpha = textElement.alpha).toArgb()
            textSize = textElement.fontSize
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT

            val typeface = when {
                textElement.isBold && textElement.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                textElement.isBold -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textElement.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                else -> Typeface.DEFAULT
            }
            this.typeface = typeface
        }

        canvas.nativeCanvas.drawText(
            textElement.text,
            textElement.position.x,
            textElement.position.y,
            paint
        )
    }
}
