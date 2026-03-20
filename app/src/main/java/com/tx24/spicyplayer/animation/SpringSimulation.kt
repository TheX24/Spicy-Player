package com.tx24.spicyplayer.animation

import kotlin.math.*

/**
 * Damped spring physics simulation.
 * Port of @spikerko/web-modules/Spring used in spicy-lyrics.
 *
 * Models a critically/under-damped spring that chases a goal value.
 * Call [setGoal] to set the target, then [step] each frame to advance.
 */
class SpringSimulation(
    initialValue: Float,
    private val frequency: Float,
    private val damping: Float
) {
    private var position: Float = initialValue
    private var velocity: Float = 0f
    private var goal: Float = initialValue

    /** Set a new target value for the spring to chase. */
    fun setGoal(target: Float, immediate: Boolean = false) {
        goal = target
        if (immediate) {
            position = target
            velocity = 0f
        }
    }

    /**
     * Advance the spring simulation by [deltaTime] seconds.
     * Returns the current position after stepping.
     */
    fun step(deltaTime: Float): Float {
        // Clamp deltaTime to avoid instability with large gaps
        val dt = deltaTime.coerceIn(0f, 0.1f)

        val angularFrequency = frequency * 2f * PI.toFloat()
        val displacement = position - goal

        // Damped harmonic oscillator
        val springForce = -angularFrequency * angularFrequency * displacement
        val dampingForce = -2f * damping * angularFrequency * velocity

        val acceleration = springForce + dampingForce

        velocity += acceleration * dt
        position += velocity * dt

        // Settle to goal if close enough
        if (abs(position - goal) < 0.0001f && abs(velocity) < 0.001f) {
            position = goal
            velocity = 0f
        }

        return position
    }

    /** Get current value without stepping */
    val current: Float get() = position
}
