package com.tx24.spicyplayer.uiNowPlaying.spicy.models

/**
 * The top-level structure containing the entire set of [Line]s parsed from a file.
 */
data class ParsedLyrics(
    val lines: List<Line>,
    val songwriters: List<String> = emptyList()
)
