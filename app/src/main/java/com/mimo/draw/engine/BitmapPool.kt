package com.mimo.draw.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class BitmapPool(private val maxSize: Int = MAX_POOL_SIZE) {
    private val pool = ConcurrentLinkedQueue<Bitmap>()
    private val totalAllocated = AtomicInteger(0)
    private val hitCount = AtomicInteger(0)
    private val missCount = AtomicInteger(0)

    fun acquire(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        val targetWidth = width.coerceAtLeast(1)
        val targetHeight = height.coerceAtLeast(1)

        val cached = pool.firstOrNull { bitmap ->
            bitmap.width == targetWidth &&
            bitmap.height == targetHeight &&
            bitmap.config == config &&
            !bitmap.isRecycled
        }

        return if (cached != null) {
            pool.remove(cached)
            hitCount.incrementAndGet()
            cached
        } else {
            missCount.incrementAndGet()
            totalAllocated.addAndGet(targetWidth * targetHeight * 4)
            Bitmap.createBitmap(targetWidth, targetHeight, config)
        }
    }

    fun release(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return

        if (pool.size < maxSize) {
            bitmap.eraseColor(0)
            pool.offer(bitmap)
        } else {
            bitmap.recycle()
            totalAllocated.addAndGet(-bitmap.width * bitmap.height * 4)
        }
    }

    fun clear() {
        pool.forEach { it.recycle() }
        pool.clear()
        totalAllocated.set(0)
    }

    fun getStats(): PoolStats {
        return PoolStats(
            poolSize = pool.size,
            totalAllocated = totalAllocated.get(),
            hitRate = if (hitCount.get() + missCount.get() > 0) {
                hitCount.get().toFloat() / (hitCount.get() + missCount.get())
            } else 0f
        )
    }

    data class PoolStats(
        val poolSize: Int,
        val totalAllocated: Int,
        val hitRate: Float
    )

    companion object {
        const val MAX_POOL_SIZE = 64
    }
}

class LayerBitmapManager(private val bitmapPool: BitmapPool) {
    private val layers = LinkedHashMap<String, LayerBitmap>(16, 0.75f, true)
    private val canvasCache = HashMap<String, Canvas>(16)

    data class LayerBitmap(
        val id: String,
        val bitmap: Bitmap,
        var isDirty: Boolean = true,
        var isVisible: Boolean = true,
        var opacity: Float = 1f
    )

    fun createLayer(id: String, width: Int, height: Int): LayerBitmap {
        val existing = layers[id]
        if (existing != null) {
            bitmapPool.release(existing.bitmap)
        }

        val bitmap = bitmapPool.acquire(width, height)
        val layer = LayerBitmap(id = id, bitmap = bitmap, isDirty = true)
        layers[id] = layer
        return layer
    }

    fun removeLayer(id: String) {
        val layer = layers.remove(id)
        if (layer != null) {
            bitmapPool.release(layer.bitmap)
            canvasCache.remove(id)
        }
    }

    fun getCanvas(layerId: String): Canvas {
        return canvasCache.getOrPut(layerId) {
            val layer = layers[layerId] ?: throw IllegalArgumentException("Layer $layerId not found")
            Canvas(layer.bitmap)
        }
    }

    fun getLayer(id: String): LayerBitmap? = layers[id]

    fun markDirty(id: String) {
        layers[id]?.isDirty = true
    }

    fun compositeToTarget(target: Bitmap) {
        val canvas = Canvas(target)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)

        for ((_, layer) in layers) {
            if (!layer.isVisible || layer.opacity <= 0f) continue

            val paint = Paint().apply {
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                isAntiAlias = true
            }

            canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
        }
    }

    fun compositeRegion(
        target: Bitmap,
        left: Int, top: Int, right: Int, bottom: Int
    ) {
        val canvas = Canvas(target)
        val rect = Rect(left, top, right, bottom)

        for ((_, layer) in layers) {
            if (!layer.isVisible || layer.opacity <= 0f) continue

            val paint = Paint().apply {
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                isAntiAlias = true
            }

            canvas.drawBitmap(layer.bitmap, rect, rect, paint)
        }
    }

    fun clear() {
        for ((_, layer) in layers) {
            bitmapPool.release(layer.bitmap)
        }
        layers.clear()
        canvasCache.clear()
    }

    fun getLayerCount(): Int = layers.size

    fun getAllLayers(): List<LayerBitmap> = layers.values.toList()

    fun reorderLayer(id: String, newIndex: Int) {
        val layer = layers.remove(id) ?: return
        val entries = ArrayList(layers.entries)

        layers.clear()
        val insertIndex = newIndex.coerceIn(0, entries.size)

        for (i in 0 until insertIndex) {
            layers[entries[i].key] = entries[i].value
        }
        layers[id] = layer
        for (i in insertIndex until entries.size) {
            layers[entries[i].key] = entries[i].value
        }
    }
}

class FilterEngine(private val bitmapPool: BitmapPool) {
    private val tempBitmaps = ArrayList<Bitmap>(4)

    fun applyBlur(input: Bitmap, radius: Float): Bitmap {
        val width = input.width
        val height = input.height
        val output = getTempBitmap(width, height)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
        }

        if (radius > 0) {
            paint.maskFilter = android.graphics.BlurMaskFilter(
                radius * 2f,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }

        canvas.drawBitmap(input, 0f, 0f, paint)
        return output
    }

    fun applyBrightness(input: Bitmap, factor: Float): Bitmap {
        val width = input.width
        val height = input.height
        val output = getTempBitmap(width, height)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply {
                    set(floatArrayOf(
                        factor, 0f, 0f, 0f, 0f,
                        0f, factor, 0f, 0f, 0f,
                        0f, 0f, factor, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
            )
        }

        canvas.drawBitmap(input, 0f, 0f, paint)
        return output
    }

    fun applyContrast(input: Bitmap, factor: Float): Bitmap {
        val width = input.width
        val height = input.height
        val output = getTempBitmap(width, height)

        val canvas = Canvas(output)
        val translate = (1f - factor) * 127.5f
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply {
                    set(floatArrayOf(
                        factor, 0f, 0f, 0f, translate,
                        0f, factor, 0f, 0f, translate,
                        0f, 0f, factor, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
            )
        }

        canvas.drawBitmap(input, 0f, 0f, paint)
        return output
    }

    fun applySaturation(input: Bitmap, factor: Float): Bitmap {
        val width = input.width
        val height = input.height
        val output = getTempBitmap(width, height)

        val canvas = Canvas(output)
        val inverseFactor = 1f - factor
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply {
                    set(floatArrayOf(
                        0.213f + 0.787f * factor, 0.715f - 0.715f * factor, 0.072f - 0.072f * factor, 0f, 0f,
                        0.213f - 0.213f * factor, 0.715f + 0.285f * factor, 0.072f - 0.072f * factor, 0f, 0f,
                        0.213f - 0.213f * factor, 0.715f - 0.715f * factor, 0.072f + 0.928f * factor, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
            )
        }

        canvas.drawBitmap(input, 0f, 0f, paint)
        return output
    }

    fun applyHueRotation(input: Bitmap, degrees: Float): Bitmap {
        val width = input.width
        val height = input.height
        val output = getTempBitmap(width, height)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply {
                    setRotate(0, degrees)
                    preConcat(android.graphics.ColorMatrix().apply {
                        setRotate(1, degrees)
                    })
                    preConcat(android.graphics.ColorMatrix().apply {
                        setRotate(2, degrees)
                    })
                }
            )
        }

        canvas.drawBitmap(input, 0f, 0f, paint)
        return output
    }

    fun applyGrayscale(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val output = getTempBitmap(width, height)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply {
                    setSaturation(0f)
                }
            )
        }

        canvas.drawBitmap(input, 0f, 0f, paint)
        return output
    }

    fun applySepia(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val output = getTempBitmap(width, height)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply {
                    set(floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
            )
        }

        canvas.drawBitmap(input, 0f, 0f, paint)
        return output
    }

    fun applyInvert(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val output = getTempBitmap(width, height)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply {
                    set(floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
            )
        }

        canvas.drawBitmap(input, 0f, 0f, paint)
        return output
    }

    fun applySharpen(input: Bitmap, factor: Float): Bitmap {
        val width = input.width
        val height = input.height
        val output = getTempBitmap(width, height)

        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = false
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply {
                    val f = factor
                    set(floatArrayOf(
                        1f + 4f * f, -f, -f,
                        -f, 1f + 4f * f, -f,
                        -f, -f, 1f + 4f * f
                    ))
                }
            )
        }

        canvas.drawBitmap(input, 0f, 0f, paint)
        return output
    }

    private fun getTempBitmap(width: Int, height: Int): Bitmap {
        val bitmap = bitmapPool.acquire(width, height)
        tempBitmaps.add(bitmap)
        return bitmap
    }

    fun recycleTempBitmaps() {
        for (bitmap in tempBitmaps) {
            bitmapPool.release(bitmap)
        }
        tempBitmaps.clear()
    }
}
