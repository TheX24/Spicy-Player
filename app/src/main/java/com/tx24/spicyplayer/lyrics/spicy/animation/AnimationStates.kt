package com.tx24.spicyplayer.lyrics.spicy.animation

/**
 * Represents the animation state of a single letter within a word.
 */
data class LetterAnimState(
    val gradientPosition: Float,
    val scale: Float,
    val yOffset: Float,
    val glow: Float,
)

/**
 * Represents the animation state of a word, including its scale, offset, and potential letter-level states.
 */
data class WordAnimState(
    val scale: Float,
    val yOffset: Float,
    val glow: Float,
    val gradientPosition: Float,
    val state: ElementState,
    /** For interlude dots only: the animated glow level (blur/opacity of the dot's halo). */
    val dotGlow: Float = 0f,
    val isLetterGroup: Boolean = false,
    val letterStates: List<LetterAnimState> = emptyList(),
)

/**
 * Represents the overall animation state of a line, including its words.
 */
data class LineAnimState(
    val opacity: Float,
    val blur: Float,
    val scale: Float,
    val isActive: Boolean,
    val wordStates: List<WordAnimState>,
    val isBackground: Boolean,
    val isSongwriter: Boolean,
    /** Line-mode only: the raw gradient position percent for the whole-line wipe (-20..100). */
    val lineGradientPercent: Float = -20f,
    /** Line-mode only: the whole-line glow spring value (shadow blur 4+8·glow, alpha glow·0.5). */
    val lineGlow: Float = 0f,
)

/**
 * Possible states for a lyric element (word, line, or dot).
 */
enum class ElementState { NotSung, Active, Sung }
