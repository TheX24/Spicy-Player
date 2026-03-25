package com.tx24.spicyplayer.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A [ForwardingPlayer] that delegates to an "active" player instance.
 * This allows switching the underlying player (e.g., during crossfade)
 * without the MediaSession losing its connection to the "active" playback state.
 */
class CrossfadePlayerWrapper(initialPlayer: Player) : ForwardingPlayer(initialPlayer) {
    
    var onSkipNext: (() -> Unit)? = null
    var onSkipPrevious: (() -> Unit)? = null

    private var activePlayer: Player = initialPlayer
    private val listeners = CopyOnWriteArraySet<Player.Listener>()

    /**
     * Switches the player that this wrapper delegates to.
     * This should be called by the crossfade logic when the "new" player
     * becomes the primary player that the user/OS should see.
     */
    fun switchActivePlayer(newPlayer: Player) {
        if (activePlayer === newPlayer) return
        
        val oldPlayer = activePlayer
        // Update the internal delegate
        activePlayer = newPlayer
        
        // CRITICAL: Migrate listeners so the OS doesn't lose track of playback
        for (listener in listeners) {
            oldPlayer.removeListener(listener)
            newPlayer.addListener(listener)
        }
        
        // Manually notify listeners of the switch so MediaSession stays in sync
        for (listener in listeners) {
            listener.onMediaItemTransition(
                newPlayer.currentMediaItem,
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
            )
            listener.onPlaybackStateChanged(newPlayer.playbackState)
            listener.onPlayWhenReadyChanged(
                newPlayer.playWhenReady,
                Player.PLAYBACK_SUPPRESSION_REASON_NONE
            )
            listener.onIsPlayingChanged(newPlayer.isPlaying)
            listener.onTimelineChanged(
                newPlayer.currentTimeline,
                Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED
            )
            listener.onMediaMetadataChanged(newPlayer.mediaMetadata)
        }
    }

    // --- Delegation Overrides ---

    override fun play() { activePlayer.play() }
    override fun pause() { activePlayer.pause() }
    override fun seekTo(positionMs: Long) { activePlayer.seekTo(positionMs) }
    override fun seekTo(mediaItemIndex: Int, positionMs: Long) { activePlayer.seekTo(mediaItemIndex, positionMs) }
    
    override fun getPlayWhenReady(): Boolean = activePlayer.playWhenReady
    override fun setPlayWhenReady(playWhenReady: Boolean) { activePlayer.playWhenReady = playWhenReady }
    
    override fun getPlaybackState(): Int = activePlayer.playbackState
    override fun isPlaying(): Boolean = activePlayer.isPlaying
    
    override fun getCurrentMediaItem(): androidx.media3.common.MediaItem? = activePlayer.currentMediaItem
    override fun getCurrentTimeline(): androidx.media3.common.Timeline = activePlayer.currentTimeline
    override fun getMediaMetadata(): androidx.media3.common.MediaMetadata = activePlayer.mediaMetadata
    override fun getDuration(): Long = activePlayer.duration
    override fun getCurrentPosition(): Long = activePlayer.currentPosition
    override fun getBufferedPosition(): Long = activePlayer.bufferedPosition
    
    override fun getVolume(): Float = activePlayer.volume
    override fun setVolume(volume: Float) { activePlayer.volume = volume }

    // --- Skip & Command Overrides ---

    override fun hasNextMediaItem(): Boolean = true
    override fun hasPreviousMediaItem(): Boolean = true

    override fun seekToNext() { onSkipNext?.invoke() }
    override fun seekToNextMediaItem() { onSkipNext?.invoke() }
    override fun seekToPrevious() { onSkipPrevious?.invoke() }
    override fun seekToPreviousMediaItem() { onSkipPrevious?.invoke() }

    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()
    }

    override fun addListener(listener: Player.Listener) {
        listeners.add(listener)
        activePlayer.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        listeners.remove(listener)
        activePlayer.removeListener(listener)
    }
}
