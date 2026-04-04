package com.tx24.spicyplayer.uiNowPlaying

import androidx.compose.runtime.Immutable
import com.tx24.spicyplayer.model.playback.PlayerState
import com.tx24.spicyplayer.model.playback.RepeatMode
import com.tx24.spicyplayer.library.store.model.queue.QueueItem
import com.tx24.spicyplayer.library.store.model.song.Song


@Immutable
sealed interface NowPlayingState {


    @Immutable
    data object NotPlaying : NowPlayingState

    @Immutable
    data class Playing(
        val queue: List<Song>,
        val songIndex: Int,
        val playbackState: PlayerState,
        val repeatMode: RepeatMode,
        val isShuffleOn: Boolean,
    ) : NowPlayingState

}

val NowPlayingState.Playing.song: Song
    get() = queue.getOrElse(songIndex) { queue.first() }