package com.tx24.spicyplayer.lyrics.spicy.canvas.kawarp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KawarpEngineTest {

    @Test
    fun `defaults match kawarp core 1_2_0`() {
        val o = KawarpOptions()
        assertEquals(1.0f, o.warpIntensity, 0f)
        assertEquals(8, o.blurPasses)
        assertEquals(1.0f, o.animationSpeed, 0f)
        assertEquals(1000f, o.transitionDuration, 0f)
        assertEquals(1.5f, o.saturation, 0f)
        assertEquals(0.15f, o.tintIntensity, 0f)
        assertEquals(0.008f, o.dithering, 0f)
        assertEquals(1.0f, o.scale, 0f)
        assertEquals(0.157f, o.tintColor[0], 0f)
        assertEquals(0.157f, o.tintColor[1], 0f)
        assertEquals(0.235f, o.tintColor[2], 0f)
    }

    @Test
    fun `speed smoothing lerps 5 percent per tick toward target`() {
        // Original: _animationSpeed += (_target - _animationSpeed) * 0.05 each frame.
        val e = KawarpEngine(KawarpOptions(animationSpeed = 1f))
        e.targetAnimationSpeed = 0.1f
        e.tick(1f) // one frame of 1s
        // after one tick speed = 1 + (0.1 - 1) * 0.05 = 0.955; time += 1 * 0.955
        assertEquals(0.955f, e.currentAnimationSpeed, 1e-6f)
        assertEquals(0.955f, e.accumulatedTime, 1e-6f)
    }

    @Test
    fun `target speed clamped to 0_1 to 5`() {
        val e = KawarpEngine(KawarpOptions())
        e.targetAnimationSpeed = 99f
        assertEquals(5f, e.targetAnimationSpeed, 0f)
        e.targetAnimationSpeed = 0f
        assertEquals(0.1f, e.targetAnimationSpeed, 0f)
    }

    @Test
    fun `blend factor ramps over transitionDuration and completes`() {
        val e = KawarpEngine(KawarpOptions(transitionDuration = 500f))
        e.startTransition(nowMs = 1000L)
        assertTrue(e.isTransitioning)
        assertEquals(0.5f, e.blendFactor(1250L), 1e-6f)
        assertEquals(1.0f, e.blendFactor(1500L), 0f)
        assertFalse(e.isTransitioning) // completing the ramp ends the transition
        assertEquals(1.0f, e.blendFactor(9999L), 0f)
    }

    @Test
    fun `no transition means blend factor 1`() {
        val e = KawarpEngine(KawarpOptions())
        assertEquals(1.0f, e.blendFactor(0L), 0f)
    }

    @Test
    fun `transitionDuration clamped to 0 to 5000`() {
        val e = KawarpEngine(KawarpOptions())
        e.transitionDuration = 9000f
        assertEquals(5000f, e.transitionDuration, 0f)
        e.transitionDuration = -1f
        assertEquals(0f, e.transitionDuration, 0f)
    }
}
