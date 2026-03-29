package com.omar.musica.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.UnshuffledShuffleOrder
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.omar.musica.model.prefs.PlayerSettings
import com.omar.musica.playback.activity.ListeningAnalytics
import com.omar.musica.playback.extensions.toDBQueueItem
import com.omar.musica.playback.extensions.toMediaItem
import com.omar.musica.playback.timer.SleepTimerManager
import com.omar.musica.playback.timer.SleepTimerManagerListener
import com.omar.musica.playback.volume.AudioVolumeChangeListener
import com.omar.musica.playback.volume.VolumeChangeObserver
import com.omar.musica.store.QueueRepository
import com.omar.musica.store.preferences.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService :
    MediaSessionService(),
    SleepTimerManagerListener,
    AudioVolumeChangeListener,
    Player.Listener {


    /*------------------------------ Properties ------------------------------*/
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var queueRepository: QueueRepository

    @Inject
    lateinit var listeningAnalyticsFactory: ListeningAnalytics.Factory
    private lateinit var listeningAnalytics: ListeningAnalytics


    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var playerSettings: StateFlow<PlayerSettings>

    private lateinit var volumeObserver: VolumeChangeObserver

    private lateinit var sleepTimerManager: SleepTimerManager

    // We use this queue to restore back the original queue when
    // shuffle mode is enabled/disabled
    private var originalQueue: List<MediaItem> = listOf()

    /*------------------------------ Methods ------------------------------*/


    override fun onCreate() {
        super.onCreate()

        Timber.d("PlaybackService started")
        Log.d("PlaybackService", "PlaybackService started")

        player = buildPlayer()
        player.addListener(this@PlaybackService)

        createAnalyticsService()

        mediaSession = buildMediaSession()

        sleepTimerManager = SleepTimerManager(this)
        player.addListener(sleepTimerManager)


        playerSettings = userPreferencesRepository.playerSettingsFlow
            .stateIn(
                scope,
                started = SharingStarted.Eagerly,
                PlayerSettings(
                    pauseOnVolumeZero = false,
                    resumeWhenVolumeIncreases = false
                )
            )

        scope.launch(Dispatchers.Main) {
            playerSettings.collect { settings ->
                val handleAudio = settings.audioFocusBehavior != "IGNORE"
                val attributes = AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build()
                player.setAudioAttributes(attributes, handleAudio)
            }
        }

        volumeObserver = VolumeChangeObserver(
            applicationContext,
            Handler(Looper.myLooper() ?: Looper.getMainLooper()),
            AudioManager.STREAM_MUSIC
        ).apply { register(this@PlaybackService) }

        recoverQueue()
        scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(10_000)
                saveCurrentPosition()
            }
        }
    }


    private fun createAnalyticsService() {
        listeningAnalytics = listeningAnalyticsFactory.create(player)
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(this, Class.forName("com.omar.musica.MainActivity"))
        intent.action = VIEW_MEDIA_SCREEN_ACTION
        return PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private suspend fun saveCurrentPosition() {
        val uriString = player.currentMediaItem?.localConfiguration?.uri
        if (uriString != null) {
            val position = player.currentPosition
            withContext(Dispatchers.IO) {
                userPreferencesRepository.saveCurrentPosition(uriString.toString(), position)
            }
        }
    }

    private suspend fun restorePosition() = userPreferencesRepository.getSavedPosition()

    private fun buildCommandButtons(): List<CommandButton> {
        return emptyList()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        Timber.i(TAG, "Controller request: ${controllerInfo.packageName}")

        Log.d("PlaybackService", "Controller request: ${controllerInfo.packageName}")
        return mediaSession!!
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun buildMediaSession(): MediaSession {
        return MediaSession
            .Builder(applicationContext, player)
            .setCallback(buildCustomCallback())
            .setCustomLayout(buildCommandButtons())
            .setSessionActivity(buildPendingIntent())
            .build()
    }

    private fun buildPlayer(): ExoPlayer {
        return ExoPlayer.Builder(applicationContext)
            .setAudioAttributes(
                AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA).build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
            }
    }


    /**
     * Saves the currently playing queue in the database to retrieve it when starting
     * the application.
     */
    private fun saveQueue() {
        val mediaItems = List(player.mediaItemCount) { player.getMediaItemAt(it) }
        queueRepository.saveQueueFromDBQueueItems(mediaItems.map { it.toDBQueueItem() })
    }

    private fun recoverQueue() {
        scope.launch(Dispatchers.Main) {
            val queue = queueRepository.getQueue()

            val (lastSongUri, lastPosition) = restorePosition()
            val songIndex = queue.indexOfFirst { it.songUri.toString() == lastSongUri }

            player.setMediaItems(
                queue.mapIndexed { index, item -> item.toMediaItem(index) },
                if (songIndex in queue.indices) songIndex else 0,
                lastPosition
            )
            player.prepare()
        }
    }


    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        if (shuffleModeEnabled) {
            // user enabled shuffle, we have to store the current MediaItems

            val currentMediaItemIndex = player.currentMediaItemIndex
            val originalMediaItems = List(player.mediaItemCount) { i -> player.getMediaItemAt(i) }

            val shuffledQueue = originalMediaItems.toMutableList()
                .shuffled()
                .toMutableList()
                .apply {
                    // remove the current playing media item because we will move it
                    remove(player.getMediaItemAt(currentMediaItemIndex))
                }

            player.moveMediaItem(currentMediaItemIndex, 0)
            player.replaceMediaItems(1, Int.MAX_VALUE, shuffledQueue)
            player.setShuffleOrder(UnshuffledShuffleOrder(player.mediaItemCount))

            originalQueue = originalMediaItems
        } else {

            val currentMediaItem = player.currentMediaItem

            player.replaceMediaItems(0, player.mediaItemCount, originalQueue)
            val currentMediaItemIndex = originalQueue.indexOf(currentMediaItem)

            player.seekTo(currentMediaItemIndex, player.currentPosition)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            if (player.shuffleModeEnabled == true) {
                player.setShuffleOrder(UnshuffledShuffleOrder(player.mediaItemCount))
            }
            saveQueue()
        }
    }

    private fun buildCustomCallback(): MediaSession.Callback {
        val customCommands = buildCommandButtons()
        return object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(SessionCommand(Commands.SET_SLEEP_TIMER, Bundle.EMPTY))
                    .add(SessionCommand(Commands.CANCEL_SLEEP_TIMER, Bundle.EMPTY))
                customCommands.forEach { commandButton ->
                    commandButton.sessionCommand?.let { availableSessionCommands.add(it) }
                }
                return MediaSession.ConnectionResult.accept(
                    availableSessionCommands.build(),
                    connectionResult.availablePlayerCommands
                )
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                if (Commands.SET_SLEEP_TIMER == customCommand.customAction) {
                    val minutes = args.getInt("MINUTES", 0)
                    val finishLastSong = args.getBoolean("FINISH_LAST_SONG", false)
                    sleepTimerManager.schedule(minutes, finishLastSong)
                }
                if (Commands.CANCEL_SLEEP_TIMER == customCommand.customAction) {
                    sleepTimerManager.deleteTimer()
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

    }


    private var pausedDueToVolume = false
    override fun onVolumeChanged(level: Int) {
        val settings = playerSettings.value
        val shouldPause = settings.pauseOnVolumeZero
        val shouldResume = settings.resumeWhenVolumeIncreases
        
        if (level < 1 && shouldPause && player.playWhenReady == true) {
            player.pause()
            if (shouldResume)
                pausedDueToVolume = true
        }
        if (level >= 1 && pausedDueToVolume && shouldResume && player.playWhenReady == false) {
            player.play()
            pausedDueToVolume = false
        }
        if (player.playWhenReady == true) pausedDueToVolume = false
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY)
            pausedDueToVolume = false
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("PlaybackService started: %s", intent.toString())
        Log.d("PlaybackService", "PlaybackService started: %s" + intent.toString())
        return START_NOT_STICKY
    }

    override fun onSleepTimerFinished() {
        player.pause()
        sleepTimerManager.deleteTimer()
    }

    override fun onDestroy() {
        Timber.d("onDestroy called")
        scope.cancel()
        runBlocking {
            saveCurrentPosition()
        }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        volumeObserver.unregister()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Timber.d("onTaskRemoved called")
        Log.d("PlaybackService", "onTaskRemoved Called")
        if (player.playWhenReady == false) {
            stopSelf()
        }
    }


    companion object {
        const val TAG = "MEDIA_SESSION"
        const val VIEW_MEDIA_SCREEN_ACTION = "MEDIA_SCREEN_ACTION"
    }

}