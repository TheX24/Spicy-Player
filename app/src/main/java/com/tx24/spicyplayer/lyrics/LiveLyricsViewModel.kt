package com.tx24.spicyplayer.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx24.spicyplayer.network.data.NetworkMonitor
import com.tx24.spicyplayer.network.model.NetworkStatus
import com.tx24.spicyplayer.playback.PlaybackManager
import com.tx24.spicyplayer.library.store.lyrics.LyricsRepository
import com.tx24.spicyplayer.library.store.lyrics.LyricsResult
import com.tx24.spicyplayer.library.store.model.song.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class LiveLyricsViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val lyricsRepository: LyricsRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {


    private val _state = MutableStateFlow<LyricsScreenState>(LyricsScreenState.Loading)
    val state: StateFlow<LyricsScreenState>
        get() = _state

    init {
        viewModelScope.launch {
            playbackManager.state.distinctUntilChanged { old, new -> old.currentPlayingSong == new.currentPlayingSong }
                .collect {
                    if (it.currentPlayingSong == null) {
                        _state.value = LyricsScreenState.NotPlaying
                    } else {
                        loadLyrics(it.currentPlayingSong!!)
                    }
                }
        }
        viewModelScope.launch {
            networkMonitor.state.collect {
                if (it == NetworkStatus.CONNECTED)
                    onRegainedNetworkConnection()
            }
        }
    }

    private fun onRegainedNetworkConnection() {
        val currentState = _state.value
        if (currentState is LyricsScreenState.NoLyrics && currentState.reason == NoLyricsReason.NETWORK_ERROR) {
            onRetry()
        }
    }

    fun onRetry() {
        val currentSong = (playbackManager.state.value.currentPlayingSong) ?: return
        viewModelScope.launch {
            loadLyrics(currentSong)
        }
    }

    private suspend fun loadLyrics(song: Song) = withContext(Dispatchers.Default) {
        _state.value = LyricsScreenState.SearchingLyrics

        val lyricsResult = lyricsRepository
            .getLyrics(
                song.uri,
                song.metadata.title,
                song.metadata.albumName.orEmpty(),
                song.metadata.artistName.orEmpty(),
                song.metadata.durationMillis.toInt() / 1000
            )

        val newState = when (lyricsResult) {
            is LyricsResult.NotFound ->
                LyricsScreenState.NoLyrics(NoLyricsReason.NOT_FOUND)

            is LyricsResult.NetworkError ->
                LyricsScreenState.NoLyrics(NoLyricsReason.NETWORK_ERROR)

            is LyricsResult.FoundPlainLyrics ->
                LyricsScreenState.TextLyrics(lyricsResult.plainLyrics, lyricsResult.lyricsSource)

            is LyricsResult.FoundSyncedLyrics ->
                LyricsScreenState.SyncedLyrics(lyricsResult.syncedLyrics, lyricsResult.lyricsSource)

            is LyricsResult.FoundTtmlLyrics -> {
                val parsed = com.tx24.spicyplayer.uiNowPlaying.spicy.parser.TtmlLyricsParser.parse(
                    lyricsResult.ttmlContent.byteInputStream()
                )
                LyricsScreenState.TtmlLyrics(parsed, lyricsResult.lyricsSource)
            }
        }

        if (isActive)
            _state.value = newState
    }

    fun songProgressMillis(): Long {
        return playbackManager.currentSongProgressMillis
    }

    fun setSongProgressMillis(millis: Long) {
        return playbackManager.seekToPositionMillis(millis)
    }

}