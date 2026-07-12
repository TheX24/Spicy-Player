package com.tx24.spicyplayer.lyrics.spicy.canvas.kawarp

/**
 * CPU port of @kawarp/core's image-change pipeline (`blurSourceInto`):
 *   1. bilinear-downsample the source to 128×128 (the GL tint pass's LINEAR sampling),
 *   2. tint dark areas toward tintColor (TINT_SHADER),
 *   3. run `blurPasses` Kawase passes with offset i+0.5, clamp-to-edge bilinear taps
 *      (KAWASE_BLUR_SHADER).
 * Runs once per cover change — 128×128 float math, sub-millisecond territory.
 */
object KawarpBlurCore {

    const val BLUR_SIZE = 128

    fun process(
        src: FloatArray,
        srcW: Int,
        srcH: Int,
        blurPasses: Int,
        tintColor: FloatArray,
        tintIntensity: Float,
    ): FloatArray {
        val n = BLUR_SIZE
        var read = FloatArray(n * n * 4)
        var write = FloatArray(n * n * 4)
        val sample = FloatArray(4)

        // Pass 0: downsample + tint. GL samples the source at each 128-grid texel
        // center with LINEAR filtering; bilinearSample reproduces that.
        var idx = 0
        for (y in 0 until n) {
            val v = (y + 0.5f) / n
            for (x in 0 until n) {
                val u = (x + 0.5f) / n
                bilinearSample(src, srcW, srcH, u * srcW - 0.5f, v * srcH - 0.5f, sample)
                var r = sample[0]; var g = sample[1]; var b = sample[2]
                val luma = 0.299f * r + 0.587f * g + 0.114f * b
                // darkMask = 1 - smoothstep(0, 0.5, luma)
                val t = (luma / 0.5f).coerceIn(0f, 1f)
                val darkMask = 1f - (t * t * (3f - 2f * t))
                val k = darkMask * tintIntensity
                r += (tintColor[0] - r) * k
                g += (tintColor[1] - g) * k
                b += (tintColor[2] - b) * k
                read[idx] = r; read[idx + 1] = g; read[idx + 2] = b; read[idx + 3] = sample[3]
                idx += 4
            }
        }

        // Kawase passes: 4 diagonal bilinear taps at ±(i+0.5) texels, averaged.
        val passes = blurPasses.coerceIn(1, 40)
        for (i in 0 until passes) {
            val off = i + 0.5f
            idx = 0
            for (y in 0 until n) {
                for (x in 0 until n) {
                    var r = 0f; var g = 0f; var b = 0f; var a = 0f
                    // fragment center in pixel coords is (x+0.5, y+0.5); taps at ±off
                    // texels land back on grid as (x±off-0.0) in bilinear pixel space.
                    for (q in 0 until 4) {
                        val sx = x + (if (q and 1 == 0) -off else off)
                        val sy = y + (if (q and 2 == 0) -off else off)
                        bilinearSample(read, n, n, sx, sy, sample)
                        r += sample[0]; g += sample[1]; b += sample[2]; a += sample[3]
                    }
                    write[idx] = r * 0.25f; write[idx + 1] = g * 0.25f
                    write[idx + 2] = b * 0.25f; write[idx + 3] = a * 0.25f
                    idx += 4
                }
            }
            val tmp = read; read = write; write = tmp
        }
        return read
    }

    /** Clamp-to-edge bilinear sample at pixel-space (px, py); texel centers at integers. */
    private fun bilinearSample(
        img: FloatArray, w: Int, h: Int, px: Float, py: Float, out: FloatArray,
    ) {
        val x0f = kotlin.math.floor(px); val y0f = kotlin.math.floor(py)
        val fx = px - x0f; val fy = py - y0f
        val x0 = x0f.toInt().coerceIn(0, w - 1); val x1 = (x0f.toInt() + 1).coerceIn(0, w - 1)
        val y0 = y0f.toInt().coerceIn(0, h - 1); val y1 = (y0f.toInt() + 1).coerceIn(0, h - 1)
        val i00 = (y0 * w + x0) * 4; val i10 = (y0 * w + x1) * 4
        val i01 = (y1 * w + x0) * 4; val i11 = (y1 * w + x1) * 4
        for (c in 0 until 4) {
            val top = img[i00 + c] + (img[i10 + c] - img[i00 + c]) * fx
            val bot = img[i01 + c] + (img[i11 + c] - img[i01 + c]) * fx
            out[c] = top + (bot - top) * fy
        }
    }
}
