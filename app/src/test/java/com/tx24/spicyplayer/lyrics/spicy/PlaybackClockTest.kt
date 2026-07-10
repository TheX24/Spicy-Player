package com.tx24.spicyplayer.lyrics.spicy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PlaybackClockTest {

    @Test
    fun `advances smoothly on wall clock while measured input is coarse`() {
        val clock = PlaybackClock()
        var now = 0L
        clock.positionMs(0L, isPlaying = true, nowMs = now)

        // Measured position only updates every 96ms (coarse), frames every 16ms.
        val outputs = mutableListOf<Long>()
        var measured = 0L
        repeat(60) { frame ->
            now += 16L
            if (frame % 6 == 5) measured += 96L
            outputs.add(clock.positionMs(measured, isPlaying = true, nowMs = now))
        }

        // Anchor extrapolation makes the output advance every frame at ~frame rate.
        val deltas = outputs.zipWithNext { a, b -> b - a }
        assertTrue("expected steady ~16ms deltas, got $deltas", deltas.all { it in 8..32 })
    }

    @Test
    fun `a stalled measured position does not cause a snap when it catches up`() {
        // Regression for "sweep starts then jumps to fully swept": a coarse position source
        // stalls for ~800ms, then delivers a large forward update. Without anchoring, the
        // predictor's error crosses the 500ms threshold and SNAPS; with anchoring, the clock
        // keeps advancing during the stall and the catch-up correction is small and smooth.
        val clock = PlaybackClock()
        var now = 0L
        clock.positionMs(0L, isPlaying = true, nowMs = now)

        val outputs = mutableListOf<Long>()
        repeat(50) { // 800ms of frames with a frozen measured position
            now += 16L
            outputs.add(clock.positionMs(0L, isPlaying = true, nowMs = now))
        }
        // Measured catches up all at once.
        now += 16L
        outputs.add(clock.positionMs(816L, isPlaying = true, nowMs = now))
        repeat(20) {
            now += 16L
            outputs.add(clock.positionMs(816L + (it + 1) * 16L, isPlaying = true, nowMs = now))
        }

        val deltas = outputs.zipWithNext { a, b -> b - a }
        assertTrue("no frame may jump (deltas: $deltas)", deltas.all { abs(it) <= 48L })
    }

    @Test
    fun `large error snaps immediately (seek)`() {
        val clock = PlaybackClock()
        clock.positionMs(1000L, isPlaying = true, nowMs = 0L)
        val out = clock.positionMs(60_000L, isPlaying = true, nowMs = 16L)
        assertEquals(60_000L + PlaybackClock.PROGRESS_POSITION_OFFSET_MS, out)
    }

    @Test
    fun `paused clock returns the measured position without lead`() {
        val clock = PlaybackClock()
        clock.positionMs(5000L, isPlaying = true, nowMs = 0L)
        val out = clock.positionMs(5000L, isPlaying = false, nowMs = 5000L)
        assertEquals(5000L, out)
        // And stays put across further paused frames.
        assertEquals(5000L, clock.positionMs(5000L, isPlaying = false, nowMs = 10_000L))
    }

    @Test
    fun `playing output includes the 100ms forward lead`() {
        val clock = PlaybackClock()
        val out = clock.positionMs(2000L, isPlaying = true, nowMs = 0L)
        assertEquals(2100L, out)
    }

    @Test
    fun `tracks a steadily advancing measured position with zero steady-state offset`() {
        val clock = PlaybackClock()
        var now = 0L
        clock.positionMs(0L, isPlaying = true, nowMs = now)
        var out = 0L
        repeat(300) {
            now += 16L
            out = clock.positionMs(now, isPlaying = true, nowMs = now)
        }
        // Predicted should sit on the measured clock (+lead), within a couple of ms.
        assertTrue(abs(out - (now + 100L)) <= 3L)
    }
}
