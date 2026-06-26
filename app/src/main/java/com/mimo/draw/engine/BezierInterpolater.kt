package com.mimo.draw.engine

import androidx.compose.ui.geometry.Offset

class BezierInterpolater {
    private val points = ArrayList<StrokePoint>(256)
    private var tension = 0.5f

    data class StrokePoint(
        val position: Offset,
        val pressure: Float,
        val timestamp: Long
    )

    data class InterpolatedSegment(
        val points: List<Offset>,
        val widths: List<Float>
    )

    fun setTension(t: Float) {
        tension = t.coerceIn(0f, 1f)
    }

    fun addPoint(position: Offset, pressure: Float, timestamp: Long) {
        points.add(StrokePoint(position, pressure, timestamp))
    }

    fun interpolate(): InterpolatedSegment {
        if (points.isEmpty()) return InterpolatedSegment(emptyList(), emptyList())
        if (points.size == 1) {
            return InterpolatedSegment(
                listOf(points[0].position),
                listOf(pressureToWidth(points[0].pressure))
            )
        }

        val resultPoints = ArrayList<Offset>()
        val resultWidths = ArrayList<Float>()

        resultPoints.add(points[0].position)
        resultWidths.add(pressureToWidth(points[0].pressure))

        for (i in 1 until points.size) {
            val prev = points[maxOf(0, i - 2)]
            val curr = points[i - 1]
            val next = points[i]
            val afterNext = points[minOf(points.size - 1, i + 1)]

            val segments = calculateSubdivisions(prev, curr, next, afterNext)

            for (seg in segments) {
                resultPoints.add(seg.first)
                resultWidths.add(pressureToWidth(seg.second))
            }
        }

        return InterpolatedSegment(resultPoints, resultWidths)
    }

    fun interpolateIncremental(): InterpolatedSegment {
        if (points.size < 2) return InterpolatedSegment(emptyList(), emptyList())

        val resultPoints = ArrayList<Offset>()
        val resultWidths = ArrayList<Float>()

        val n = points.size
        if (n >= 4) {
            val p0 = points[n - 4]
            val p1 = points[n - 3]
            val p2 = points[n - 2]
            val p3 = points[n - 1]

            val segments = calculateSubdivisions(p0, p1, p2, p3)

            for (seg in segments) {
                resultPoints.add(seg.first)
                resultWidths.add(pressureToWidth(seg.second))
            }
        } else if (n >= 2) {
            val p1 = points[n - 2]
            val p2 = points[n - 1]
            resultPoints.add(p1.position)
            resultWidths.add(pressureToWidth(p1.pressure))
            resultPoints.add(p2.position)
            resultWidths.add(pressureToWidth(p2.pressure))
        }

        return InterpolatedSegment(resultPoints, resultWidths)
    }

    private fun calculateSubdivisions(
        p0: StrokePoint, p1: StrokePoint, p2: StrokePoint, p3: StrokePoint
    ): List<Pair<Offset, Float>> {
        val result = ArrayList<Pair<Offset, Float>>()
        val steps = calculateSteps(p1, p2)

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val point = catmullRom(p0.position, p1.position, p2.position, p3.position, t)
            val pressure = lerp(p1.pressure, p2.pressure, t)
            result.add(Pair(point, pressure))
        }

        return result
    }

    private fun catmullRom(
        p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float
    ): Offset {
        val t2 = t * t
        val t3 = t2 * t

        val x = 0.5f * (
            (2f * p1.x) +
            (-p0.x + p2.x) * t +
            (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
            (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3
        )

        val y = 0.5f * (
            (2f * p1.y) +
            (-p0.y + p2.y) * t +
            (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
            (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3
        )

        return Offset(x, y)
    }

    private fun calculateSteps(p1: StrokePoint, p2: StrokePoint): Int {
        val dx = p2.position.x - p1.position.x
        val dy = p2.position.y - p1.position.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        val velocity = if (p2.timestamp > p1.timestamp) {
            distance / (p2.timestamp - p1.timestamp).coerceAtLeast(1)
        } else {
            0f
        }

        val baseSteps = (distance / POINT_DENSITY).toInt().coerceIn(MIN_SEGMENTS, MAX_SEGMENTS)
        val velocityAdjustment = (velocity * VELOCITY_STEP_FACTOR).toInt().coerceIn(0, 4)

        return (baseSteps + velocityAdjustment).coerceIn(MIN_SEGMENTS, MAX_SEGMENTS)
    }

    private fun pressureToWidth(pressure: Float): Float {
        val normalized = pressure.coerceIn(0.05f, 1f)
        return MIN_WIDTH + (MAX_WIDTH - MIN_WIDTH) * normalized * normalized
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    fun clear() {
        points.clear()
    }

    fun getPoints(): List<StrokePoint> = points.toList()

    companion object {
        private const val POINT_DENSITY = 2f
        private const val MIN_SEGMENTS = 2
        private const val MAX_SEGMENTS = 16
        private const val VELOCITY_STEP_FACTOR = 0.005f
        private const val MIN_WIDTH = 0.5f
        private const val MAX_WIDTH = 48f
    }
}
