package com.tx24.spicyplayer.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import android.net.Uri
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.DefaultDataSource
import kotlinx.coroutines.*

/**
 * A wrapper around ExoPlayer to manage audio playback.
 * Handles playlist preparation, resource management, and real-time audio effects
 * (Equalizer, Bass Boost) via Android AudioEffect API.
 * Uses a dual-player setup for crossfading between tracks.
 */
class AudioPlayer(private val context: Context) {

    private var player1: ExoPlayer? = null
    private var player2: ExoPlayer? = null
    private var crossfadeWrapper: CrossfadePlayerWrapper? = null

    var onSkipNext: (() -> Unit)? = null
    var onSkipPrevious: (() -> Unit)? = null

    var activePlayer: ExoPlayer? = null
        private set

    val player: ExoPlayer?
        get() = activePlayer

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var crossfadeJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val playerListeners = mutableListOf<Player.Listener>()

    /**
     * Injects the players and wrapper from the MediaPlaybackService.
     */
    fun injectPlayers(p1: ExoPlayer, p2: ExoPlayer, wrapper: CrossfadePlayerWrapper) {
        player1 = p1
        player2 = p2
        crossfadeWrapper = wrapper
        
        crossfadeWrapper?.onSkipNext = { onSkipNext?.invoke() }
        crossfadeWrapper?.onSkipPrevious = { onSkipPrevious?.invoke() }
        
        activePlayer = p1
        
        // Re-add any listeners that were registered before injection
        playerListeners.forEach { activePlayer?.addListener(it) }
    }

    /**
     * Helper to create a MediaItem with metadata for the notification/carousel.
     * Uses URIs for artwork to avoid TransactionTooLargeException.
     */
    fun createMediaItem(
        uri: String,
        title: String,
        artist: String,
        artworkUri: String
    ): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(artworkUri))
            .build()

        return MediaItem.Builder()
            .setUri(Uri.parse(uri))
            .setMediaMetadata(metadata)
            .build()
    }

    // ── Audio session setup ───────────────────────────────────────────────────

    /**
     * (Re-)attaches AudioEffect instances to the current audio session.
     * Must be called whenever a new media item starts playing so the session ID is valid.
     */
    fun attachEffects() {
        val currentActive = activePlayer ?: return
        val sessionId = currentActive.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return

        try {
            equalizer?.release()
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
            updateEqualizer()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Equalizer init failed", e)
        }

        try {
            bassBoost?.release()
            bassBoost = BassBoost(0, sessionId).apply { 
                enabled = currentBassBoostEnabled 
                if (enabled) setStrength(currentBassBoostStrength.toShort())
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "BassBoost init failed", e)
        }

        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "LoudnessEnhancer init failed", e)
        }
    }

    // ── Equalizer ─────────────────────────────────────────────────────────────

    private var currentEqLevels = shortArrayOf(0, 0, 0, 0, 0)
    private var isEqCustom = false
    private var currentBassBoostEnabled = false
    private var currentBassBoostStrength = 0

    private fun updateEqualizer() {
        val eq = equalizer ?: return
        if (!eq.enabled) eq.enabled = true

        val numBands = eq.numberOfBands.toInt()
        val minRange = eq.bandLevelRange.getOrNull(0) ?: (-1500).toShort()
        val maxRange = eq.bandLevelRange.getOrNull(1) ?: 1500.toShort()

        for (i in 0 until minOf(numBands, currentEqLevels.size)) {
            var level = currentEqLevels[i].toInt()
            level = level.coerceIn(minRange.toInt(), maxRange.toInt())

            try {
                eq.setBandLevel(i.toShort(), level.toShort())
            } catch (e: Exception) {
                Log.e("AudioPlayer", "EQ setBandLevel($i) failed", e)
            }
        }
    }

    fun applyEqPreset(preset: String) {
        val levels: List<Short> = when (preset) {
            "BASS"       -> listOf(600, 400,    0, -200, -200)
            "TREBLE"     -> listOf(-200, -200,  0,  400,  600)
            "VOCAL"      -> listOf(-100,    0, 300,  300,    0)
            "FLAT"       -> listOf(   0,    0,   0,    0,    0)
            else         -> return
        }

        for (i in 0 until minOf(5, levels.size)) {
            currentEqLevels[i] = levels[i]
        }
        updateEqualizer()
    }

    fun applyEqBands(gainsDb: List<Float>) {
        for (i in 0 until minOf(5, gainsDb.size)) {
            currentEqLevels[i] = (gainsDb[i] * 100).toInt().toShort()
        }
        updateEqualizer()
    }

    // ── Bass Boost ────────────────────────────────────────────────────────────

    fun setBassBoost(enabled: Boolean, strength: Int) {
        currentBassBoostEnabled = enabled
        currentBassBoostStrength = strength
        bassBoost?.enabled = enabled
        if (enabled) {
            try { bassBoost?.setStrength(strength.toShort()) } catch(e: Exception) {}
        }
        updateEqualizer()
    }

    // ── Loudness Enhancer ─────────────────────────────────────────────────────

    fun setLoudness(enabled: Boolean, strength: Int) {
        loudnessEnhancer?.enabled = enabled
        if (enabled) {
            // max slider 100 -> 10000 mB (10dB)
            val gainmB = strength * 100
            try { loudnessEnhancer?.setTargetGain(gainmB) } catch(e: Exception) {}
        }
    }

    // ── Playlist & Crossfade ──────────────────────────────────────────────────

    fun preparePlaylist(uris: List<String>, playWhenReady: Boolean = true) {
        val currentActive = activePlayer ?: return
        val p1 = player1
        val p2 = player2
        val idle = if (currentActive === p1) p2 else p1
        
        crossfadeJob?.cancel()
        
        idle?.stop()
        idle?.clearMediaItems()
        idle?.volume = 1.0f

        currentActive.volume = 1.0f

        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

        val concatenatingMediaSource = ConcatenatingMediaSource()
        for (uri in uris) {
            val mediaItem = MediaItem.fromUri(uri)
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            concatenatingMediaSource.addMediaSource(mediaSource)
        }

        currentActive.setMediaSource(concatenatingMediaSource)
        currentActive.prepare()
        currentActive.playWhenReady = playWhenReady

        currentActive.postDelayed({ attachEffects() }, 200)
    }

    fun playNextWithCrossfade(uris: List<String>, crossfadeDurationMs: Long, playWhenReady: Boolean = true) {
        val currentActive = activePlayer ?: return
        val p1 = player1
        val p2 = player2
        val newPlayer = if (currentActive === p1) p2 else p1
        
        if (newPlayer == null || crossfadeDurationMs <= 0 || !currentActive.isPlaying) {
            preparePlaylist(uris, playWhenReady)
            return
        }

        crossfadeJob?.cancel()

        val oldPlayer = currentActive

        // Temporarily disable auto audio focus on oldPlayer so it doesn't get paused when newPlayer starts
        val attributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        oldPlayer.setAudioAttributes(attributes, false)

        // Initialize new player's source
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

        val concatenatingMediaSource = ConcatenatingMediaSource()
        for (uri in uris) {
            val mediaItem = MediaItem.fromUri(uri)
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            concatenatingMediaSource.addMediaSource(mediaSource)
        }

        newPlayer.setMediaSource(concatenatingMediaSource)
        newPlayer.prepare()
        newPlayer.volume = 0f
        newPlayer.playWhenReady = playWhenReady

        // Switch active player and notify wrapper
        activePlayer = newPlayer
        crossfadeWrapper?.switchActivePlayer(newPlayer)
        
        // Migrate listeners
        playerListeners.forEach { listener ->
            oldPlayer.removeListener(listener)
            newPlayer.addListener(listener)
        }

        newPlayer.postDelayed({ attachEffects() }, 200)

        // Perform crossfade
        val remainingTimeMs = oldPlayer.duration - oldPlayer.currentPosition
        val actualCrossfadeMs = minOf(crossfadeDurationMs, remainingTimeMs).coerceAtLeast(100L)

        crossfadeJob = coroutineScope.launch {
            val startTime = SystemClock.elapsedRealtime()
            val startOldVol = oldPlayer.volume

            while (isActive) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                if (elapsed >= actualCrossfadeMs) break
                
                val progress = elapsed.toFloat() / actualCrossfadeMs.toFloat()
                
                oldPlayer.volume = (startOldVol * (1f - progress)).coerceIn(0f, 1f)
                newPlayer.volume = progress.coerceIn(0f, 1f)
                
                delay(30)
            }

            oldPlayer.stop()
            oldPlayer.clearMediaItems()
            oldPlayer.volume = 1.0f // Restore volume for when it's next used
            newPlayer.volume = 1.0f
        }
    }

    fun updateAudioFocus(mode: String) {
        val handleFocus = mode == "PAUSE"
        val attributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        
        // We only want to set this if the player is not currently fading out
        // For simplicity, just set it on both and playNextWithCrossfade will override it if needed
        player1?.setAudioAttributes(attributes, handleFocus)
        player2?.setAudioAttributes(attributes, handleFocus)
    }

    fun addListener(listener: Player.Listener) {
        if (!playerListeners.contains(listener)) {
            playerListeners.add(listener)
            activePlayer?.addListener(listener)
        }
    }

    fun removeListener(listener: Player.Listener) {
        playerListeners.remove(listener)
        player1?.removeListener(listener)
        player2?.removeListener(listener)
    }

    fun pause() {
        crossfadeJob?.cancel()
        player1?.pause()
        player2?.pause()
    }

    fun play() {
        activePlayer?.play()
    }

    fun release() {
        crossfadeJob?.cancel()
        coroutineScope.cancel()
        equalizer?.release()
        equalizer = null
        bassBoost?.release()
        bassBoost = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        // We don't release players here anymore as they are owned by the service
    }
}

// Extension to post a delayed action on the player's application thread
private fun ExoPlayer.postDelayed(action: () -> Unit, delayMs: Long) {
    applicationLooper.let {
        android.os.Handler(it).postDelayed(action, delayMs)
    }
}
