package com.tx24.spicyplayer.models

/**
 * Represents a full line of lyrics composed of multiple [Word]s.
 */
data class Line(
    /** The list of words that make up this line. */
    val words: List<Word>,
    /** The official start time of the line in milliseconds. */
    val startMs: Long,
    /** Optional identifier for the singer (agent). */
    val agent: String? = null,
    /** If true, this is a background vocal line. */
    val isBackground: Boolean = false,
    /** If true, the line should be aligned to the opposite side (e.g., right-aligned for harmonies). */
    val oppositeAligned: Boolean = false,
    /** If true, this line contains songwriter information rather than lyrics. */
    val isSongwriter: Boolean = false,
    /** If true, this line represents an instrumental interlude. */
    val isInterlude: Boolean = false,
    /** Explicit end time for interludes, as they might not have words. */
    val interludeEndMs: Long = -1L,
) {
    /** 
     * The end time of the line. 
     * For interludes, it uses [interludeEndMs]. 
     * Otherwise, it uses the end time of the last word. 
     */
    val endMs: Long
        get() = if (isInterlude && interludeEndMs > 0) interludeEndMs else words.lastOrNull()?.endMs ?: startMs

    /** The duration of the entire line in milliseconds, guaranteed to be at least 1ms. */
    val duration: Long
        get() = (endMs - startMs).coerceAtLeast(1)
}
