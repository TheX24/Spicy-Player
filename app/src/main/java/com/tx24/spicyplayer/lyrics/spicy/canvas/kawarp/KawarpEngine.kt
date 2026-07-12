package com.tx24.spicyplayer.lyrics.spicy.canvas.kawarp

/**
 * Options mirror @kawarp/core 1.2.0 `KawarpOptions` defaults exactly.
 * spicy-lyrics overrides at the call site (see KawarpBackground).
 */
data class KawarpOptions(
    val warpIntensity: Float = 1.0f,
    val blurPasses: Int = 8,
    val animationSpeed: Float = 1.0f,
    val transitionDuration: Float = 1000f,
    val saturation: Float = 1.5f,
    val tintColor: FloatArray = floatArrayOf(0.157f, 0.157f, 0.235f),
    val tintIntensity: Float = 0.15f,
    val dithering: Float = 0.008f,
    val scale: Float = 1.0f,
)

/**
 * 1:1 port of the non-GL state in @kawarp/core's `Kawarp` class: per-frame
 * animation-speed smoothing, accumulated warp time, and crossfade transitions.
 * Smoothing is deliberately per-*frame* (not per-second), like the original.
 */
class KawarpEngine(options: KawarpOptions) {

    var currentAnimationSpeed: Float = options.animationSpeed
        private set
    var targetAnimationSpeed: Float = options.animationSpeed
        set(value) { field = value.coerceIn(0.1f, 5f) }

    var accumulatedTime: Float = 0f
        private set

    var transitionDuration: Float = options.transitionDuration
        set(value) { field = value.coerceIn(0f, 5000f) }

    var isTransitioning: Boolean = false
        private set
    private var transitionStartMs: Long = 0L

    /** Advance the warp clock by one frame; returns the new accumulated time. */
    fun tick(dtSeconds: Float): Float {
        currentAnimationSpeed += (targetAnimationSpeed - currentAnimationSpeed) * 0.05f
        accumulatedTime += dtSeconds * currentAnimationSpeed
        return accumulatedTime
    }

    fun startTransition(nowMs: Long) {
        isTransitioning = true
        transitionStartMs = nowMs
    }

    /** Crossfade factor current→next; 1 when idle or finished (matches render()). */
    fun blendFactor(nowMs: Long): Float {
        if (!isTransitioning) return 1f
        if (transitionDuration <= 0f) { isTransitioning = false; return 1f }
        val f = ((nowMs - transitionStartMs) / transitionDuration).coerceAtMost(1f)
        if (f >= 1f) isTransitioning = false
        return f
    }
}
