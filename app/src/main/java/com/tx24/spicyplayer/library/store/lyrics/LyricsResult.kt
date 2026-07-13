package com.tx24.spicyplayer.library.store.lyrics

import com.tx24.spicyplayer.model.lyrics.LyricsFetchSource
import com.tx24.spicyplayer.model.lyrics.PlainLyrics
import com.tx24.spicyplayer.model.lyrics.SynchronizedLyrics


sealed interface LyricsResult {

    data object NotFound: LyricsResult

    data object NetworkError: LyricsResult

    data class FoundPlainLyrics(
        val plainLyrics: PlainLyrics,
        val lyricsSource: LyricsFetchSource
    ): LyricsResult

    data class FoundSyncedLyrics(
        val syncedLyrics: SynchronizedLyrics,
        val lyricsSource: LyricsFetchSource
    ): LyricsResult

    data class FoundTtmlLyrics(
        val ttmlContent: String,
        val lyricsSource: LyricsFetchSource
    ): LyricsResult
}
