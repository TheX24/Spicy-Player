package com.tx24.spicyplayer.lyrics.spicy

/**
 * The three rendering-quality tracks from the original Spicy Lyrics extension.
 *
 * - [FULL]: springs + glow + distance blur + per-letter emphasis (default, highest fidelity).
 * - [SIMPLE]: word scale/glow disabled (gradient wipe only), reduced letter effects.
 * - [MINIMAL]: sung lines fade fully out; no letters/glow/blur; longer transitions.
 */
enum class LyricsQualityMode { FULL, SIMPLE, MINIMAL }

/**
 * Mode-varying tunables for the lyrics engine. All spring frequencies/damping ratios and
 * spline control points are reference constants living inside `LyricsAnimator` (an exact port
 * of the extension's `LyricsAnimator.ts`); this config only carries the values that genuinely
 * differ per quality mode (mirroring the reference's `$simpleLyricsMode` CSS/JS switches and
 * our Compose-level Minimal mode).
 */
data class RenderConfig(
    val mode: LyricsQualityMode,

    // Gradient text alphas (Mixed.css: --gradient-alpha / --gradient-alpha-end !important).
    // Fixed for every word/letter/line state — only --gradient-position moves between
    // NotSung(-20)/Active(animated)/Sung(100). Multiplied by line opacity at draw time.
    val gradientAlphaBright: Float,
    val gradientAlphaDim: Float,

    // Line opacity states (CSS --Vocal-*-opacity).
    val opacityActive: Float,
    val opacityNotSung: Float,
    val opacitySung: Float,

    /** Line opacity/scale transition duration (CSS transition: 0.2s; Minimal 0.4s). */
    val lineTransitionMs: Int,

    /** Distance-based blur halo on inactive lines (FULL only). */
    val distanceBlurEnabled: Boolean,

    // Per-letter "held word" emphasis capability (IsLetterCapable.ts).
    val lettersEnabled: Boolean,
    val letterDurationThresholdMs: Long,   // 1000 full, 1050 simple
    val letterMaxLength: Int,              // unlimited full, 12 simple

    /** Gap threshold for synthesizing interlude dot lines (3s; 5s Minimal). */
    val interludeGapThresholdMs: Long,
) {
    val isSimple: Boolean get() = mode == LyricsQualityMode.SIMPLE

    companion object {
        val FULL = RenderConfig(
            mode = LyricsQualityMode.FULL,
            gradientAlphaBright = 0.85f,
            gradientAlphaDim = 0.35f,
            opacityActive = 1.0f,
            opacityNotSung = 0.51f,
            opacitySung = 0.497f,
            lineTransitionMs = 200,
            distanceBlurEnabled = true,
            lettersEnabled = true,
            letterDurationThresholdMs = 1000L,
            letterMaxLength = Int.MAX_VALUE,
            interludeGapThresholdMs = 3000L,
        )

        val SIMPLE = FULL.copy(
            mode = LyricsQualityMode.SIMPLE,
            gradientAlphaBright = 1.0f,
            gradientAlphaDim = 0.3f,
            opacityNotSung = 0.45f,
            opacitySung = 0.35f,
            distanceBlurEnabled = false,
            letterDurationThresholdMs = 1050L,
            letterMaxLength = 12,
        )

        val MINIMAL = FULL.copy(
            mode = LyricsQualityMode.MINIMAL,
            opacityNotSung = 0.5f,
            opacitySung = 0.0f,            // sung lines fade out entirely
            lineTransitionMs = 400,
            distanceBlurEnabled = false,
            lettersEnabled = false,
            interludeGapThresholdMs = 5000L,
        )

        fun forMode(mode: LyricsQualityMode): RenderConfig = when (mode) {
            LyricsQualityMode.FULL -> FULL
            LyricsQualityMode.SIMPLE -> SIMPLE
            LyricsQualityMode.MINIMAL -> MINIMAL
        }

        /** Resolves a persisted settings string (case-insensitive) to a preset, defaulting to [FULL]. */
        fun forModeName(name: String?): RenderConfig = when (name?.uppercase()) {
            "SIMPLE" -> SIMPLE
            "MINIMAL" -> MINIMAL
            else -> FULL
        }
    }
}
