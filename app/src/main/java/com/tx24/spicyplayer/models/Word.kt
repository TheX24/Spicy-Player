package com.tx24.spicyplayer.models

/**
 * Represents a single word or syllable group in the lyrics.
 */
data class Word(
    /** The actual text content of the word. */
    val text: String,
    /** Start time in milliseconds. */
    val startMs: Long,
    /** End time in milliseconds. */
    val endMs: Long,
    /** Internal flag indicating if this is part of a larger word (UI specific). */
    val isPartOfWord: Boolean = false,
    /** If true, the word contains individual [Letter] timings for syllabic highlighting. */
    val isLetterGroup: Boolean = false,
    /** List of individual [Letter] objects for granular timing. */
    val letters: List<Letter> = emptyList(),
) {
    /** The duration of the word in milliseconds, guaranteed to be at least 1ms. */
    val duration: Long
        get() = (endMs - startMs).coerceAtLeast(1)
}
