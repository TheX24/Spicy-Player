package com.tx24.spicyplayer.lyrics.spicy.models

/**
 * The top-level structure containing the entire set of [Line]s parsed from a file.
 */
data class ParsedLyrics(
    val lines: List<Line>,
    val songwriters: List<String> = emptyList(),
    /** Synchronization granularity; drives which render path is used. */
    val type: LyricsType = LyricsType.Syllable,
) {
    /** True if any word carries romanized text (from TTML/API metadata or on-device romanization). */
    val hasTransliteration: Boolean
        get() = lines.any { line -> line.words.any { it.romanizedText != null } }
}
