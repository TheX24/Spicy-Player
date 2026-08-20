package com.tx24.spicyplayer.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx24.spicyplayer.network.data.NetworkMonitor
import com.tx24.spicyplayer.network.model.NetworkStatus
import com.tx24.spicyplayer.playback.PlaybackManager
import com.tx24.spicyplayer.library.store.lyrics.LyricsRepository
import com.tx24.spicyplayer.library.store.lyrics.LyricsResult
import com.tx24.spicyplayer.library.store.model.song.Song
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsFooter
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsProvenance
import com.tx24.spicyplayer.lyrics.spicy.models.lyricsDocumentId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
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
                // collectLatest so a rapid skip cancels the previous song's in-flight load instead
                // of queuing behind it — otherwise a slow network lookup for a song you've already
                // skipped past blocks the next one, even when its lyrics are cached/pre-matched.
                .collectLatest {
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

        // A throw here would cancel the song-change collector in init and stop
        // lyrics from loading for every subsequent song, so fail into NoLyrics.
        val newState = try {
            val lyricsResult = lyricsRepository
                .getLyrics(
                    song.uri,
                    song.metadata.title,
                    song.metadata.albumName.orEmpty(),
                    song.metadata.artistName.orEmpty(),
                    song.metadata.durationMillis.toInt() / 1000
                )

            when (lyricsResult) {
                is LyricsResult.NotFound ->
                    LyricsScreenState.NoLyrics(NoLyricsReason.NOT_FOUND)

                is LyricsResult.NetworkError ->
                    LyricsScreenState.NoLyrics(NoLyricsReason.NETWORK_ERROR)

                is LyricsResult.FoundPlainLyrics -> {
                    val raw = lyricsResult.plainLyrics.constructStringForSharing()
                    val document = lyricsResult.plainLyrics.toSpicyStaticParsed().copy(
                        documentId = lyricsDocumentId(song.uri.toString(), lyricsResult.lyricsSource.name, raw),
                        footer = LyricsFooter(provenance = LyricsProvenance(lyricsResult.lyricsSource.name)),
                    )
                    LyricsScreenState.Ready(document)
                }

                is LyricsResult.FoundSyncedLyrics -> {
                    val raw = lyricsResult.syncedLyrics.originalString
                    val document = lyricsResult.syncedLyrics.toSpicyParsedLyrics().copy(
                        documentId = lyricsDocumentId(song.uri.toString(), lyricsResult.lyricsSource.name, raw),
                        footer = LyricsFooter(provenance = LyricsProvenance(lyricsResult.lyricsSource.name)),
                    )
                    LyricsScreenState.Ready(document)
                }

                is LyricsResult.FoundTtmlLyrics -> {
                    val parsed = com.tx24.spicyplayer.lyrics.spicy.parser.TtmlLyricsParser.parse(
                        lyricsResult.ttmlContent.byteInputStream()
                    )
                    LyricsScreenState.Ready(
                        parsed.copy(
                            documentId = lyricsDocumentId(
                                song.uri.toString(), lyricsResult.lyricsSource.name, lyricsResult.ttmlContent,
                            ),
                            footer = parsed.footer.copy(
                                provenance = LyricsProvenance(lyricsResult.lyricsSource.name),
                            ),
                        )
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to load lyrics for %s", song.metadata.title)
            LyricsScreenState.NoLyrics(NoLyricsReason.NOT_FOUND)
        }

        if (isActive)
            _state.value = newState
    }

    fun songProgressMillis(): Long {
        return playbackManager.currentSongProgressMillis
    }

    fun isPlaying(): Boolean = playbackManager.isCurrentlyPlaying

    fun playbackSpeed(): Float = playbackManager.playbackParameters.first

    fun songDurationMillis(): Long =
        playbackManager.state.value.currentPlayingSong?.metadata?.durationMillis ?: 0L

    fun setSongProgressMillis(millis: Long) {
        return playbackManager.seekToPositionMillis(millis)
    }

}
