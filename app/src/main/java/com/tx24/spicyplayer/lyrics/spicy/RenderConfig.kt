package com.tx24.spicyplayer.lyrics.spicy

enum class SimpleAnimationStyle { CALCULATE, ANIMATE }

data class RenderConfig(
    val simpleLyricsMode: Boolean,
    /** Already scoped to a fullscreen, non-compact surface by the caller. */
    val minimalLyricsMode: Boolean,
    val simpleAnimationStyle: SimpleAnimationStyle = SimpleAnimationStyle.CALCULATE,
    val gradientAlphaBright: Float = 0.85f,
    val gradientAlphaDim: Float = 0.5f,
    val lineGradientAlphaDim: Float = 0.35f,
    val opacityActive: Float = 1f,
    val opacityNotSung: Float = if (minimalLyricsMode) 0.5f else 0.51f,
    val opacitySung: Float = if (minimalLyricsMode) 0f else 0.497f,
    val lineTransitionMs: Int = if (minimalLyricsMode) 400 else 200,
    /** Simple deliberately retains the reference's distance blur. */
    val distanceBlurEnabled: Boolean = true,
    /** Minimal is a line-visibility layer and does not disable word/letter motion. */
    val lettersEnabled: Boolean = true,
    val letterDurationThresholdMs: Long = if (simpleLyricsMode) 1050L else 1000L,
    val letterMaxLength: Int = if (simpleLyricsMode) 12 else Int.MAX_VALUE,
    val interludeGapThresholdMs: Long = if (minimalLyricsMode) 5000L else 3000L,
) {
    val isSimple: Boolean get() = simpleLyricsMode
    val isMinimal: Boolean get() = minimalLyricsMode

    companion object {
        val FULL = RenderConfig(false, false)
        val SIMPLE = RenderConfig(true, false)
        val MINIMAL = RenderConfig(false, true)

        fun create(
            simpleLyricsMode: Boolean,
            minimalLyricsMode: Boolean,
            simpleAnimationStyle: SimpleAnimationStyle = SimpleAnimationStyle.CALCULATE,
            fullscreen: Boolean = true,
            compact: Boolean = false,
        ) = RenderConfig(
            simpleLyricsMode = simpleLyricsMode,
            minimalLyricsMode = minimalLyricsMode && fullscreen && !compact,
            simpleAnimationStyle = simpleAnimationStyle,
        )
    }
}
