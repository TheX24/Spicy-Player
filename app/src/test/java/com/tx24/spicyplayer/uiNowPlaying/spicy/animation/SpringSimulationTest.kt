package com.tx24.spicyplayer.uiNowPlaying.spicy.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertEquals(GOAL, spring.current, 0.001f)
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
    fun `spring settles to goal exactly once close enough`() {
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL)
        spring.run(seconds = 10f)
        // The settle threshold snaps position to the goal bit-exactly.
        assertEquals(GOAL, spring.current, 0f)
    }

    @Test
    fun `huge delta time is clamped and stays finite`() {
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL)
        val x = spring.step(100f)
        assertTrue(x.isFinite())
        assertTrue(x > 0f && x < GOAL)
    }

    @Test
    fun `negative delta time does not move the spring`() {
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL)
        assertEquals(0f, spring.step(-1f), 0f)
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
    fun `immediate goal snaps position and velocity`() {
        val spring = SpringSimulation(0f, frequency = 1.5f, damping = 1f)
        spring.setGoal(GOAL, immediate = true)
        assertEquals(GOAL, spring.current, 0f)
        // No residual velocity: further steps stay at the goal.
        assertEquals(GOAL, spring.step(FRAME_DT), 0f)
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
    fun `zero frequency snaps to goal without producing NaN`() {
        val spring = SpringSimulation(0f, frequency = 0f, damping = 0.4f)
        spring.setGoal(GOAL)
        val x = spring.step(FRAME_DT)
        assertTrue(x.isFinite())
        assertEquals(GOAL, x, 0f)
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
