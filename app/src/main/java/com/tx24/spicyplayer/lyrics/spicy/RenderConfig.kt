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

    // Gradient text alphas (Mixed.css: --gradient-alpha / --gradient-alpha-end). Fixed per
    // element — only --gradient-position moves between NotSung(-20)/Active(animated)/Sung(100),
    // multiplied by line opacity at draw time. CSS cascade trap: the `.line`-level
    // `--gradient-alpha-end: 0.35 !important` never reaches words/letters, because they carry
    // their own DIRECT 0.5 declaration (a direct custom property beats an inherited !important).
    // So words/letters sweep bright→0.5 while Line-mode text (living on the .line element
    // itself) sweeps bright→0.35.
    val gradientAlphaBright: Float,
    val gradientAlphaDim: Float,
    val lineGradientAlphaDim: Float,

    // Line opacity states (CSS --Vocal-*-opacity).
    val opacityActive: Float,
    val opacityNotSung: Float,
    val opacitySung: Float,

    /**
     * Line opacity/scale transition duration. Reference CSS is 0.2s/0.4s(Minimal), but that value
     * is tuned for a GPU-composited CSS transition; run through our cubic-bezier(0.61,1,0.88,1)
     * easing (front-loaded — most of the motion happens in the first half) on a per-frame Compose
     * canvas, 200ms reads as a near-instant snap rather than a visible fade. Widened so the
     * active→inactive dim is actually perceptible.
     */
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
            gradientAlphaDim = 0.5f,
            lineGradientAlphaDim = 0.35f,
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
            lineGradientAlphaDim = 0.3f,
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
