package com.tx24.spicyplayer.uiNowPlaying.spicy.canvas

import android.graphics.Bitmap

/**
 * Simple box blur implementation for pre-blurring small bitmaps.
 * Applied iteratively (3 passes) to approximate a Gaussian blur.
 * Designed for use on small, downscaled bitmaps (e.g. 128×128).
 */
object StackBlur {

    /**
     * Applies an iterative box blur to a copy of the given bitmap.
     *
     * @param source The source bitmap (any config, not modified).
     * @param radius The blur radius.
     * @param iterations Number of box-blur passes (3 approximates Gaussian).
     * @return A new blurred ARGB_8888 bitmap.
     */
    fun blur(source: Bitmap, radius: Int, iterations: Int = 3): Bitmap {
        if (radius <= 0) return source.copy(Bitmap.Config.ARGB_8888, true)

        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
        copy.getPixels(pixels, 0, w, 0, 0, w, h)

        val buffer = IntArray(w * h)

        repeat(iterations) {
            // Horizontal pass
            for (y in 0 until h) {
                val rowOffset = y * w
                // Sliding window sums
                var rSum = 0L; var gSum = 0L; var bSum = 0L; var aSum = 0L
                val windowSize = (2 * radius + 1)

                // Seed the window with (radius+1) leftmost pixels
                for (ix in -radius..radius) {
                    val px = pixels[rowOffset + ix.coerceIn(0, w - 1)]
                    aSum += (px ushr 24) and 0xFF
                    rSum += (px shr 16) and 0xFF
                    gSum += (px shr 8) and 0xFF
                    bSum += px and 0xFF
                }

                for (x in 0 until w) {
                    buffer[rowOffset + x] = (
                        ((aSum / windowSize).toInt().coerceIn(0, 255) shl 24) or
                        ((rSum / windowSize).toInt().coerceIn(0, 255) shl 16) or
                        ((gSum / windowSize).toInt().coerceIn(0, 255) shl 8) or
                        (bSum / windowSize).toInt().coerceIn(0, 255)
                    )

                    // Remove the leftmost pixel of the window
                    val removeX = (x - radius).coerceIn(0, w - 1)
                    val removePx = pixels[rowOffset + removeX]
                    aSum -= (removePx ushr 24) and 0xFF
                    rSum -= (removePx shr 16) and 0xFF
                    gSum -= (removePx shr 8) and 0xFF
                    bSum -= removePx and 0xFF

                    // Add the next pixel to the right of the window
                    val addX = (x + radius + 1).coerceIn(0, w - 1)
                    val addPx = pixels[rowOffset + addX]
                    aSum += (addPx ushr 24) and 0xFF
                    rSum += (addPx shr 16) and 0xFF
                    gSum += (addPx shr 8) and 0xFF
                    bSum += addPx and 0xFF
                }
            }

            // Vertical pass
            for (x in 0 until w) {
                var rSum = 0L; var gSum = 0L; var bSum = 0L; var aSum = 0L
                val windowSize = (2 * radius + 1)

                for (iy in -radius..radius) {
                    val px = buffer[iy.coerceIn(0, h - 1) * w + x]
                    aSum += (px ushr 24) and 0xFF
                    rSum += (px shr 16) and 0xFF
                    gSum += (px shr 8) and 0xFF
                    bSum += px and 0xFF
                }

                for (y in 0 until h) {
                    pixels[y * w + x] = (
                        ((aSum / windowSize).toInt().coerceIn(0, 255) shl 24) or
                        ((rSum / windowSize).toInt().coerceIn(0, 255) shl 16) or
                        ((gSum / windowSize).toInt().coerceIn(0, 255) shl 8) or
                        (bSum / windowSize).toInt().coerceIn(0, 255)
                    )

                    val removeY = (y - radius).coerceIn(0, h - 1)
                    val removePx = buffer[removeY * w + x]
                    aSum -= (removePx ushr 24) and 0xFF
                    rSum -= (removePx shr 16) and 0xFF
                    gSum -= (removePx shr 8) and 0xFF
                    bSum -= removePx and 0xFF

                    val addY = (y + radius + 1).coerceIn(0, h - 1)
                    val addPx = buffer[addY * w + x]
                    aSum += (addPx ushr 24) and 0xFF
                    rSum += (addPx shr 16) and 0xFF
                    gSum += (addPx shr 8) and 0xFF
                    bSum += addPx and 0xFF
                }
            }
        }

        copy.setPixels(pixels, 0, w, 0, 0, w, h)
        return copy
    }
}
