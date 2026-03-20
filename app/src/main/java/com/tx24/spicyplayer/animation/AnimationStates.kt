package com.tx24.spicyplayer.animation

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
    val activeAnimFactor: Float,
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
)

/**
 * Possible states for a lyric element (word, line, or dot).
 */
enum class ElementState { NotSung, Active, Sung }
