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
import androidx.media3.common.Player
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

    private val player1 = createPlayer()
    private val player2 = createPlayer()

    var activePlayer: ExoPlayer = player1
        private set

    val player: ExoPlayer
        get() = activePlayer

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var crossfadeJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val playerListeners = mutableListOf<Player.Listener>()

    private fun createPlayer(): ExoPlayer {
        val p = ExoPlayer.Builder(context).build()
        p.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true
        )
        return p
    }

    // ── Audio session setup ───────────────────────────────────────────────────

    /**
     * (Re-)attaches AudioEffect instances to the current audio session.
     * Must be called whenever a new media item starts playing so the session ID is valid.
     */
    fun attachEffects() {
        val sessionId = activePlayer.audioSessionId
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
        crossfadeJob?.cancel()
        
        // Reset the idle player just in case
        val idlePlayer = if (activePlayer === player1) player2 else player1
        idlePlayer.stop()
        idlePlayer.clearMediaItems()
        idlePlayer.volume = 1.0f

        activePlayer.volume = 1.0f

        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

        val concatenatingMediaSource = ConcatenatingMediaSource()
        for (uri in uris) {
            val mediaItem = MediaItem.fromUri(uri)
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            concatenatingMediaSource.addMediaSource(mediaSource)
        }

        activePlayer.setMediaSource(concatenatingMediaSource)
        activePlayer.prepare()
        activePlayer.playWhenReady = playWhenReady

        activePlayer.postDelayed({ attachEffects() }, 200)
    }

    fun playNextWithCrossfade(uris: List<String>, crossfadeDurationMs: Long, playWhenReady: Boolean = true) {
        if (crossfadeDurationMs <= 0 || !activePlayer.isPlaying) {
            preparePlaylist(uris, playWhenReady)
            return
        }

        crossfadeJob?.cancel()

        val oldPlayer = activePlayer
        val newPlayer = if (activePlayer === player1) player2 else player1

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

        // Switch active player
        activePlayer = newPlayer
        
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
        player1.setAudioAttributes(attributes, handleFocus)
        player2.setAudioAttributes(attributes, handleFocus)
    }

    fun addListener(listener: Player.Listener) {
        if (!playerListeners.contains(listener)) {
            playerListeners.add(listener)
            activePlayer.addListener(listener)
        }
    }

    fun removeListener(listener: Player.Listener) {
        playerListeners.remove(listener)
        player1.removeListener(listener)
        player2.removeListener(listener)
    }

    fun pause() {
        crossfadeJob?.cancel()
        player1.pause()
        player2.pause()
    }

    fun play() {
        activePlayer.play()
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
        player1.release()
        player2.release()
    }
}

// Extension to post a delayed action on the player's application thread
private fun ExoPlayer.postDelayed(action: () -> Unit, delayMs: Long) {
    applicationLooper.let {
        android.os.Handler(it).postDelayed(action, delayMs)
    }
}
