package com.tx24.spicyplayer.animation

/**
 * Holds the spring simulations for a single word's animation properties.
 */
data class WordSprings(
    val scale: SpringSimulation = SpringSimulation(0.95f, LyricsAnimator.SCALE_FREQUENCY, LyricsAnimator.SCALE_DAMPING),
    val yOffset: SpringSimulation = SpringSimulation(0.01f, LyricsAnimator.Y_OFFSET_FREQUENCY, LyricsAnimator.Y_OFFSET_DAMPING),
    val glow: SpringSimulation = SpringSimulation(0f, LyricsAnimator.GLOW_FREQUENCY, LyricsAnimator.GLOW_DAMPING),
    val activeFactor: SpringSimulation = SpringSimulation(0f, 1.5f, 0.8f) // Controls alpha transition
)

/** Holds spring simulations for interlude dots. */
data class DotSprings(
    val scale:   SpringSimulation = SpringSimulation(0.75f, 0.7f,  0.6f),
    val yOffset: SpringSimulation = SpringSimulation(0f,    1.25f, 0.4f),
    val opacity: SpringSimulation = SpringSimulation(0.35f, 1.0f,  0.5f),
)

/** Holds spring simulations for individual letters in a held word. */
data class LetterSprings(
    val scale: SpringSimulation = SpringSimulation(0.95f, LyricsAnimator.SCALE_FREQUENCY, LyricsAnimator.SCALE_DAMPING),
    val yOffset: SpringSimulation = SpringSimulation(0.01f, LyricsAnimator.Y_OFFSET_FREQUENCY, LyricsAnimator.Y_OFFSET_DAMPING),
    val glow: SpringSimulation = SpringSimulation(0f, LyricsAnimator.GLOW_FREQUENCY, LyricsAnimator.GLOW_DAMPING)
)
