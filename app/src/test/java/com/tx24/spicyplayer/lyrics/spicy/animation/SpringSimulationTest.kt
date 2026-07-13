package com.tx24.spicyplayer.lyrics.spicy.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

class SpringSimulationTest {

    private companion object {
        const val FRAME_DT = 1f / 60f
        const val GOAL = 100f
    }

    private fun SpringSimulation.run(seconds: Float, dt: Float = FRAME_DT): Float {
        var value = current
        repeat((seconds / dt).toInt()) { value = step(dt) }
        return value
    }

    // ── Reference parity: exact spr.lua math ────────────────────────────────────────

    /** Independent double-precision implementation of the reference's underdamped Step. */
    private fun referenceUnderdampedStep(
        p0: Double, v0: Double, goal: Double, freqHz: Double, damping: Double, dt: Double,
    ): Pair<Double, Double> {
        val f = freqHz * 2.0 * PI
        val d = damping
        val q = exp(-d * f * dt)
        val c = sqrt(1.0 - d * d)
        val i = cos(dt * f * c)
        val j = sin(dt * f * c)
        val z = j / c
        val y = j / (f * c)
        val o = p0 - goal
        val p = (o * (i + z * d) + v0 * y) * q + goal
        val v = (v0 * (i - z * d) - o * (z * f)) * q
        return p to v
    }

    @Test
    fun `underdamped step matches the reference formula exactly`() {
        // Word scale spring tuning: f=0.88Hz, d=0.64, chasing a fixed goal.
        val spring = SpringSimulation(0.95f, frequency = 0.88f, damping = 0.64f)
        spring.setGoal(1.0505f)

        var p = 0.95
        var v = 0.0
        repeat(30) {
            val actual = spring.step(FRAME_DT)
            val (ep, ev) = referenceUnderdampedStep(p, v, 1.0505, 0.88, 0.64, FRAME_DT.toDouble())
            p = ep; v = ev
            assertEquals("frame $it", p.toFloat(), actual, 1e-4f)
        }
    }

    @Test
    fun `velocity is preserved across goal changes`() {
        // The reference SetGoal does NOT reset velocity — momentum carries across retargets.
        val spring = SpringSimulation(0f, frequency = 0.88f, damping = 0.64f)
        spring.setGoal(GOAL)
        spring.run(seconds = 0.3f)
        val before = spring.current
        spring.setGoal(0f)
        // Immediately after retargeting, the spring should keep moving in its old direction
        // for at least one frame (velocity was upward).
        val after = spring.step(FRAME_DT)
        assertTrue("expected momentum to carry (was $before, now $after)", after > before)
    }

    @Test
    fun `there is no settle snap`() {
        // The reference never snaps to the goal; it converges asymptotically.
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL)
        spring.run(seconds = 20f)
        assertEquals(GOAL, spring.current, 0.01f)
    }

    // ── General behaviour ───────────────────────────────────────────────────────────

    @Test
    fun `critically damped spring converges to goal`() {
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL)
        spring.run(seconds = 10f)
        assertEquals(GOAL, spring.current, 0.001f)
    }

    @Test
    fun `critically damped spring never overshoots`() {
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL)
        repeat(600) {
            spring.step(FRAME_DT)
            assertTrue(
                "position ${spring.current} overshot goal $GOAL",
                spring.current <= GOAL + 0.001f
            )
        }
    }

    @Test
    fun `under damped spring overshoots then converges`() {
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 0.4f)
        spring.setGoal(GOAL)
        var peak = 0f
        repeat(600) {
            peak = maxOf(peak, spring.step(FRAME_DT))
        }
        assertTrue("under-damped spring should overshoot, peak was $peak", peak > GOAL + 1f)
        assertEquals(GOAL, spring.current, 0.01f)
    }

    @Test
    fun `over damped spring converges without overshoot`() {
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1.5f)
        spring.setGoal(GOAL)
        repeat(1200) {
            spring.step(FRAME_DT)
            assertTrue(spring.current <= GOAL + 0.001f)
        }
        assertEquals(GOAL, spring.current, 0.01f)
    }

    @Test
    fun `huge delta time stays finite and near the goal`() {
        // No dt clamp (reference): a long stall integrates analytically toward the goal.
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL)
        val x = spring.step(100f)
        assertTrue(x.isFinite())
        assertEquals(GOAL, x, 0.01f)
    }

    @Test
    fun `stepping is frame rate independent`() {
        // The analytic solution forms a semigroup: two half-steps equal one full step.
        val coarse = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        val fine = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        coarse.setGoal(GOAL)
        fine.setGoal(GOAL)
        repeat(60) { coarse.step(1f / 30f) }
        repeat(120) { fine.step(1f / 60f) }
        assertEquals(coarse.current, fine.current, 0.05f)
    }

    @Test
    fun `under damped stepping is frame rate independent`() {
        val coarse = SpringSimulation(0f, frequency = 1.5f, damping = 0.4f)
        val fine = SpringSimulation(0f, frequency = 1.5f, damping = 0.4f)
        coarse.setGoal(GOAL)
        fine.setGoal(GOAL)
        repeat(30) { coarse.step(1f / 30f) }
        repeat(60) { fine.step(1f / 60f) }
        assertEquals(coarse.current, fine.current, 0.05f)
    }

    @Test
    fun `replacePosition snaps position and velocity`() {
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL, replacePosition = true)
        assertEquals(GOAL, spring.current, 0f)
        // No residual velocity: further steps stay at the goal.
        assertEquals(GOAL, spring.step(FRAME_DT), 1e-4f)
    }

    @Test
    fun `resetTo rebases position without residual velocity`() {
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL)
        spring.run(seconds = 1f)
        spring.resetTo(50f)
        assertEquals(50f, spring.current, 0f)
        // Still converges to the goal from the new position.
        spring.run(seconds = 10f)
        assertEquals(GOAL, spring.current, 0.001f)
    }

    @Test
    fun `spring converges downward from above the goal`() {
        val spring = SpringSimulation(200f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL)
        repeat(600) {
            spring.step(FRAME_DT)
            assertFalse(spring.current < GOAL - 0.001f)
        }
        assertEquals(GOAL, spring.current, 0.001f)
    }
}
