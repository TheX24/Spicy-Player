package com.tx24.spicyplayer.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.provider.DocumentsContract
import android.view.WindowManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import com.tx24.spicyplayer.MainActivity
import com.tx24.spicyplayer.R
import com.tx24.spicyplayer.service.ScanService
import android.content.ServiceConnection
import android.content.ComponentName
import android.os.IBinder
import android.os.Build
import android.os.PowerManager
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ExperimentalMaterial3Api
import com.tx24.spicyplayer.models.Line
import com.tx24.spicyplayer.models.NowPlayingData
import com.tx24.spicyplayer.models.NowPlayingMetadata
import com.tx24.spicyplayer.parser.TtmlLyricsParser
import com.tx24.spicyplayer.player.AudioPlayer
import com.tx24.spicyplayer.util.ScanProgress
import com.tx24.spicyplayer.ui.canvas.DynamicBackgroundView
import com.tx24.spicyplayer.ui.canvas.SpicyLyricsView
import com.tx24.spicyplayer.ui.components.NowPlayingHeader
import com.tx24.spicyplayer.ui.components.NowPlayingControls
import com.tx24.spicyplayer.ui.components.AppMenuBottomSheet
import com.tx24.spicyplayer.ui.settings.EqualizerScreen
import com.tx24.spicyplayer.ui.settings.SettingsScreen
import com.tx24.spicyplayer.util.formatTime
import com.tx24.spicyplayer.util.performScan
import com.tx24.spicyplayer.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpicyPlayerApp(audioPlayer: AudioPlayer) {
    val context = LocalContext.current
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
    var songPairs by remember { mutableStateOf<List<Pair<File, File?>>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(ScanProgress()) }
    var scanHistory by remember { mutableStateOf(listOf<String>()) }
    var isCrossfadingTriggered by remember { mutableStateOf(false) }
    var loadingJob by remember { mutableStateOf<Job?>(null) }
    
    // Scan Service Connection
    var scanService by remember { mutableStateOf<ScanService?>(null) }
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as ScanService.ScanBinder
                scanService = binder.getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                scanService = null
            }
        }
    }

    LaunchedEffect(Unit) {
        val intent = Intent(context, ScanService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // Collect progress from service
    LaunchedEffect(scanService) {
        scanService?.let { service ->
            service.progressFlow.collect { progress ->
                scanProgress = progress
                isScanning = true
                if (!progress.isUpdating && progress.summary.isNotEmpty()) {
                    scanHistory = scanHistory + progress.summary
                }
            }
        }
    }

    // Collect results from service
    LaunchedEffect(scanService) {
        scanService?.let { service ->
            service.resultFlow.collect { results ->
                songPairs = results
                isScanning = false
                Toast.makeText(context, "Matched ${results.size} songs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission Launcher for Notifications (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission denied. Sync progress hidden.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(isScanning) {
        if (isScanning && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ── Queue & controls state ─────────────────────────────────────────────
    // playQueue: empty = no explicit queue (sequential through songPairs)
    var playQueue by remember { mutableStateOf<List<Pair<File, File?>>>(emptyList()) }
    var playQueueIndex by remember { mutableIntStateOf(0) }
    var loopMode by remember { mutableIntStateOf(0) }   // 0=off 1=all 2=one
    var showMenu by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(AppScreen.NOW_PLAYING) }
    var eqNavigationSource by remember { mutableStateOf(AppScreen.NOW_PLAYING) }
    var controlsCollapsed by remember { mutableStateOf(false) }

    // ── Dynamic color from cover art ──────────────────────────────────────
    var dynamicPrimaryColor by remember { mutableStateOf(Color(0xFF6650A4)) }
    var currentBitrate by remember { mutableStateOf("") }
    var currentFormat by remember { mutableStateOf("") }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var lastSongStartTime by remember { mutableLongStateOf(0L) }

    val appLogo = remember { BitmapFactory.decodeResource(context.resources, R.drawable.logo) }

    // ── Metadata Cache ────────────────────────────────────────────────────
    val metadataCache = remember { LruCache<String, NowPlayingMetadata>(25) }

    // ── Settings ──────────────────────────────────────────────────────────
    val settingsVm: SettingsViewModel = viewModel()
    val appTheme           by settingsVm.appTheme.collectAsStateWithLifecycle()
    val materialYou        by settingsVm.materialYou.collectAsStateWithLifecycle()
    val pureBlack          by settingsVm.pureBlack.collectAsStateWithLifecycle()
    val keepScreenOn       by settingsVm.keepScreenOn.collectAsStateWithLifecycle()
    val audioFocusSetting  by settingsVm.audioFocus.collectAsStateWithLifecycle()
    val controlsStyle      by settingsVm.controlsStyle.collectAsStateWithLifecycle()
    val blurIntensity      by settingsVm.backgroundBlur.collectAsStateWithLifecycle()
    val eqPreset           by settingsVm.eqPreset.collectAsStateWithLifecycle()
    val bassBoostEnabled   by settingsVm.bassBoostEnabled.collectAsStateWithLifecycle()
    val lyricsFontSizeStr  by settingsVm.lyricsFontSize.collectAsStateWithLifecycle()
    val lyricsOffsetMs     by settingsVm.lyricsOffsetMs.collectAsStateWithLifecycle()
    val bassBoostStrength  by settingsVm.bassBoostStrength.collectAsStateWithLifecycle()
    val crossfadeDuration  by settingsVm.crossfadeDuration.collectAsStateWithLifecycle()
    val backSkipThreshold  by settingsVm.backSkipThreshold.collectAsStateWithLifecycle()
    val customEqBands      by settingsVm.customEqBands.collectAsStateWithLifecycle()
    val contrastLevel      by settingsVm.contrastLevel.collectAsStateWithLifecycle()
    val loudnessEnabled    by settingsVm.loudnessEnabled.collectAsStateWithLifecycle()
    val loudnessStrength   by settingsVm.loudnessStrength.collectAsStateWithLifecycle()
    
    // ── Apply dynamic audio settings ──────────────────────────────────────
    LaunchedEffect(audioFocusSetting) {
        audioPlayer.updateAudioFocus(audioFocusSetting)
    }

    // ── Back handling ─────────────────────────────────────────────────────
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    
    BackHandler {
        if (currentScreen == AppScreen.EQUALIZER) {
            currentScreen = eqNavigationSource
        } else if (currentScreen != AppScreen.NOW_PLAYING) {
            currentScreen = AppScreen.NOW_PLAYING
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                (context as? ComponentActivity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Derive typed values
    val lyricsFontScale = when (lyricsFontSizeStr) { "SMALL" -> 0.80f; "LARGE" -> 1.25f; else -> 1.0f }
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appTheme) { "LIGHT" -> false; "DARK" -> true; else -> systemDark }

    // ── Immersive mode persistence ─────────────────────────────────────────
    LaunchedEffect(showMenu) {
        if (showMenu) {
            (context as? MainActivity)?.hideSystemBars()
        }
    }

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

    // ── Color scheme: Material You override + pure black + theme ─────────
    val colorScheme = remember(dynamicPrimaryColor, materialYou, isDark, pureBlack, contrastLevel) {
        val base = when {
            materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> {
                val onPrimary = if (dynamicPrimaryColor.luminance() > 0.4f) Color.Black else Color.White
                if (isDark) {
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
                } else {
                    lightColorScheme(
                        primary = dynamicPrimaryColor,
                        onPrimary = if (dynamicPrimaryColor.luminance() > 0.4f) Color.Black else Color.White,
                    )
                }
            }
        }
        
        // AMOLED pure black override
        val withBlack = if (pureBlack && isDark) {
            base.copy(background = Color.Black, surface = Color.Black)
        } else base

        // Apply Contrast Level
        if (contrastLevel > 0f) {
            val fgBoost = contrastLevel * 0.3f
            val bgBoost = contrastLevel * 0.5f
            
            val fgTarget = if (isDark) Color.White else Color.Black
            val bgTarget = if (isDark) Color.Black else Color.White
            
            withBlack.copy(
                background = androidx.compose.ui.graphics.lerp(withBlack.background, bgTarget, bgBoost),
                surface = androidx.compose.ui.graphics.lerp(withBlack.surface, bgTarget, bgBoost),
                surfaceVariant = androidx.compose.ui.graphics.lerp(withBlack.surfaceVariant, bgTarget, bgBoost),
                primary = androidx.compose.ui.graphics.lerp(withBlack.primary, fgTarget, fgBoost),
                onPrimary = androidx.compose.ui.graphics.lerp(withBlack.onPrimary, bgTarget, bgBoost),
                onSurface = androidx.compose.ui.graphics.lerp(withBlack.onSurface, fgTarget, fgBoost),
                onBackground = androidx.compose.ui.graphics.lerp(withBlack.onBackground, fgTarget, fgBoost),
                onSurfaceVariant = androidx.compose.ui.graphics.lerp(withBlack.onSurfaceVariant, fgTarget, fgBoost),
                secondary = androidx.compose.ui.graphics.lerp(withBlack.secondary, fgTarget, fgBoost),
                onSecondary = androidx.compose.ui.graphics.lerp(withBlack.onSecondary, bgTarget, bgBoost),
                primaryContainer = androidx.compose.ui.graphics.lerp(withBlack.primaryContainer, bgTarget, bgBoost),
                onPrimaryContainer = androidx.compose.ui.graphics.lerp(withBlack.onPrimaryContainer, fgTarget, fgBoost)
            )
        } else withBlack
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

    // ── Keep screen on ────────────────────────────────────────────────────
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        val window = (view.context as? ComponentActivity)?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // ── Audio focus behavior ──────────────────────────────────────────────
    DisposableEffect(audioFocusSetting) {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    if (audioFocusSetting != "IGNORE") {
                        audioPlayer.pause()
                        isPlaying = false
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (audioFocusSetting == "PAUSE") {
                        audioPlayer.pause()
                        isPlaying = false
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    when (audioFocusSetting) {
                        "DUCK"  -> audioPlayer.player?.volume = 0.3f
                        "PAUSE" -> { audioPlayer.pause(); isPlaying = false }
                        // IGNORE -> do nothing
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    audioPlayer.player?.volume = 1.0f
                    if (audioFocusSetting != "IGNORE" && !isPlaying) {
                        audioPlayer.play()
                        isPlaying = true
                    }
                }
            }
        }
        val req = AudioManager.OnAudioFocusChangeListener { focusChange -> focusListener.onAudioFocusChange(focusChange) }
        onDispose { /* listener cleanup managed by ExoPlayer internally */ }
    }

    // ── EQ preset side-effect ─────────────────────────────────────────────
    LaunchedEffect(eqPreset, customEqBands) { 
        if (eqPreset == "CUSTOM") {
            audioPlayer.applyEqBands(customEqBands)
        } else {
            audioPlayer.applyEqPreset(eqPreset) 
        }
    }

    // ── Bass boost & Loudness side-effects ─────────────────────────────────
    LaunchedEffect(bassBoostEnabled, bassBoostStrength) { audioPlayer.setBassBoost(bassBoostEnabled, bassBoostStrength) }
    LaunchedEffect(loudnessEnabled, loudnessStrength) { audioPlayer.setLoudness(loudnessEnabled, loudnessStrength) }

    val triggerRescan = {
        if (!isScanning) {
            isScanning = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val scanPath = settingsVm.scanDirectory.value.ifBlank { "/sdcard/Music/" }
                    scanHistory = emptyList()
                    val intent = Intent(context, ScanService::class.java).apply {
                        putExtra("scan_path", scanPath)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } catch (e: Exception) {
                    Log.e("SpicyPlayer", "Rescan start error", e)
                    launch(Dispatchers.Main) { isScanning = false }
                }
            }
        }
    }

    // ── Directory Picker Launcher ──────────────────────────────────────────
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    
                    val documentId = DocumentsContract.getTreeDocumentId(it)
                    val split = documentId.split(":")
                    val type = split[0]
                    val relativePath = if (split.size > 1) split[1] else ""
                    
                    val path = if ("primary".equals(type, ignoreCase = true)) {
                        "/sdcard/$relativePath"
                    } else {
                        "/storage/$type/$relativePath"
                    }

                    settingsVm.setScanDirectory(path)
                    // Auto rescan when directory changes
                    isScanning = false // reset just in case
                    coroutineScope.launch {
                        delay(500) // wait for datastore
                        triggerRescan()
                    }
                } catch (e: Exception) {
                    Log.e("SpicyPlayer", "Error handling directory picker", e)
                    Toast.makeText(context, "Failed to set directory: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // ── Initial scan ──────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (songPairs.isEmpty() && !isScanning) {
            isScanning = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val scanPath = settingsVm.scanDirectory.value.ifBlank { "/sdcard/Music/" }
                    
                    // Attempt to load from cache first
                    val cachedPairs = com.tx24.spicyplayer.util.loadCachedScan(context, scanPath)
                    
                    if (cachedPairs != null) {
                        launch(Dispatchers.Main) {
                            songPairs = cachedPairs
                            isScanning = false
                        }
                    } else {
                        scanHistory = emptyList()
                        val scanPath = settingsVm.scanDirectory.value.ifBlank { "/sdcard/Music/" }
                        val intent = Intent(context, ScanService::class.java).apply {
                            putExtra("scan_path", scanPath)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        isScanning = true
                    }
                } catch (e: Exception) {
                    Log.e("SpicyPlayer", "Initial scan error", e)
                    launch(Dispatchers.Main) { isScanning = false }
                }
            }
        }
    }

    // ── Song loading helpers ──────────────────────────────────────────────
    val loadSongFromPair: (Pair<File, File?>, Int, Boolean, Boolean) -> Unit = { pair, indexInSongPairs, playWait, useCrossfade ->
        val (selectedAudio, selectedTtml) = pair
        loadingJob?.cancel()
        
        // Check cache first
        val cachedMetadata = metadataCache.get(selectedAudio.absolutePath)
        if (cachedMetadata != null) {
            trackName = cachedMetadata.t
            artistName = cachedMetadata.a
            coverArtBitmap = cachedMetadata.art
            currentBitrate = cachedMetadata.bitrate
            currentFormat = cachedMetadata.ext
            currentSongIndex = indexInSongPairs
            // We still need to load lyrics if any, but we can do it in background
        }

        loadingJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.d("SpicyPlayer", "Loading: ${selectedAudio.name}, playWait=$playWait")

                val parsedLyrics = try {
                    if (selectedTtml != null) {
                        selectedTtml.inputStream().use { TtmlLyricsParser.parse(it) }
                    } else null
                } catch (e: Exception) {
                    Log.e("SpicyPlayer", "TTML parse failed", e); null
                }

                val result = if (cachedMetadata != null) {
                    cachedMetadata
                } else {
                    try {
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
                                if (raw?.let { it.width > 1024 || it.height > 1024 } == true)
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
                }

                launch(Dispatchers.Main) {
                    isCrossfadingTriggered = false
                    lines = parsedLyrics?.lines ?: emptyList()
                    trackName = result.t
                    artistName = result.a
                    coverArtBitmap = result.art
                    currentBitrate = result.bitrate
                    currentFormat = result.ext
                    currentSongIndex = indexInSongPairs
                    
                    // Cache the metadata for future use
                    metadataCache.put(selectedAudio.absolutePath, result)
                    
                    if (crossfadeDuration > 0 && useCrossfade) {
                        audioPlayer.playNextWithCrossfade(listOf(selectedAudio.absolutePath), crossfadeDuration * 1000L, playWhenReady = playWait)
                    } else {
                        audioPlayer.preparePlaylist(listOf(selectedAudio.absolutePath), playWhenReady = playWait)
                    }
                    isPlaying = playWait
                }
            } catch (e: Exception) {
                Log.e("SpicyPlayer", "Load error", e)
            }
        }
    }

    val loadSong: (Int, Boolean, Boolean) -> Unit = { index, playWait, useCrossfade ->
        if (index in songPairs.indices) {
            loadSongFromPair(songPairs[index], index, playWait, useCrossfade)
        }
    }

    // ── Queue helpers ─────────────────────────────────────────────────────
    val loadNext: (Boolean) -> Unit = { useCrossfade ->
        Log.d("SpicyPlayer", "loadNext: current=$currentSongIndex, loopMode=$loopMode")
        when {
            loopMode == 2 -> {
                audioPlayer.player?.seekTo(0)
                audioPlayer.play()
                isPlaying = true
            }
            playQueue.isNotEmpty() -> {
                val next = if (loopMode == 1)
                    (playQueueIndex + 1) % playQueue.size
                else
                    (playQueueIndex + 1)
                
                if (next < playQueue.size) {
                    playQueueIndex = next
                    loadSongFromPair(playQueue[next], songPairs.indexOf(playQueue[next]), true, useCrossfade)
                } else {
                    isPlaying = false
                }
            }
            songPairs.isNotEmpty() -> {
                val next = (currentSongIndex + 1)
                if (next < songPairs.size) {
                    loadSong(next, true, useCrossfade)
                } else if (loopMode == 1) {
                    loadSong(0, true, useCrossfade)
                } else {
                    isPlaying = false
                }
            }
        }
    }

    val loadPrevious = {
        Log.d("SpicyPlayer", "loadPrevious: current=$currentSongIndex")
        if (currentTimeMs > backSkipThreshold * 1000L) {
            audioPlayer.player?.seekTo(0)
            currentTimeMs = 0L
        } else {
            when {
                playQueue.isNotEmpty() -> {
                    val prev = (playQueueIndex - 1 + playQueue.size) % playQueue.size
                    playQueueIndex = prev
                    loadSongFromPair(playQueue[prev], songPairs.indexOf(playQueue[prev]), true, false)
                }
                songPairs.isNotEmpty() -> {
                    val prev = (currentSongIndex - 1 + songPairs.size) % songPairs.size
                    loadSong(prev, true, false)
                }
            }
        }
    }

    val handleShuffle = {
        val shuffled = if (playQueue.isEmpty()) songPairs.shuffled() else playQueue.shuffled()
        if (shuffled.isNotEmpty()) {
            playQueue = shuffled
            playQueueIndex = 0
            loadSongFromPair(shuffled[0], songPairs.indexOf(shuffled[0]), true, false)
        }
    }

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
                        currentLoadNext(true)
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
        audioPlayer.addListener(listener)
        onDispose { audioPlayer.removeListener(listener) }
    }

    // ── Skip Callbacks ──────────────────────────────────────────────────
    LaunchedEffect(audioPlayer) {
        audioPlayer.onSkipNext = { loadNext(true) }
        audioPlayer.onSkipPrevious = { loadPrevious() }
    }

    // ── Playback time loop ────────────────────────────────────────────────
    LaunchedEffect(isPlaying, isDraggingSlider) {
        if (isPlaying && !isDraggingSlider) {
            currentTimeMs = audioPlayer.player?.currentPosition ?: 0L
            var lastTime = System.currentTimeMillis()
            while (isPlaying && !isDraggingSlider) {
                val now = System.currentTimeMillis()
                val dt = now - lastTime
                lastTime = now

                val dur = audioPlayer.player?.duration ?: 0L
                if (dur > 0) currentDurationMs = dur

                val actualPos = audioPlayer.player?.currentPosition ?: 0L
                val diff = actualPos - currentTimeMs
                if (kotlin.math.abs(diff) > 500) {
                    currentTimeMs = actualPos
                } else {
                    val correction = diff * 0.1f
                    currentTimeMs += dt + correction.toLong()
                }

                if (currentDurationMs > 0 && crossfadeDuration > 0) {
                    val remainingMs = currentDurationMs - actualPos
                    if (remainingMs > 0 && remainingMs <= crossfadeDuration * 1000L && !isCrossfadingTriggered && loadingJob?.isActive != true) {
                        isCrossfadingTriggered = true
                        currentLoadNext(true)
                    }
                }

                delay(16)
            }
        }
    }

    // ── colours ───────────────────────────────────────────────────────────
    val onSurface = Color.White
    val primaryTint = colorScheme.primary
    val disabledTint = onSurface.copy(alpha = 0.38f)

    // ── UI ────────────────────────────────────────────────────────────────
    MaterialTheme(colorScheme = colorScheme) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val duration = 400
                val enterSlide = when {
                    initialState == AppScreen.NOW_PLAYING && targetState == AppScreen.SETTINGS -> slideInHorizontally { it }
                    initialState == AppScreen.SETTINGS && targetState == AppScreen.EQUALIZER -> slideInHorizontally { it }
                    initialState == AppScreen.EQUALIZER && targetState == AppScreen.SETTINGS -> slideInHorizontally { -it / 3 }
                    initialState == AppScreen.SETTINGS && targetState == AppScreen.NOW_PLAYING -> slideInHorizontally { -it / 3 }
                    initialState == AppScreen.NOW_PLAYING && targetState == AppScreen.EQUALIZER -> slideInHorizontally { it }
                    initialState == AppScreen.EQUALIZER && targetState == AppScreen.NOW_PLAYING -> slideInHorizontally { -it / 3 }
                    else -> fadeIn()
                }
                val exitSlide = when {
                    initialState == AppScreen.NOW_PLAYING && targetState == AppScreen.SETTINGS -> slideOutHorizontally { -it / 3 }
                    initialState == AppScreen.SETTINGS && targetState == AppScreen.EQUALIZER -> slideOutHorizontally { -it / 3 }
                    initialState == AppScreen.EQUALIZER && targetState == AppScreen.SETTINGS -> slideOutHorizontally { it }
                    initialState == AppScreen.SETTINGS && targetState == AppScreen.NOW_PLAYING -> slideOutHorizontally { it }
                    initialState == AppScreen.NOW_PLAYING && targetState == AppScreen.EQUALIZER -> slideOutHorizontally { -it / 3 }
                    initialState == AppScreen.EQUALIZER && targetState == AppScreen.NOW_PLAYING -> slideOutHorizontally { it }
                    else -> fadeOut()
                }

                (enterSlide + fadeIn(tween(duration))).togetherWith(
                    exitSlide + fadeOut(tween(duration))
                ).apply {
                    targetContentZIndex = if (targetState.ordinal > initialState.ordinal) 1f else -1f
                }
            },
            modifier = Modifier.background(colorScheme.background),
            contentAlignment = Alignment.Center,
            label = "app_nav"
        ) { screen ->
            when (screen) {
                AppScreen.SETTINGS -> {
                    // ── Settings screen ──────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorScheme.background)
                    ) {
                        DynamicBackgroundView(
                            coverArtBitmap = coverArtBitmap ?: appLogo,
                            blurIntensity = blurIntensity
                        )
                        SettingsScreen(
                            onBack = { currentScreen = AppScreen.NOW_PLAYING },
                            onRescan = { triggerRescan() },
                            onNavigateToEqualizer = {
                                eqNavigationSource = AppScreen.SETTINGS
                                currentScreen = AppScreen.EQUALIZER
                            },
                            onDirectoryClick = {
                                val rootUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A")
                                directoryPickerLauncher.launch(rootUri)
                            },
                            isScanning = isScanning
                        )
                    }
                }
                AppScreen.EQUALIZER -> {
                    // ── Equalizer screen ──────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorScheme.background)
                    ) {
                        DynamicBackgroundView(
                            coverArtBitmap = coverArtBitmap ?: appLogo,
                            blurIntensity = blurIntensity
                        )
                        EqualizerScreen(
                            currentPreset = eqPreset,
                            onPresetChange = { settingsVm.setEqPreset(it) },
                            customBands = customEqBands,
                            onCustomBandsChange = { settingsVm.setCustomEqBands(it) },
                            onCustomBandsPreview = { if (eqPreset == "CUSTOM") audioPlayer.applyEqBands(it) },
                            bassBoostEnabled = bassBoostEnabled,
                            onBassBoostEnabledChange = { settingsVm.setBassBoost(it) },
                            bassBoostStrength = bassBoostStrength,
                            onBassBoostStrengthChange = { settingsVm.setBassBoostStrength(it) },
                            loudnessEnabled = loudnessEnabled,
                            onLoudnessEnabledChange = { settingsVm.setLoudnessEnabled(it) },
                            loudnessStrength = loudnessStrength,
                            onLoudnessStrengthChange = { settingsVm.setLoudnessStrength(it) },
                            onBack = { currentScreen = eqNavigationSource }
                        )
                    }
                }
                AppScreen.NOW_PLAYING -> {
                    // ── Now Playing ──────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorScheme.background)
                    ) {
                        DynamicBackgroundView(
                            coverArtBitmap = coverArtBitmap ?: appLogo,
                            blurIntensity = blurIntensity
                        )

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
                                        currentTimeMs = currentTimeMs + lyricsOffsetMs,
                                        onSeekWord = { timeMs ->
                                            audioPlayer.player?.seekTo(timeMs)
                                            currentTimeMs = timeMs
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        fontSizeScale = lyricsFontScale
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
                                onSeekFinished = { audioPlayer.player?.seekTo(currentTimeMs) },
                                isDraggingSlider = isDraggingSlider,
                                onDraggingChanged = { isDraggingSlider = it },
                                currentBitrate = currentBitrate,
                                currentFormat = currentFormat,
                                primaryTint = primaryTint,
                                onShowMenu = { showMenu = true },
                                onShuffle = { handleShuffle() },
                                isShuffleActive = playQueue.isNotEmpty(),
                                onPrevious = { loadPrevious() },
                                onNext = { loadNext(false) },
                                isPlaying = isPlaying,
                                onTogglePlay = {
                                    if (audioPlayer.player?.isPlaying == true) {
                                        audioPlayer.pause()
                                        isPlaying = false
                                    } else {
                                        audioPlayer.play()
                                        isPlaying = true
                                    }
                                },
                                loopMode = loopMode,
                                onToggleLoop = { loopMode = (loopMode + 1) % 3 },
                                colorScheme = colorScheme,
                                formatTime = { formatTime(it) },
                                isExpressive = controlsStyle == "EXPRESSIVE"
                            )

                        }
                    } 
                } 
            } 
        } 

        // ── Custom 3-Dots Menu Bottom Sheet ─────────────────────────────────
        AppMenuBottomSheet(
            showMenu = showMenu,
            onDismiss = { showMenu = false },
            colorScheme = colorScheme,
            onNavigateToEqualizer = {
                eqNavigationSource = AppScreen.NOW_PLAYING
                currentScreen = AppScreen.EQUALIZER
            },
            onNavigateToSettings = {
                currentScreen = AppScreen.SETTINGS
            }
        )

        // ── Scanning Progress Dialog ────────────────────────────────────────
        if (isScanning) {
            Dialog(
                onDismissRequest = { /* Prevent dismiss during scan */ }
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Library Synchronizing",
                                style = MaterialTheme.typography.headlineSmall,
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colorScheme.surfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                scanHistory.forEach { historyItem ->
                                    Text(
                                        text = historyItem,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                
                                // Current active phase
                                val currentText = buildString {
                                    append(scanProgress.phase)
                                    if (scanProgress.totalCount > 0) {
                                        append(" ${scanProgress.currentCount}/${scanProgress.totalCount}")
                                    } else if (scanProgress.currentCount > 0) {
                                        append(" ${scanProgress.currentCount}")
                                    }
                                }
                                
                                Text(
                                    text = currentText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Progress bar
                            if (scanProgress.totalCount > 0) {
                                LinearProgressIndicator(
                                    progress = { scanProgress.currentCount.toFloat() / scanProgress.totalCount.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                                    color = colorScheme.primary,
                                    trackColor = colorScheme.surfaceVariant
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = colorScheme.primary,
                                    trackColor = colorScheme.surfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "You can exit the app, it will continue scanning in the background",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            // Battery Optimization Prompt (always helpful to check)
                            val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
                            val isIgnoringBatteryOptimizations = remember(isScanning) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
                                } else {
                                    true
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (!isIgnoringBatteryOptimizations) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.errorContainer,
                                        contentColor = colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.BatteryAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unrestrict Background Sync", style = MaterialTheme.typography.labelLarge)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        try { context.startActivity(intent) } catch (e: Exception) {}
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp), tint = colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Check Background Settings", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
