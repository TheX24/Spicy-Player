package com.tx24.spicyplayer

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.tx24.spicyplayer.player.AudioPlayer
import com.tx24.spicyplayer.service.MediaPlaybackService
import com.tx24.spicyplayer.ui.SpicyPlayerApp

class MainActivity : ComponentActivity() {
    private lateinit var audioPlayer: AudioPlayer
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var playbackService: MediaPlaybackService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlaybackService.LocalBinder
            playbackService = binder.getService()
            audioPlayer.injectPlayers(
                playbackService!!.player1,
                playbackService!!.player2,
                playbackService!!.crossfadeWrapper
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        
        audioPlayer = AudioPlayer(this)
        
        // Bind to MediaPlaybackService
        val intent = Intent(this, MediaPlaybackService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Initialize MediaController
        val sessionToken = SessionToken(this, ComponentName(this, MediaPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            // Here you could link mediaController to audioPlayer if needed
        }, MoreExecutors.directExecutor())

        setContent {
            SpicyPlayerApp(audioPlayer)
        }
    }

    /**
     * Demonstrates how to initiate playback for the first time.
     * Starts the MediaPlaybackService as a foreground service to comply with Android 14.
     */
    private fun playTestTrack() {
        val testUri = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"
        val mediaItem = audioPlayer.createMediaItem(
            uri = testUri,
            title = "Test Track",
            artist = "Spicy Artist",
            artworkUri = "https://storage.googleapis.com/exoplayer-test-media-0/artwork.png"
        )
        
        // 1. Prepare the playlist on the player
        audioPlayer.preparePlaylist(listOf(testUri))
        
        // 2. Explicitly start the service to ensure foreground state for Android 14
        val intent = Intent(this, MediaPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        // 3. Begin playback
        audioPlayer.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        audioPlayer.release()
    }

    fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
