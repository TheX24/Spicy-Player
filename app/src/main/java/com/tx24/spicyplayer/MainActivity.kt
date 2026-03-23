package com.tx24.spicyplayer

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import java.text.Normalizer
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import com.tx24.spicyplayer.models.Line
import com.tx24.spicyplayer.models.NowPlayingData
import com.tx24.spicyplayer.models.NowPlayingMetadata
import com.tx24.spicyplayer.parser.TtmlLyricsParser
import com.tx24.spicyplayer.player.AudioPlayer
import com.tx24.spicyplayer.ui.canvas.DynamicBackgroundView
import com.tx24.spicyplayer.ui.canvas.SpicyLyricsView
import com.tx24.spicyplayer.ui.components.NowPlayingHeader
import com.tx24.spicyplayer.ui.components.NowPlayingControls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var audioPlayer: AudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        audioPlayer = AudioPlayer(this)
        setContent {
            SpicyPlayerApp(audioPlayer)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpicyPlayerApp(audioPlayer: AudioPlayer) {
    val coroutineScope = rememberCoroutineScope()

    // ── Core playback state ────────────────────────────────────────────────
    var lines by remember { mutableStateOf<List<Line>>(emptyList()) }
    var currentTimeMs by remember { mutableStateOf(0L) }
    var currentDurationMs by remember { mutableLongStateOf(1L) }
    var isPlaying by remember { mutableStateOf(false) }
    var coverArtBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var trackName by remember { mutableStateOf("") }
    var artistName by remember { mutableStateOf("") }
    var currentSongIndex by remember { mutableIntStateOf(-1) }
    var songPairs by remember { mutableStateOf<List<Pair<File, File>>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }

    // ── Queue & controls state ─────────────────────────────────────────────
    // playQueue: empty = no explicit queue (sequential through songPairs)
    var playQueue by remember { mutableStateOf<List<Pair<File, File>>>(emptyList()) }
    var playQueueIndex by remember { mutableIntStateOf(0) }
    var loopMode by remember { mutableIntStateOf(0) }   // 0=off 1=all 2=one
    var showMenu by remember { mutableStateOf(false) }
    var controlsCollapsed by remember { mutableStateOf(false) }

    // ── Dynamic color from cover art ──────────────────────────────────────
    var dynamicPrimaryColor by remember { mutableStateOf(Color(0xFF6650A4)) }
    var currentBitrate by remember { mutableStateOf("") }
    var currentFormat by remember { mutableStateOf("") }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var lastSongStartTime by remember { mutableLongStateOf(0L) }

    val context = LocalContext.current
    val appLogo = remember { BitmapFactory.decodeResource(context.resources, R.drawable.logo) }

    LaunchedEffect(coverArtBitmap) {
        val bmp = coverArtBitmap ?: appLogo
        if (bmp != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val palette = Palette.from(bmp).generate()
                val rgb = palette.getVibrantColor(
                    palette.getMutedColor(
                        palette.getDominantColor(0xFF6650A4.toInt())
                    )
                )
                launch(Dispatchers.Main) { dynamicPrimaryColor = Color(rgb) }
            }
        } else {
            dynamicPrimaryColor = Color(0xFF6650A4)
        }
    }

    val colorScheme = remember(dynamicPrimaryColor) {
        val onPrimary = if (dynamicPrimaryColor.luminance() > 0.4f) Color.Black else Color.White
        darkColorScheme(
            primary = dynamicPrimaryColor,
            onPrimary = onPrimary,
            primaryContainer = dynamicPrimaryColor.copy(alpha = 0.25f),
            onPrimaryContainer = onPrimary,
            secondary = dynamicPrimaryColor.copy(alpha = 0.8f),
            onSecondary = onPrimary,
            surface = Color(0xFF1C1B1F),
            onSurface = Color.White,
            background = Color(0xFF121212),
            onBackground = Color.White,
        )
    }

    // ── Header animation ──────────────────────────────────────────────────
    val nowPlayingData = remember(currentSongIndex, trackName, artistName, coverArtBitmap) {
        NowPlayingData(currentSongIndex, trackName, artistName, coverArtBitmap)
    }

    val headerProgress by animateFloatAsState(
        targetValue = if (currentSongIndex == -1) 0f else 1f,
        animationSpec = tween(800, easing = LinearOutSlowInEasing)
    )

    val currentImageSize = 48.dp + (72.dp * headerProgress)
    val currentSpacerWidth = (16.dp * headerProgress)
    val currentHeaderBias = -1f + headerProgress
    val metadataAlpha = headerProgress

    // ── Permissions ───────────────────────────────────────────────────────
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { _ -> }
    )
    LaunchedEffect(Unit) { permissionLauncher.launch(permissions) }

    // ── Normalisation helpers ─────────────────────────────────────────────
    fun robustNormalize(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC)
        .lowercase().trim().replace(Regex("\\s+"), " ")

    fun fuzzyNormalize(s: String): String =
        robustNormalize(s)
            .replace(Regex("\\s*[\\[({].*?[\\])}]\\s*"), " ")
            .replace(Regex("[^\\p{L}\\p{N}]"), "")
            .trim()

    // ── Initial scan ──────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (songPairs.isEmpty() && !isScanning) {
            isScanning = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val musicDir = File("/sdcard/Music/")
                    if (!musicDir.exists()) {
                        Log.e("SpicyPlayer", "Music directory not found: ${musicDir.absolutePath}")
                        return@launch
                    }

                    val allFiles = musicDir.walkTopDown().filter { it.isFile }.toList()
                    val audioExtensions = listOf("flac", "mp3", "m4a", "wav", "ogg", "aac")
                    val audioFiles = allFiles.filter { it.extension.lowercase() in audioExtensions }
                    val ttmlFiles = allFiles.filter { it.name.endsWith(".ttml", ignoreCase = true) }

                    Log.d("SpicyPlayer", "Found ${audioFiles.size} audio, ${ttmlFiles.size} TTML")

                    val scannedPairs = audioFiles.mapNotNull { audioFile ->
                        val baseName = robustNormalize(audioFile.nameWithoutExtension)
                        val fuzzyBase = fuzzyNormalize(audioFile.nameWithoutExtension)

                        var ttmlFile = allFiles.find {
                            it.name.endsWith(".ttml", ignoreCase = true) &&
                                robustNormalize(it.nameWithoutExtension) == baseName
                        }
                        if (ttmlFile == null) {
                            ttmlFile = allFiles.find {
                                it.name.endsWith(".ttml", ignoreCase = true) &&
                                    fuzzyNormalize(it.nameWithoutExtension) == fuzzyBase
                            }
                        }
                        if (ttmlFile == null && fuzzyBase.length >= 3) {
                            ttmlFile = ttmlFiles.find {
                                fuzzyNormalize(it.nameWithoutExtension).contains(fuzzyBase) ||
                                    fuzzyBase.contains(fuzzyNormalize(it.nameWithoutExtension))
                            }
                        }
                        if (ttmlFile == null) {
                            try {
                                val retriever = MediaMetadataRetriever()
                                audioFile.inputStream().use { retriever.setDataSource(it.fd) }
                                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                                retriever.release()
                                if (!title.isNullOrBlank()) {
                                    val fuzzyTitle = fuzzyNormalize(title)
                                    if (fuzzyTitle.length >= 3) {
                                        ttmlFile = ttmlFiles.find {
                                            val ft = fuzzyNormalize(it.nameWithoutExtension)
                                            ft == fuzzyTitle || ft.contains(fuzzyTitle) || fuzzyTitle.contains(ft)
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        if (ttmlFile == null) {
                            val latinOnly = fuzzyBase.replace(Regex("[^a-zA-Z0-9]"), "")
                            if (latinOnly.length >= 5) {
                                ttmlFile = ttmlFiles.find {
                                    val tl = fuzzyNormalize(it.nameWithoutExtension).replace(Regex("[^a-zA-Z0-9]"), "")
                                    tl == latinOnly && tl.isNotEmpty()
                                }
                            }
                        }

                        if (ttmlFile != null) {
                            Log.d("SpicyPlayer", "Paired: ${audioFile.name} -> ${ttmlFile.name}")
                            audioFile to ttmlFile
                        } else {
                            Log.w("SpicyPlayer", "No TTML for: ${audioFile.name}")
                            null
                        }
                    }.sortedBy { it.first.name.lowercase() }

                    launch(Dispatchers.Main) {
                        songPairs = scannedPairs
                        isScanning = false
                    }
                } catch (e: Exception) {
                    Log.e("SpicyPlayer", "Scan error", e)
                    isScanning = false
                }
            }
        }
    }

    // ── Song loading helpers ──────────────────────────────────────────────
    val loadSongFromPair = { pair: Pair<File, File>, indexInSongPairs: Int, playWait: Boolean ->
        val (selectedAudio, selectedTtml) = pair
        coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.d("SpicyPlayer", "Loading: ${selectedAudio.name}, playWait=$playWait")

                val parsedLyrics = try {
                    selectedTtml.inputStream().use { TtmlLyricsParser.parse(it) }
                } catch (e: Exception) {
                    Log.e("SpicyPlayer", "TTML parse failed", e); null
                }

                val result = try {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(selectedAudio.absolutePath)
                        val t = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: selectedAudio.nameWithoutExtension
                        val a = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: "Unknown Artist"
                        val br = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                            ?.let { (it.toInt() / 1000).toString() + " kbps" } ?: ""
                        val ext = selectedAudio.extension.uppercase()
                        
                        val artBytes = retriever.embeddedPicture
                        val bmp = artBytes?.let {
                            val raw = BitmapFactory.decodeByteArray(it, 0, it.size)
                            if (raw.width > 1024 || raw.height > 1024)
                                Bitmap.createScaledBitmap(raw, 1024, 1024, true) else raw
                        }
                        
                        NowPlayingMetadata(t, a, bmp, br, ext)
                    } finally {
                        retriever.release()
                    }
                } catch (e: Exception) {
                    Log.e("SpicyPlayer", "Metadata error", e)
                    NowPlayingMetadata(selectedAudio.nameWithoutExtension, "Unknown Artist", null, "", selectedAudio.extension.uppercase())
                }

                launch(Dispatchers.Main) {
                    lines = parsedLyrics?.lines ?: emptyList()
                    trackName = result.t
                    artistName = result.a
                    coverArtBitmap = result.art
                    currentBitrate = result.bitrate
                    currentFormat = result.ext
                    currentSongIndex = indexInSongPairs
                    audioPlayer.preparePlaylist(listOf(selectedAudio.absolutePath), playWhenReady = playWait)
                    isPlaying = playWait
                }
            } catch (e: Exception) {
                Log.e("SpicyPlayer", "Load error", e)
            }
        }
    }

    val loadSong = { index: Int, playWait: Boolean ->
        if (index in songPairs.indices) {
            loadSongFromPair(songPairs[index], index, playWait)
        }
    }

    // ── Queue helpers ─────────────────────────────────────────────────────
    val loadNext = {
        Log.d("SpicyPlayer", "loadNext: current=$currentSongIndex, loopMode=$loopMode")
        when {
            loopMode == 2 -> {
                // Loop one: replay current
                audioPlayer.player.seekTo(0)
                audioPlayer.player.play()
                isPlaying = true
            }
            playQueue.isNotEmpty() -> {
                val next = if (loopMode == 1)
                    (playQueueIndex + 1) % playQueue.size
                else
                    (playQueueIndex + 1)
                
                if (next < playQueue.size) {
                    playQueueIndex = next
                    loadSongFromPair(playQueue[next], songPairs.indexOf(playQueue[next]), true)
                } else {
                    // End of queue
                    isPlaying = false
                }
            }
            songPairs.isNotEmpty() -> {
                val next = (currentSongIndex + 1)
                if (next < songPairs.size) {
                    loadSong(next, true)
                } else if (loopMode == 1) {
                    loadSong(0, true)
                } else {
                    // End of playlist
                    isPlaying = false
                }
            }
        }
    }

    val loadPrevious = {
        Log.d("SpicyPlayer", "loadPrevious: current=$currentSongIndex")
        when {
            playQueue.isNotEmpty() -> {
                val prev = (playQueueIndex - 1 + playQueue.size) % playQueue.size
                playQueueIndex = prev
                loadSongFromPair(playQueue[prev], songPairs.indexOf(playQueue[prev]), true)
            }
            songPairs.isNotEmpty() -> {
                val prev = (currentSongIndex - 1 + songPairs.size) % songPairs.size
                loadSong(prev, true)
            }
        }
    }

    /** Shuffle: if no queue → shuffle all songs into queue and play first.
     *  If queue exists → reshuffle the queue and play from new first. */
    val handleShuffle = {
        val shuffled = if (playQueue.isEmpty()) songPairs.shuffled() else playQueue.shuffled()
        if (shuffled.isNotEmpty()) {
            playQueue = shuffled
            playQueueIndex = 0
            loadSongFromPair(shuffled[0], songPairs.indexOf(shuffled[0]), true)
        }
    }

    // ── Auto-advance on song end ──────────────────────────────────────────
    // REMOVED startup auto-load as requested
    
    val currentLoadNext by rememberUpdatedState(loadNext)

    DisposableEffect(audioPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val played = System.currentTimeMillis() - lastSongStartTime
                    if (played < 2000) {
                        Log.e("SpicyPlayer", "Rapid skip (${played}ms). Stopping.")
                        isPlaying = false
                    } else {
                        currentLoadNext()
                    }
                } else if (playbackState == Player.STATE_READY) {
                    lastSongStartTime = System.currentTimeMillis()
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("SpicyPlayer", "Player error: ${error.message}", error)
                isPlaying = false
            }
        }
        audioPlayer.player.addListener(listener)
        onDispose { audioPlayer.player.removeListener(listener) }
    }

    // ── Playback time loop ────────────────────────────────────────────────
    LaunchedEffect(isPlaying, isDraggingSlider) {
        if (isPlaying && !isDraggingSlider) {
            currentTimeMs = audioPlayer.player.currentPosition
            var lastTime = System.currentTimeMillis()
            while (isPlaying && !isDraggingSlider) {
                val now = System.currentTimeMillis()
                val dt = now - lastTime
                lastTime = now

                // Update total duration
                val dur = audioPlayer.player.duration
                if (dur > 0) currentDurationMs = dur

                val actualPos = audioPlayer.player.currentPosition
                val diff = actualPos - currentTimeMs
                if (kotlin.math.abs(diff) > 500) {
                    currentTimeMs = actualPos
                } else {
                    val correction = diff * 0.1f
                    currentTimeMs += dt + correction.toLong()
                }
                delay(16)
            }
        }
    }

    // ── Formatting helper ────────────────────────────────────────────────
    fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    // ── colours ───────────────────────────────────────────────────────────
    val onSurface = Color.White
    val primaryTint = colorScheme.primary
    val disabledTint = onSurface.copy(alpha = 0.38f)

    // ── UI ────────────────────────────────────────────────────────────────
    MaterialTheme(colorScheme = colorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            DynamicBackgroundView(coverArtBitmap = coverArtBitmap ?: appLogo)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                // ── Header ──────────────────────────────────────────────
                NowPlayingHeader(
                    nowPlayingData = nowPlayingData,
                    headerProgress = headerProgress,
                    currentImageSize = currentImageSize,
                    currentSpacerWidth = currentSpacerWidth,
                    currentHeaderBias = currentHeaderBias,
                    metadataAlpha = metadataAlpha
                )

                // ── Lyrics ──────────────────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    if (currentSongIndex != -1) {
                        SpicyLyricsView(
                            lines = lines,
                            currentTimeMs = currentTimeMs,
                            onSeekWord = { timeMs ->
                                audioPlayer.player.seekTo(timeMs)
                                currentTimeMs = timeMs
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // ── Floating Bottom Controls ──────────────────────
                NowPlayingControls(
                    controlsCollapsed = controlsCollapsed,
                    onToggleCollapse = { controlsCollapsed = !controlsCollapsed },
                    currentTimeMs = currentTimeMs,
                    currentDurationMs = currentDurationMs,
                    onSeek = { currentTimeMs = it },
                    onSeekFinished = { audioPlayer.player.seekTo(currentTimeMs) },
                    isDraggingSlider = isDraggingSlider,
                    onDraggingChanged = { isDraggingSlider = it },
                    currentBitrate = currentBitrate,
                    currentFormat = currentFormat,
                    primaryTint = primaryTint,
                    onShowMenu = { showMenu = true },
                    onShuffle = { handleShuffle() },
                    isShuffleActive = playQueue.isNotEmpty(),
                    onPrevious = { loadPrevious() },
                    onNext = { loadNext() },
                    isPlaying = isPlaying,
                    onTogglePlay = {
                        if (audioPlayer.player.isPlaying) {
                            audioPlayer.player.pause()
                            isPlaying = false
                        } else {
                            audioPlayer.player.play()
                            isPlaying = true
                        }
                    },
                    loopMode = loopMode,
                    onToggleLoop = { loopMode = (loopMode + 1) % 3 },
                    colorScheme = colorScheme,
                    formatTime = { formatTime(it) },
                    isExpressive = true // Future proof: classic design is kept accessible
                )

                if (showMenu) {
                    ModalBottomSheet(
                        onDismissRequest = { showMenu = false },
                        containerColor = colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "More Options",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
                                color = colorScheme.onSurfaceVariant
                            )
                            
                            ListItem(
                                headlineContent = { Text("Add to Playlist") },
                                leadingContent = { Icon(Icons.Rounded.FavoriteBorder, contentDescription = null) },
                                modifier = Modifier.clickable { showMenu = false },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            ListItem(
                                headlineContent = { Text("Sleep Timer") },
                                leadingContent = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                                modifier = Modifier.clickable { showMenu = false },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            ListItem(
                                headlineContent = { Text("Equalizer") },
                                leadingContent = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                                modifier = Modifier.clickable { showMenu = false },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            ListItem(
                                headlineContent = { Text("Settings") },
                                leadingContent = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                modifier = Modifier.clickable { showMenu = false },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            Spacer(Modifier.height(48.dp))
                        }
                    }
                }
            }
        }
    }
}
