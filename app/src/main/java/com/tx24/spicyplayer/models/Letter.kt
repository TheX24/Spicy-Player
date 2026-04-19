package com.tx24.spicyplayer.models

/**
 * Represents a single character (letter) with its specific timing within a [Word].
 * Used for granular syllable-level highlighting.
 */
data class Letter(
    val char: String,
    val startMs: Long,
    val endMs: Long,
)
