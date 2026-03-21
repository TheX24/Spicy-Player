package com.tx24.spicyplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import com.tx24.spicyplayer.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tx24.spicyplayer.models.Line
import com.tx24.spicyplayer.parser.TtmlLyricsParser
import com.tx24.spicyplayer.player.AudioPlayer
import com.tx24.spicyplayer.ui.canvas.SpicyLyricsView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * The main entry point of the Spicy Player application.
 * This activity initializes the [AudioPlayer] and sets up the Jetpack Compose UI.
 */
class MainActivity : ComponentActivity() {
    /**
     * The audio player instance used for media playback.
     */
    private lateinit var audioPlayer: AudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the audio player with the application context.
        audioPlayer = AudioPlayer(this)
        
        // Set the content of the activity using Jetpack Compose.
        setContent {
            SpicyPlayerApp(audioPlayer)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release audio player resources when the activity is destroyed.
        audioPlayer.release()
    }
}

/**
 * The main UI component of the Spicy Player application.
 * This composable manages the application state, permissions, and layout.
 *
 * @param audioPlayer The [AudioPlayer] instance for controlling playback.
 */
@Composable
fun SpicyPlayerApp(audioPlayer: AudioPlayer) {
    val coroutineScope = rememberCoroutineScope()
    
    // State for the parsed lyrics lines.
    var lines by remember { mutableStateOf<List<Line>>(emptyList()) }
    
    // State for the current playback time in milliseconds.
    var currentTimeMs by remember { mutableStateOf(0L) }
    
    // State indicating whether audio is currently playing.
    var isPlaying by remember { mutableStateOf(false) }

    // Define the required permissions based on the Android version.
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Launcher for requesting multiple permissions.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { _ -> /* Handle permission results if necessary */ }
    )

    // Request permissions when the app starts.
    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions)
    }

    // A loop to update the currentTimeMs while audio is playing.
    // This includes a synchronization logic to match the player's actual position.
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            currentTimeMs = audioPlayer.player.currentPosition
            var lastTime = System.currentTimeMillis()
            while (isPlaying) {
                val now = System.currentTimeMillis()
                val dt = now - lastTime
                lastTime = now
                
                val actualPos = audioPlayer.player.currentPosition
                val diff = actualPos - currentTimeMs
                
                // If the drift is significant (>200ms), snap to the actual position.
                if (kotlin.math.abs(diff) > 200) {
                    currentTimeMs = actualPos
                } else {
                    // Apply a small correction to smooth out minor drifts.
                    val correction = diff * 0.1f
                    currentTimeMs += dt + correction.toLong()
                }
                delay(16) // Update at approximately 60fps.
            }
        }
    }

    // Define the application theme and surface.
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121212) // Dark background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header row with the app title and a scan button.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(48.dp).padding(end = 12.dp)
                    )
                    Text("Spicy Player", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(onClick = {
                        coroutineScope.launch {
                            try {
                                // Find any .ttml and .flac in the Music directory for testing.
                                // TODO: Implement a proper file picker.
                                val musicDir = File("/sdcard/Music/")
                                val files = musicDir.listFiles() ?: emptyArray()
                                
                                val ttmlFile = files.firstOrNull { it.name.endsWith(".ttml", ignoreCase = true) }
                                if (ttmlFile != null && ttmlFile.exists()) {
                                    lines = TtmlLyricsParser.parse(ttmlFile.inputStream()).lines
                                }
                                
                                val flacFile = files.firstOrNull { it.name.endsWith(".flac", ignoreCase = true) }
                                if (flacFile != null && flacFile.exists()) {
                                    audioPlayer.preparePlaylist(listOf(flacFile.absolutePath))
                                    isPlaying = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }) {
                        Text("Scan Ext")
                    }
                }
                
                // The main lyrics display area.
                Box(modifier = Modifier.weight(1f)) {
                    SpicyLyricsView(
                        lines = lines,
                        currentTimeMs = currentTimeMs,
                        onSeekWord = { timeMs -> 
                            // Seek both the audio player and the UI state.
                            audioPlayer.player.seekTo(timeMs)
                            currentTimeMs = timeMs
                        }
                    )
                }
                
                // Bottom control bar with a Play/Pause button.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = { 
                        if (audioPlayer.player.isPlaying) {
                            audioPlayer.player.pause()
                            isPlaying = false
                        } else {
                            audioPlayer.player.play()
                            isPlaying = true
                        }
                    }) {
                        Text(if (isPlaying) "Pause" else "Play")
                    }
                }
            }
        }
    }
}
