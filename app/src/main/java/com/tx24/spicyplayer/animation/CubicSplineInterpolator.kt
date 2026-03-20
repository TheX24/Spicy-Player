package com.tx24.spicyplayer.animation

/**
 * Natural cubic spline interpolation over (time, value) control points.
 * Port of the "cubic-spline" npm package used by spicy-lyrics.
 */
class CubicSplineInterpolator(points: List<AnimationPoint>) {
    private val xs: FloatArray
    private val ys: FloatArray
    private val ks: FloatArray // Coefficients for the spline segments

    init {
        require(points.size >= 2) { "Need at least 2 points for interpolation" }
        val sorted = points.sortedBy { it.time }
        xs = FloatArray(sorted.size) { sorted[it].time }
        ys = FloatArray(sorted.size) { sorted[it].value }
        ks = naturalSplineCoefficients(xs, ys)
    }

    /** Evaluate the spline at a given time t. Clamps to [first, last] range. */
    fun at(t: Float): Float {
        val clamped = t.coerceIn(xs.first(), xs.last())
        // Find the segment
        var i = 1
        while (i < xs.size - 1 && xs[i] < clamped) i++
        val segIdx = i - 1

        val dx = xs[segIdx + 1] - xs[segIdx]
        if (dx == 0f) return ys[segIdx]

        val localT = (clamped - xs[segIdx]) / dx
        val a = ks[segIdx] * dx - (ys[segIdx + 1] - ys[segIdx])
        val b = -ks[segIdx + 1] * dx + (ys[segIdx + 1] - ys[segIdx])

        return (1 - localT) * ys[segIdx] + localT * ys[segIdx + 1] +
                localT * (1 - localT) * (a * (1 - localT) + b * localT)
    }

    companion object {
        /**
         * Compute natural cubic spline coefficients using the tridiagonal algorithm.
         */
        private fun naturalSplineCoefficients(xs: FloatArray, ys: FloatArray): FloatArray {
            val n = xs.size
            val ks = FloatArray(n)

            if (n == 2) {
                // Simple linear interpolation
                val slope = (ys[1] - ys[0]) / (xs[1] - xs[0])
                ks[0] = slope
                ks[1] = slope
                return ks
            }

            // Build tridiagonal system
            val a = FloatArray(n)
            val b = FloatArray(n)
            val c = FloatArray(n)
            val d = FloatArray(n)

            // Natural spline: second derivative = 0 at endpoints
            a[0] = 0f
            b[0] = 2f / (xs[1] - xs[0])
            c[0] = 1f / (xs[1] - xs[0])
            d[0] = 3f * (ys[1] - ys[0]) / ((xs[1] - xs[0]) * (xs[1] - xs[0]))

            for (i in 1 until n - 1) {
                val dx0 = xs[i] - xs[i - 1]
                val dx1 = xs[i + 1] - xs[i]
                a[i] = 1f / dx0
                b[i] = 2f * (1f / dx0 + 1f / dx1)
                c[i] = 1f / dx1
                d[i] = 3f * ((ys[i] - ys[i - 1]) / (dx0 * dx0) + (ys[i + 1] - ys[i]) / (dx1 * dx1))
            }

            val last = n - 1
            val dxLast = xs[last] - xs[last - 1]
            a[last] = 1f / dxLast
            b[last] = 2f / dxLast
            c[last] = 0f
            d[last] = 3f * (ys[last] - ys[last - 1]) / (dxLast * dxLast)

            // Forward sweep
            for (i in 1 until n) {
                val m = a[i] / b[i - 1]
                b[i] -= m * c[i - 1]
                d[i] -= m * d[i - 1]
            }

            // Back substitution
            ks[last] = d[last] / b[last]
            for (i in n - 2 downTo 0) {
                ks[i] = (d[i] - c[i] * ks[i + 1]) / b[i]
            }

            return ks
        }
    }
}

data class AnimationPoint(val time: Float, val value: Float)
