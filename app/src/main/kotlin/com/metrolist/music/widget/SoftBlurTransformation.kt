/**
 * Adapted from Pear Music's low-resolution cached blur implementation.
 * Original project: https://github.com/dragonzords/Pear-Music
 * GPL-3.0; Metrofy adaptation dated 2026-11-19.
 */
package com.metrolist.music.widget

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.max

/** Three horizontal/vertical box-blur passes on a small bitmap, cached by Coil. */
class SoftBlurTransformation(
    private val width: Int = 64,
    private val radius: Int = 4,
) : Transformation() {
    override val cacheKey: String = "softblur-$width-$radius"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = width
        val h = max(1, input.height * w / max(1, input.width))
        val source = if (input.config == Bitmap.Config.HARDWARE) {
            input.copy(Bitmap.Config.ARGB_8888, false)
        } else input
        val small = Bitmap.createScaledBitmap(source, w, h, true)
        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        repeat(3) {
            boxBlur(pixels, w, h, radius, horizontal = true)
            boxBlur(pixels, w, h, radius, horizontal = false)
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int, horizontal: Boolean) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val line = IntArray(length)
        val divisor = 2 * radius + 1
        for (lineIndex in 0 until lines) {
            for (index in 0 until length) {
                line[index] = pixels[if (horizontal) lineIndex * width + index else index * width + lineIndex]
            }
            var alpha = 0
            var red = 0
            var green = 0
            var blue = 0
            for (offset in -radius..radius) {
                val color = line[offset.coerceIn(0, length - 1)]
                alpha += color ushr 24
                red += (color shr 16) and 0xFF
                green += (color shr 8) and 0xFF
                blue += color and 0xFF
            }
            for (index in 0 until length) {
                val target = if (horizontal) lineIndex * width + index else index * width + lineIndex
                pixels[target] = ((alpha / divisor) shl 24) or
                    ((red / divisor) shl 16) or ((green / divisor) shl 8) or (blue / divisor)
                val outgoing = line[(index - radius).coerceIn(0, length - 1)]
                val incoming = line[(index + radius + 1).coerceIn(0, length - 1)]
                alpha += (incoming ushr 24) - (outgoing ushr 24)
                red += ((incoming shr 16) and 0xFF) - ((outgoing shr 16) and 0xFF)
                green += ((incoming shr 8) and 0xFF) - ((outgoing shr 8) and 0xFF)
                blue += (incoming and 0xFF) - (outgoing and 0xFF)
            }
        }
    }
}
