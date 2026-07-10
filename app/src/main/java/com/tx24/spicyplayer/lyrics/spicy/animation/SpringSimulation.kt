package com.tx24.spicyplayer.lyrics.spicy.animation

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Exact port of `spicy-lyrics/src/modules/Spring.ts`, itself a port of Fraktality's spr.lua
 * (MIT). Behavioural contract, kept deliberately identical to the reference:
 *
 * - [frequency] is stored raw in Hz and converted to rad/s (×2π) inside every [step].
 * - [step] takes **seconds**, has NO deltaTime clamping and NO settle/snap logic — position
 *   and velocity are both advanced with the closed-form solution and always returned as-is.
 * - [setGoal] only moves the goal; velocity is preserved (momentum carries across retargets).
 *   Passing `replacePosition = true` snaps position to the goal and zeroes velocity — the
 *   reference uses that only when a spring is first created.
 */
class SpringSimulation(
    startPosition: Float,
    private var frequency: Float,
    private var damping: Float,
) {
    private var g: Float = startPosition   // goal
    private var p: Float = startPosition   // position
    private var v: Float = 0f              // velocity

    private companion object {
        const val EPS = 1e-5f
    }

    fun setGoal(target: Float, replacePosition: Boolean = false) {
        g = target
        if (replacePosition) {
            p = target
            v = 0f
        }
    }

    /** Snaps the spring's position (velocity zeroed) without changing the goal. */
    fun resetTo(currentValue: Float) {
        p = currentValue
        v = 0f
    }

    fun step(dt: Float): Float {
        val d = damping
        val f = frequency * (2f * PI.toFloat())  // Hz -> rad/s
        val g = this.g
        var p = this.p
        var v = this.v

        if (d == 1f) { // critically damped
            val q = exp(-f * dt)
            val w = dt * q

            val c0 = q + w * f
            val c2 = q - w * f
            val c3 = w * f * f

            val o = p - g
            p = o * c0 + v * w + g
            v = v * c2 - o * c3
        } else if (d < 1f) { // underdamped
            val q = exp(-d * f * dt)
            val c = sqrt(1f - d * d)

            val i = cos(dt * f * c)
            val j = sin(dt * f * c)

            // Sinc-like terms with Taylor fallbacks near zero (reference-exact).
            val z: Float = if (c > EPS) {
                j / c
            } else {
                val a = dt * f
                a + ((a * a) * (c * c) * (c * c) / 20f - c * c) * (a * a * a) / 6f
            }

            val y: Float = if (f * c > EPS) {
                j / (f * c)
            } else {
                val b = f * c
                dt + ((dt * dt) * (b * b) * (b * b) / 20f - b * b) * (dt * dt * dt) / 6f
            }

            val o = p - g
            p = (o * (i + z * d) + v * y) * q + g
            v = (v * (i - z * d) - o * (z * f)) * q
        } else { // overdamped
            val c = sqrt(d * d - 1f)

            val r1 = -f * (d + c)
            val r2 = -f * (d - c)

            val ec1 = exp(r1 * dt)
            val ec2 = exp(r2 * dt)

            val o = p - g
            val co2 = (v - o * r1) / (2f * f * c)
            val co1 = ec1 * (o - co2)

            p = co1 + co2 * ec2 + g
            v = co1 * r1 + co2 * ec2 * r2
        }

        this.p = p
        this.v = v
        return p
    }

    val current: Float get() = p
}
