package com.tx24.spicyplayer.animation

import kotlin.math.*

/**
 * Damped spring physics using the analytic (closed-form) solution.
 *
 * Unlike Euler or Verlet integration, this is mathematically exact for any
 * deltaTime — no energy drift, no frame-rate dependency.
 *
 * The solution follows the damped harmonic oscillator:
 *   x'' + 2*ζ*ω*x' + ω²*x = ω²*goal
 * where
 *   ω (angularFreq) = frequency * 2π
 *   ζ (zeta)        = damping
 *
 * Under-damped (ζ < 1):  oscillates, decays exponentially
 * Critically damped (ζ = 1): fastest non-oscillating
 * Over-damped (ζ > 1): slow return, no overshoot
 *
 * Port of the analytic spring solver pattern used in high-quality animation
 * runtimes (Framer Motion, React Spring, etc.).
 */
class SpringSimulation(
    initialValue: Float,
    private val frequency: Float,
    private val damping: Float
) {
    private var x: Float = initialValue   // position (relative to goal)
    private var v: Float = 0f             // velocity
    private var goal: Float = initialValue

    fun setGoal(target: Float, immediate: Boolean = false) {
        goal = target
        if (immediate) {
            x = target
            v = 0f
        }
    }

    fun resetTo(currentValue: Float) {
        x = currentValue
        v = 0f
    }

    fun step(deltaTime: Float): Float {
        val dt = deltaTime.coerceIn(0f, 0.064f)

        val ω  = frequency * 2f * PI.toFloat()  // angular frequency
        val ζ  = damping                          // damping ratio
        val x0 = x - goal                         // displacement from goal
        val v0 = v

        val result: Pair<Float, Float> = when {
            ζ < 1f -> {
                // Under-damped: oscillates with decaying amplitude
                val ωd = ω * sqrt(1f - ζ * ζ)   // damped angular frequency
                val A  = x0
                val B  = (v0 + ζ * ω * x0) / ωd
                val e  = exp(-ζ * ω * dt)
                val cosT = cos(ωd * dt)
                val sinT = sin(ωd * dt)

                val newX = e * (A * cosT + B * sinT) + goal
                val newV = e * ((v0 + ζ * ω * x0) * cosT
                        - (x0 * ωd + ζ * ω * (v0 + ζ * ω * x0) / ωd) * sinT)
                    // Simplified derivative of the above:
                    // dX/dt = e*(-ζω*(A cos + B sin) + ωd*(-A sin + B cos))
                val dNewV = e * ((-ζ * ω * (A * cosT + B * sinT) + ωd * (-A * sinT + B * cosT)))
                Pair(newX, dNewV)
            }
            ζ == 1f -> {
                // Critically damped: fastest non-oscillating return
                val e = exp(-ω * dt)
                val newX = (x0 * (1f + ω * dt) + v0 * dt) * e + goal
                val newV = (v0 * (1f - ω * dt) - x0 * ω * ω * dt) * e
                Pair(newX, newV)
            }
            else -> {
                // Over-damped: slow non-oscillating return
                val ωd = ω * sqrt(ζ * ζ - 1f)
                val γ1 = -ζ * ω + ωd
                val γ2 = -ζ * ω - ωd
                val A  = (v0 - γ2 * x0) / (2f * ωd)
                val B  = x0 - A
                val newX = A * exp(γ1 * dt) + B * exp(γ2 * dt) + goal
                val newV = A * γ1 * exp(γ1 * dt) + B * γ2 * exp(γ2 * dt)
                Pair(newX, newV)
            }
        }

        x = result.first
        v = result.second

        // Settle if close enough
        if (abs(x - goal) < 0.00005f && abs(v) < 0.0005f) {
            x = goal
            v = 0f
        }

        return x
    }

    val current: Float get() = x
}
