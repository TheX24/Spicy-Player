package com.tx24.spicyplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.DefaultDataSource

/**
 * A wrapper around ExoPlayer to manage audio playback.
 * Handles playlist preparation and resource management.
 */
class AudioPlayer(private val context: Context) {
    /**
     * The underlying [ExoPlayer] instance.
     */
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    /**
     * Prepares a playlist from a list of URIs (file paths).
     * Uses a [ConcatenatingMediaSource] to support gapless playback for FLAC files.
     *
     * @param uris List of absolute file paths to the audio files.
     */
    fun preparePlaylist(uris: List<String>) {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
        
        // Use MediaSource concat adapter for FLAC gapless support
        val concatenatingMediaSource = ConcatenatingMediaSource()
        for (uri in uris) {
            val mediaItem = MediaItem.fromUri(uri)
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        
        player.setMediaSource(concatenatingMediaSource)
        player.prepare()
        player.playWhenReady = true
    }
    
    /**
     * Releases the audio player resources.
     * Should be called when the player is no longer needed (e.g., in Activity.onDestroy).
     */
    fun release() {
        player.release()
    }
}
