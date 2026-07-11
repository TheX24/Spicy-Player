package com.tx24.spicyplayer.playback.state

import com.tx24.spicyplayer.model.playback.PlaybackState
import com.tx24.spicyplayer.library.store.model.song.Song

data class MediaPlayerState(
    val currentPlayingSong: Song?,
    val songIndex: Int,
    val playbackState: PlaybackState
) {

    companion object {
        val empty = MediaPlayerState(null, -1, PlaybackState.emptyState)
    }

}
