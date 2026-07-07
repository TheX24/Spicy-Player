package com.tx24.spicyplayer.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.tx24.spicyplayer.model.playback.PlaybackState
import com.tx24.spicyplayer.model.playback.PlayerState
import com.tx24.spicyplayer.playback.extensions.EXTRA_SONG_ORIGINAL_INDEX
import com.tx24.spicyplayer.playback.state.MediaPlayerState
import com.tx24.spicyplayer.model.prefs.PlayerSettings
import com.tx24.spicyplayer.library.store.MediaRepository
import com.tx24.spicyplayer.library.store.PlaylistsRepository
import com.tx24.spicyplayer.library.store.preferences.UserPreferencesRepository
import com.tx24.spicyplayer.library.store.model.queue.Queue
import com.tx24.spicyplayer.library.store.model.queue.QueueItem
import com.tx24.spicyplayer.library.store.model.song.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


/**
 * This singleton class represents the interface between the application and the media playback service running in the background.
 * It exposes the current state of the MediaSessionService as state flows so UI can update accordingly
 * It provides methods to manipulate the service, like changing the queue, pausing, rewinding, etc...
 */
@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext context: Context,
    private val mediaRepository: MediaRepository,
    private val playlistsRepository: PlaylistsRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : PlaylistPlaybackActions {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /** Null until the async connection to [PlaybackService] completes. */
    private var mediaController: MediaController? = null

    /** If the [MediaController] is connected to the media service */
    private val isReady = MutableStateFlow(false)

    /**
     * Runs [action] on the connected controller. Commands issued in the window
     * before the async service connection completes are dropped instead of
     * crashing with an uninitialized controller.
     */
    private inline fun withController(action: MediaController.() -> Unit) {
        val controller = mediaController
        if (controller == null) {
            Timber.w("MediaController not connected yet; command dropped")
            return
        }
        controller.action()
    }

    /** Cached player settings — always readable synchronously for use in playPreviousSong, etc. */
    private val playerSettings: StateFlow<PlayerSettings> =
        userPreferencesRepository.playerSettingsFlow
            .stateIn(coroutineScope, SharingStarted.Eagerly, PlayerSettings(pauseOnVolumeZero = false, resumeWhenVolumeIncreases = false))

    init {
        initMediaController(context)
    }

    private val _state = MutableStateFlow(MediaPlayerState.empty)

    val state: StateFlow<MediaPlayerState>
        get() = _state

    val queue = MutableStateFlow(Queue.EMPTY)

    val currentSongProgress: Float
        get() {
            val controller = mediaController ?: return 0f
            // duration is 0 before prepare and C.TIME_UNSET (negative) while unknown
            val duration = controller.duration
            if (duration <= 0L) return 0f
            return (controller.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }

    val currentSongProgressMillis
        get() = mediaController?.currentPosition ?: 0L

    val playbackParameters: Pair<Float, Float>
        get() {
            val p = mediaController?.playbackParameters ?: return 1f to 1f
            return p.speed to p.pitch
        }

    private val playbackState: PlayerState
        get() {
            return if (mediaController?.playWhenReady == true) PlayerState.PLAYING else PlayerState.PAUSED
        }


    fun clearQueue() = withController {
        clearMediaItems()
    }

    /**
     * Toggle the player state
     */
    fun togglePlayback() = withController {
        prepare()
        playWhenReady = !playWhenReady
    }

    /**
     * Skip forward in currently playing song
     */
    fun forward() = withController {
        sendCustomCommand(
            SessionCommand(Commands.JUMP_FORWARD, bundleOf()),
            bundleOf()
        )
    }

    /**
     * Skip backward in currently playing song
     */
    fun backward() = withController {
        sendCustomCommand(
            SessionCommand(Commands.JUMP_BACKWARD, bundleOf()),
            bundleOf()
        )
    }

    /**
     * Jumps to the next song in the queue
     */
    fun playNextSong() = withController {
        seekToNext()
    }

    /**
     * Jumps to the previous song in the queue, or seeks to the beginning
     * if the current position exceeds [PlayerSettings.previousSkipThreshold].
     */
    fun playPreviousSong() = withController {
        val thresholdMs = playerSettings.value.previousSkipThreshold * 1000L
        if (currentPosition > thresholdMs) {
            seekTo(0)
        } else {
            // Use seekToPreviousMediaItem() instead of seekToPrevious() to bypass
            // Media3's own internal maxSeekToPreviousPositionMs threshold (default 3s),
            // which would otherwise override our custom threshold setting.
            seekToPreviousMediaItem()
        }
    }

    fun playSongAtIndex(index: Int) = withController {
        seekTo(index, 0)
    }

    fun removeSongAtIndex(index: Int) = withController {
        removeMediaItem(index)
    }

    fun reorderSong(from: Int, to: Int) = withController {
        moveMediaItem(from, to)
    }

    fun seekToPosition(progress: Float) = withController {
        val songDuration = duration
        if (songDuration <= 0L) return@withController
        seekTo((songDuration * progress).toLong())
    }

    fun seekToPositionMillis(millis: Long) = withController {
        seekTo(millis)
    }

    /**
     * Changes the current playlist of the player and starts playing the song at the specified index
     */
    fun setPlaylistAndPlayAtIndex(playlist: List<Song>, index: Int = 0) = withController {
        if (playlist.isEmpty()) return@withController
        val mediaItems = playlist.toMediaItems(0)
        stop() // release everything
        setMediaItems(mediaItems, index, 0)
        prepare()
        play()
    }

    /** Randomize the order of the list of songs and play */
    fun shuffle(songs: List<Song>) = withController {
        if (songs.isEmpty()) return@withController
        val shuffled = songs.shuffled()
        stop()
        setMediaItems(shuffled.toMediaItems(0), 0, 0)
        prepare()
        play()
    }

    fun shuffleNext(songs: List<Song>) = withController {
        val shuffled = songs.shuffled()
        addMediaItems(currentMediaItemIndex + 1, shuffled.toMediaItems(getMaximumOriginalId() + 1))
    }

    fun playNext(songs: List<Song>) = withController {
        if (songs.isEmpty()) return@withController
        addMediaItems(currentMediaItemIndex + 1, songs.toMediaItems(getMaximumOriginalId() + 1))
        prepare()
    }

    fun addToQueue(songs: List<Song>) = withController {
        addMediaItems(songs.toMediaItems(getMaximumOriginalId() + 1))
        prepare()
    }

    override fun playPlaylist(playlistId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            val songs = playlistsRepository.getPlaylistSongs(playlistId)
            withContext(Dispatchers.Main) {
                setPlaylistAndPlayAtIndex(songs)
            }
        }
    }

    override fun addPlaylistToNext(playlistId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            val songs = playlistsRepository.getPlaylistSongs(playlistId)
            withContext(Dispatchers.Main) {
                playNext(songs)
            }
        }
    }

    override fun addPlaylistToQueue(playlistId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            val songs = playlistsRepository.getPlaylistSongs(playlistId)
            withContext(Dispatchers.Main) {
                addToQueue(songs)
            }
        }
    }

    override fun shufflePlaylist(playlistId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            val songs = playlistsRepository.getPlaylistSongs(playlistId)
            if (songs.isEmpty()) return@launch
            withContext(Dispatchers.Main) {
                shuffle(songs)
            }
        }
    }

    override fun shufflePlaylistNext(playlistId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            val songs = playlistsRepository.getPlaylistSongs(playlistId)
            withContext(Dispatchers.Main) {
                shuffleNext(songs)
            }
        }
    }

    private fun MediaController.getMaximumOriginalId(): Int {
        val count = mediaItemCount
        if (count == 0) return 0
        return (0 until count).maxOf {
            val mediaItem = getMediaItemAt(it)
            // Externally supplied media items (e.g. Android Auto) may carry no extras
            mediaItem.requestMetadata.extras?.getInt(EXTRA_SONG_ORIGINAL_INDEX) ?: 0
        }
    }

    fun getCurrentSongIndex() = mediaController?.currentMediaItemIndex ?: 0

    fun setSleepTimer(minutes: Int, finishLastSong: Boolean) = withController {
        sendCustomCommand(
            SessionCommand(Commands.SET_SLEEP_TIMER, bundleOf()),
            bundleOf(
                "MINUTES" to minutes,
                "FINISH_LAST_SONG" to finishLastSong
            )
        )
    }

    fun setPlaybackParameters(speed: Float, pitch: Float) = withController {
        playbackParameters = PlaybackParameters(speed, pitch)
    }

    fun deleteSleepTimer() = withController {
        sendCustomCommand(
            SessionCommand(Commands.CANCEL_SLEEP_TIMER, Bundle.EMPTY), Bundle.EMPTY
        )
    }

    fun toggleRepeatMode() = withController {
        repeatMode = getRepeatModeFromPlayer(repeatMode).next().toPlayer()
    }

    fun toggleShuffleMode() = withController {
        shuffleModeEnabled = !shuffleModeEnabled
    }

    private fun updateState() {
        val mediaController = mediaController ?: return updateToEmptyState()
        updateQueue()
        val currentMediaItem = mediaController.currentMediaItem ?: return updateToEmptyState()
        val songUri = currentMediaItem.requestMetadata.mediaUri ?: return updateToEmptyState()
        val song = mediaRepository.songsFlow.value.getSongByUri(songUri.toString())
            ?: return updateToEmptyState()
        val playbackState = PlaybackState(
            playbackState,
            mediaController.shuffleModeEnabled,
            getRepeatModeFromPlayer(mediaController.repeatMode)
        )
        val songIndex = mediaController.currentMediaItemIndex
        _state.value = MediaPlayerState(song, songIndex, playbackState)
    }

    private fun updateToEmptyState() {
        _state.value = MediaPlayerState.empty
    }

    private fun stopPlayback() = withController {
        stop()
    }

    private fun initMediaController(context: Context) {
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val mediaControllerFuture = MediaController.Builder(context, sessionToken)
            .setApplicationLooper(context.mainLooper)
            .buildAsync()
        mediaControllerFuture.addListener(
            {
                val controller = mediaControllerFuture.get()
                mediaController = controller
                isReady.value = true
                updateState()
                updateQueue()
                attachListeners(controller)
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun attachListeners(mediaController: MediaController) {
        mediaController.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                updateState()
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    updateQueue()
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updateState()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                updateState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Timber.d("Media transitioned to ${mediaItem?.requestMetadata?.mediaUri}")
                updateState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                updateState()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
            }

        })
    }

    private fun updateQueue() {
        val mediaController = mediaController ?: return
        val count = mediaController.mediaItemCount

        val songsLibrary = mediaRepository.songsFlow.value

        if (count <= 0) {
            val q = Queue(listOf())
            queue.value = q
            return
        }

        val queueItems = (0 until count).mapNotNull { i ->
            val mediaItem = mediaController.getMediaItemAt(i)
            val requestMetadata = mediaItem.requestMetadata
            val song = songsLibrary.getSongByUri(requestMetadata.mediaUri.toString())
                ?: return@mapNotNull null
            QueueItem(
                song,
                requestMetadata.extras?.getInt(EXTRA_SONG_ORIGINAL_INDEX, i) ?: i
            )
        }

        val q = Queue(queueItems)
        queue.value = q
    }

    private fun Song.toMediaItem(index: Int) =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setArtist(metadata.artistName)
                    .setAlbumTitle(metadata.albumName)
                    .setTitle(metadata.title)
                    .build()
            )
            .setRequestMetadata(
                RequestMetadata.Builder().setMediaUri(uri)
                    .setExtras(bundleOf(EXTRA_SONG_ORIGINAL_INDEX to index))
                    .build() // to be able to retrieve the URI easily
            )
            .build()

    private fun List<Song>.toMediaItems(startingIndex: Int) = mapIndexed { index, song ->
        song.toMediaItem(startingIndex + index)
    }

    suspend fun waitUntilReady() {
        isReady.filter { it == true }.first()
    }

}