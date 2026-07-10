package com.tx24.spicyplayer.lyrics.spicy.animation

import androidx.compose.runtime.MonotonicFrameClock
import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario tests for the reference state machine: NotSung lines freeze, Active lines animate,
 * Sung lines keep settling only until the next line is itself Sung.
 */
class LyricsAnimatorTest {

    /** Virtual frame clock so Compose Animatables complete synchronously on Unconfined. */
    private class TestFrameClock : MonotonicFrameClock {
        private var timeNanos = 0L
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            timeNanos += 16_000_000L
            return onFrame(timeNanos)
        }
    }

    private fun animator() =
        LyricsAnimator(CoroutineScope(Dispatchers.Unconfined + TestFrameClock()), RenderConfig.FULL)

    private val lines = listOf(
        Line(words = listOf(Word("Hello", 0L, 1000L)), startMs = 0L),
        Line(words = listOf(Word("world", 2000L, 3000L)), startMs = 2000L),
    )

    private val frameDt = 1f / 60f

    @Test
    fun `not-sung lines are frozen - word states are the same instance across frames`() {
        val a = animator()
        val first = a.animate(lines, currentTimeMs = 500L, deltaTime = frameDt)
        val second = a.animate(lines, currentTimeMs = 520L, deltaTime = frameDt)
        // Line 2 is NotSung: its word states must be reused verbatim, not recomputed.
        assertSame(first[1].wordStates, second[1].wordStates)
        // And they are the initial resting state.
        assertEquals(-20f, second[1].wordStates[0].gradientPosition, 0.001f)
        assertEquals(0.95f, second[1].wordStates[0].scale, 0.001f)
    }

    @Test
    fun `active line words animate toward the sweep`() {
        val a = animator()
        // Warm up a few frames mid-word.
        var states = a.animate(lines, 400L, frameDt)
        repeat(10) { states = a.animate(lines, 400L + it * 16L, frameDt) }
        val word = states[0].wordStates[0]
        // Gradient tracks progress directly (-20 + 120·pct at ~0.55 ≈ 46).
        assertTrue("gradient should be sweeping, was ${word.gradientPosition}", word.gradientPosition > 0f)
        // Scale should have left its resting value.
        assertTrue("scale should have risen, was ${word.scale}", word.scale > 0.951f)
    }

    @Test
    fun `sung line keeps settling while the next line is not sung, then freezes`() {
        val a = animator()
        // Play through line 1.
        var t = 0L
        while (t < 1100L) { a.animate(lines, t, frameDt); t += 16L }

        // Line 1 is Sung, line 2 NotSung -> finalize pass keeps stepping (fresh instances).
        val s1 = a.animate(lines, 1200L, frameDt)
        val s2 = a.animate(lines, 1216L, frameDt)
        assertNotEquals(
            "sung line should still be settling while next line isn't sung",
            s1[0].wordStates[0].scale, s2[0].wordStates[0].scale
        )
        assertEquals(100f, s2[0].wordStates[0].gradientPosition, 0.001f)

        // Advance until BOTH lines are sung: line 1's successor is Sung -> line 1 freezes.
        while (t < 3100L) { a.animate(lines, t, frameDt); t += 16L }
        val f1 = a.animate(lines, 3200L, frameDt)
        val f2 = a.animate(lines, 3216L, frameDt)
        assertSame("fully passed line should be frozen", f1[0].wordStates, f2[0].wordStates)
        // The LAST line always finalizes (no next line), so it keeps settling.
        assertEquals(100f, f2[1].wordStates[0].gradientPosition, 0.001f)
    }

    @Test
    fun `blur is distance-based and zero on the active line`() {
        val a = animator()
        val states = a.animate(lines, 500L, frameDt)
        assertEquals(0f, states[0].blur, 0.001f)
        assertEquals(1.25f, states[1].blur, 0.001f)  // distance 1 × BlurMultiplier
    }
}
