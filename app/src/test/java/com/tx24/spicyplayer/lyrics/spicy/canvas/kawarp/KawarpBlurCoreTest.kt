package com.tx24.spicyplayer.lyrics.spicy.canvas.kawarp

import org.junit.Assert.assertEquals
import org.junit.Test

class KawarpBlurCoreTest {

    private fun solid(w: Int, h: Int, r: Float, g: Float, b: Float): FloatArray {
        val a = FloatArray(w * h * 4)
        for (i in 0 until w * h) {
            a[i * 4] = r; a[i * 4 + 1] = g; a[i * 4 + 2] = b; a[i * 4 + 3] = 1f
        }
        return a
    }

    @Test
    fun `solid color is invariant under blur with zero tint`() {
        // Kawase taps average equal values; clamp-to-edge keeps borders solid too.
        val out = KawarpBlurCore.process(
            src = solid(64, 64, 0.25f, 0.5f, 0.75f), srcW = 64, srcH = 64,
            blurPasses = 8, tintColor = floatArrayOf(0f, 0f, 0f), tintIntensity = 0f,
        )
        assertEquals(KawarpBlurCore.BLUR_SIZE * KawarpBlurCore.BLUR_SIZE * 4, out.size)
        for (i in 0 until out.size step 4) {
            assertEquals(0.25f, out[i], 1e-4f)
            assertEquals(0.5f, out[i + 1], 1e-4f)
            assertEquals(0.75f, out[i + 2], 1e-4f)
        }
    }

    @Test
    fun `tint pulls pure black fully toward tint color`() {
        // luma(black)=0 → darkMask = 1 - smoothstep(0,0.5,0) = 1 → mix by intensity.
        val out = KawarpBlurCore.process(
            src = solid(8, 8, 0f, 0f, 0f), srcW = 8, srcH = 8,
            blurPasses = 1, tintColor = floatArrayOf(0.157f, 0.157f, 0.235f), tintIntensity = 1f,
        )
        assertEquals(0.157f, out[0], 1e-4f)
        assertEquals(0.235f, out[2], 1e-4f)
    }

    @Test
    fun `tint leaves bright pixels untouched`() {
        // luma(white)=1 ≥ 0.5 → darkMask = 0.
        val out = KawarpBlurCore.process(
            src = solid(8, 8, 1f, 1f, 1f), srcW = 8, srcH = 8,
            blurPasses = 1, tintColor = floatArrayOf(1f, 0f, 0f), tintIntensity = 1f,
        )
        assertEquals(1f, out[0], 1e-4f)
        assertEquals(1f, out[1], 1e-4f)
    }

    @Test
    fun `blur spreads an impulse and preserves total energy away from edges`() {
        // Single bright pixel in the center of a black field must diffuse outward.
        val w = 128; val h = 128
        val src = FloatArray(w * h * 4)
        val cx = 64; val cy = 64
        val ci = (cy * w + cx) * 4
        src[ci] = 1f; src[ci + 3] = 1f
        for (i in 3 until src.size step 4) src[i] = 1f
        val out = KawarpBlurCore.process(src, w, h, blurPasses = 3,
            tintColor = floatArrayOf(0f, 0f, 0f), tintIntensity = 0f)
        // center is dimmer than input, neighbors picked up energy
        val center = out[ci]
        val neighbor = out[((cy + 4) * w + cx) * 4]
        org.junit.Assert.assertTrue("center dimmed", center < 1f)
        org.junit.Assert.assertTrue("energy spread", neighbor > 0f)
    }
}
