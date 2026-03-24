package com.tx24.spicyplayer.service

import android.app.PendingIntent
import android.content.Intent
import com.tx24.spicyplayer.MainActivity
import android.os.Binder
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tx24.spicyplayer.player.CrossfadePlayerWrapper

/**
 * A service that manages a MediaSession and dual ExoPlayer instances for background playback
 * with crossfading support.
 */
class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    lateinit var player1: ExoPlayer
    lateinit var player2: ExoPlayer
    lateinit var crossfadeWrapper: CrossfadePlayerWrapper

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == SERVICE_INTERFACE) {
            return super.onBind(intent)
        }
        return binder
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // 1. Initialize Dual ExoPlayers
        player1 = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(audioAttributes, true)
        }
        player2 = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(audioAttributes, true)
        }

        // 2. Initialize Crossfade Wrapper
        crossfadeWrapper = CrossfadePlayerWrapper(player1)

        // 3. Initialize MediaSession bound to the wrapper
        // Use a PendingIntent to open the MainActivity when the notification is tapped
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        mediaSession = MediaSession.Builder(this, crossfadeWrapper)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) {
            if (!player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            } else {
                player.pause()
                stopSelf()
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player1.release()
            player2.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
