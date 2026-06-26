package com.mimo.draw.engine

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlin.math.sqrt

class PressureEngine {
    var minPressure = 0.05f
    var maxPressure = 1.0f
    var sensitivity = 1.5f
    var smoothingFactor = 0.35f

    private val pressureBuffer = FloatArray(PRESSURE_BUFFER_SIZE)
    private var bufferIndex = 0
    private var lastSmoothedPressure = 0.5f
    private var velocityTracker = VelocityTracker()

    data class PressurePoint(
        val position: Offset,
        val pressure: Float,
        val timestamp: Long,
        val tiltX: Float = 0f,
        val tiltY: Float = 0f,
        val velocity: Float = 0f
    )

    fun processMotionEvent(event: MotionEvent): PressurePoint? {
        if (event.actionMasked != MotionEvent.ACTION_MOVE &&
            event.actionMasked != MotionEvent.ACTION_DOWN &&
            event.actionMasked != MotionEvent.ACTION_POINTER_DOWN) {
            return null
        }

        val rawPressure = event.getAxisValue(MotionEvent.AXIS_PRESSURE).coerceIn(0f, 1f)
        val tiltX = event.getAxisValue(MotionEvent.AXIS_TILT_X)
        val tiltY = event.getAxisValue(MotionEvent.AXIS_TILT_Y)

        val smoothedPressure = smoothPressure(rawPressure)

        velocityTracker.addEvent(
            Offset(event.x, event.y),
            event.eventTime
        )
        val velocity = velocityTracker.getVelocity()

        return PressurePoint(
            position = Offset(event.x, event.y),
            pressure = mapPressure(smoothedPressure),
            timestamp = event.eventTime,
            tiltX = tiltX,
            tiltY = tiltY,
            velocity = velocity
        )
    }

    private fun smoothPressure(raw: Float): Float {
        pressureBuffer[bufferIndex % PRESSURE_BUFFER_SIZE] = raw
        bufferIndex++

        val count = minOf(bufferIndex, PRESSURE_BUFFER_SIZE)
        var sum = 0f
        var weightSum = 0f

        for (i in 0 until count) {
            val idx = (bufferIndex - 1 - i).coerceAtLeast(0) % PRESSURE_BUFFER_SIZE
            val weight = 1f / (1f + i * 0.5f)
            sum += pressureBuffer[idx] * weight
            weightSum += weight
        }

        val averaged = if (weightSum > 0) sum / weightSum else raw
        lastSmoothedPressure = lastSmoothedPressure * (1f - smoothingFactor) + averaged * smoothingFactor

        return lastSmoothedPressure.coerceIn(0f, 1f)
    }

    private fun mapPressure(pressure: Float): Float {
        val normalized = (pressure - minPressure) / (maxPressure - minPressure)
        val clamped = normalized.coerceIn(0f, 1f)
        return clamped.pow(sensitivity)
    }

    fun reset() {
        bufferIndex = 0
        lastSmoothedPressure = 0.5f
        pressureBuffer.fill(0f)
        velocityTracker.reset()
    }

    class VelocityTracker {
        private data class Event(val position: Offset, val timestamp: Long)
        private val events = ArrayList<Event>(MAX_EVENTS)
        private var lastVelocity = 0f

        fun addEvent(position: Offset, timestamp: Long) {
            events.add(Event(position, timestamp))
            if (events.size > MAX_EVENTS) {
                events.removeAt(0)
            }
        }

        fun getVelocity(): Float {
            if (events.size < 2) return 0f

            val recent = events.takeLast(5)
            if (recent.size < 2) return lastVelocity

            var totalDistance = 0f
            var totalTime = 0L

            for (i in 1 until recent.size) {
                val dx = recent[i].position.x - recent[i - 1].position.x
                val dy = recent[i].position.y - recent[i - 1].position.y
                totalDistance += sqrt(dx * dx + dy * dy)
                totalTime += recent[i].timestamp - recent[i - 1].timestamp
            }

            if (totalTime > 0) {
                lastVelocity = (totalDistance / totalTime * 1000f)
            }

            return lastVelocity
        }

        fun reset() {
            events.clear()
            lastVelocity = 0f
        }

        companion object {
            private const val MAX_EVENTS = 20
        }
    }

    companion object {
        private const val PRESSURE_BUFFER_SIZE = 8
    }
}

private fun Float.pow(exp: Float): Float {
    return kotlin.math.pow(this.toDouble(), exp.toDouble()).toFloat()
}

fun PointerInputScope.setupPressureInput(
    onPoint: (PressureEngine.PressurePoint) -> Unit,
    onEnd: () -> Unit
) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
                if (change.pressed) {
                    val pressure = change.pressure
                    onPoint(
                        PressureEngine.PressurePoint(
                            position = change.position,
                            pressure = pressure.coerceIn(0.05f, 1f),
                            timestamp = System.nanoTime()
                        )
                    )
                }
            }
            if (event.changes.all { !it.pressed }) {
                onEnd()
            }
        }
    }
}
