package com.galeriara.data.encoding

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Processes an already EXIF-oriented Bitmap. URI decoding remains outside this low-level helper. */
object ClipImagePreprocessor {
    private const val SIZE = 224
    private val mean = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
    private val std = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

    fun toTensor(bitmap: Bitmap): FloatArray {
        require(!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0)
        val width = bitmap.width
        val height = bitmap.height
        val shortest = min(width, height)
        val resizedWidth = if (width <= height) SIZE else (SIZE.toDouble() * width / shortest).toInt()
        val resizedHeight = if (height <= width) SIZE else (SIZE.toDouble() * height / shortest).toInt()
        val source = IntArray(Math.multiplyExact(width, height))
        bitmap.getPixels(source, 0, width, 0, 0, width, height)
        // getPixels exposes unpremultiplied channels; composite alpha over white before resizing.
        source.indices.forEach { index ->
            val pixel = source[index]
            val alpha = pixel ushr 24
            fun channel(shift: Int): Int = ((((pixel ushr shift) and 255) * alpha +
                255 * (255 - alpha)) / 255.0).roundToInt()
            source[index] = (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        }
        val horizontal = IntArray(Math.multiplyExact(resizedWidth, height))
        val xWeights = Array(resizedWidth) { coefficients(width, resizedWidth, it) }
        for (y in 0 until height) for (x in 0 until resizedWidth) {
            val (start, weights) = xWeights[x]
            val channels = DoubleArray(3)
            weights.indices.forEach { i ->
                val pixel = source[y * width + start + i]
                channels[0] += ((pixel ushr 16) and 255) * weights[i]
                channels[1] += ((pixel ushr 8) and 255) * weights[i]
                channels[2] += (pixel and 255) * weights[i]
            }
            horizontal[y * resizedWidth + x] = (255 shl 24) or
                (channels[0].roundToInt().coerceIn(0, 255) shl 16) or
                (channels[1].roundToInt().coerceIn(0, 255) shl 8) or
                channels[2].roundToInt().coerceIn(0, 255)
        }
        val left = (resizedWidth - SIZE) / 2
        val top = (resizedHeight - SIZE) / 2
        val tensor = FloatArray(3 * SIZE * SIZE)
        for (y in 0 until SIZE) {
            val (start, weights) = coefficients(height, resizedHeight, top + y)
            for (x in 0 until SIZE) {
                val channels = DoubleArray(3)
                weights.indices.forEach { i ->
                    val pixel = horizontal[(start + i) * resizedWidth + left + x]
                    channels[0] += ((pixel ushr 16) and 255) * weights[i]
                    channels[1] += ((pixel ushr 8) and 255) * weights[i]
                    channels[2] += (pixel and 255) * weights[i]
                }
                for (c in 0..2) {
                    val byteValue = channels[c].roundToInt().coerceIn(0, 255)
                    tensor[c * SIZE * SIZE + y * SIZE + x] =
                        ((byteValue / 255.0).toFloat() - mean[c]) / std[c]
                }
            }
        }
        return tensor
    }

    // Separable antialiased Catmull-Rom (a=-0.5), matching Pillow's BICUBIC geometry.
    private fun coefficients(sourceSize: Int, targetSize: Int, targetIndex: Int): Pair<Int, DoubleArray> {
        val scale = sourceSize.toDouble() / targetSize
        val filterScale = max(1.0, scale)
        val center = (targetIndex + 0.5) * scale
        val start = max(0, (center - 2 * filterScale + 0.5).toInt())
        val end = min(sourceSize, (center + 2 * filterScale + 0.5).toInt())
        val weights = DoubleArray(end - start) { i ->
            val x = abs((start + i + 0.5 - center) / filterScale)
            when {
                x < 1 -> ((1.5 * x - 2.5) * x) * x + 1
                x < 2 -> ((-0.5 * x + 2.5) * x - 4) * x + 2
                else -> 0.0
            }
        }
        val sum = weights.sum()
        require(sum > 0)
        weights.indices.forEach { weights[it] /= sum }
        return start to weights
    }
}
