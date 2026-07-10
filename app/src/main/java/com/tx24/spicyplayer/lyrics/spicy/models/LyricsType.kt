package com.tx24.spicyplayer.lyrics.spicy.models

/**
 * The synchronization granularity of a parsed lyrics document, mirroring the
 * original Spicy Lyrics `lyrics.Type` dispatch (`Syllable` / `Line` / `Static`).
 *
 * - [Syllable]: word/syllable-timed karaoke (full per-word gradient wipe + springs).
 * - [Line]: line-timed; the whole line fills as one gradient sweep.
 * - [Static]: unsynced text; rendered as a plain, non-interactive list.
 */
enum class LyricsType {
    Syllable,
    Line,
    Static,
}
