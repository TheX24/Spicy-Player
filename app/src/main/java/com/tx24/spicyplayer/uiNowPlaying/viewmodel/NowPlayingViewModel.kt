package com.tx24.spicyplayer.uiNowPlaying.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx24.spicyplayer.playback.PlaybackManager
import com.tx24.spicyplayer.uiNowPlaying.NowPlayingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
) : ViewModel(), INowPlayingViewModel {


    private val _state: StateFlow<NowPlayingState> =
        playbackManager.state.combine(playbackManager.queue) {
            mediaPlayerState, queue ->
            val mediaPlayerSong = mediaPlayerState.currentPlayingSong
                ?: return@combine NowPlayingState.NotPlaying
            
            val songs = queue.items.map { it.song }
            // Clamp the index to the current queue size to handle intermediate states
            // where the queue has been updated but the player state index hasn't caught up yet.
            val safeIndex = mediaPlayerState.songIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
            
            NowPlayingState.Playing(
                queue = songs,
                songIndex = safeIndex,
                mediaPlayerState.playbackState.playerState,
                repeatMode = mediaPlayerState.playbackState.repeatMode,
                isShuffleOn = mediaPlayerState.playbackState.isShuffleOn
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, NowPlayingState.NotPlaying)


    val state: StateFlow<NowPlayingState>
        get() = _state

    override fun currentSongProgress() = playbackManager.currentSongProgress

    override fun togglePlayback() {
        playbackManager.togglePlayback()
    }

    override fun nextSong() {
        playbackManager.playNextSong()
    }

    override fun jumpForward() {
        playbackManager.forward()
    }

    override fun jumpBackward() {
        playbackManager.backward()
    }

    override fun playSongAtIndex(index: Int) {
        playbackManager.playSongAtIndex(index)
    }

    override fun onUserSeek(progress: Float) {
        playbackManager.seekToPosition(progress)
    }

    override fun previousSong() {
        playbackManager.playPreviousSong()
    }

    override fun toggleRepeatMode() {
        playbackManager.toggleRepeatMode()
    }

    override fun toggleShuffleMode() {
        playbackManager.toggleShuffleMode()
    }

}