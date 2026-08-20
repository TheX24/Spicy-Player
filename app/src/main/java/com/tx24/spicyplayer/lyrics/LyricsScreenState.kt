package com.tx24.spicyplayer.lyrics

import com.tx24.spicyplayer.lyrics.spicy.models.LyricsDocument


sealed interface LyricsScreenState {

    data object Loading: LyricsScreenState

    data object NotPlaying: LyricsScreenState

    data object SearchingLyrics: LyricsScreenState

    data class Ready(val document: LyricsDocument): LyricsScreenState

    data class NoLyrics(val reason: NoLyricsReason): LyricsScreenState
}

enum class NoLyricsReason {
    NETWORK_ERROR, NOT_FOUND
}
