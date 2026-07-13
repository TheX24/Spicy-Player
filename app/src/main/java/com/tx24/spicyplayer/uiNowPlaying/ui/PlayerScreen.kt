package com.tx24.spicyplayer.uiNowPlaying.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.ColorInt
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.tx24.spicyplayer.model.playback.PlayerState
import com.tx24.spicyplayer.model.playback.RepeatMode
import com.tx24.spicyplayer.library.store.model.song.Song
import com.tx24.spicyplayer.ui.albumart.LocalInefficientThumbnailImageLoader
import com.tx24.spicyplayer.ui.albumart.SongAlbumArtModel
import com.tx24.spicyplayer.ui.albumart.toSongAlbumArtModel
import com.tx24.spicyplayer.ui.common.toInt
import com.tx24.spicyplayer.lyrics.LiveLyricsScreen
import com.tx24.spicyplayer.lyrics.fadingEdge
import com.tx24.spicyplayer.uiNowPlaying.viewmodel.INowPlayingViewModel

/**
 * Fires [onTouch] on the very first pointer contact, before any descendant gesture (word-seek,
 * drag-to-scroll, button press, ...) has a chance to consume it — so it catches every touch
 * anywhere in the subtree, not just a clean unconsumed tap.
 */
private fun Modifier.detectAnyTouch(onTouch: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onTouch()
    }
}

@Composable
fun PlayingScreen2(
    modifier: Modifier,
    songs: List<Song>,
    songIndex: Int,
    song: Song,
    repeatMode: RepeatMode,
    isShuffleOn: Boolean,
    playbackState: PlayerState,
    isShowingLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    screenSize: NowPlayingScreenSize,
    nowPlayingActions: INowPlayingViewModel,
    onOpenQueue: () -> Unit = {},
    onCollapse: () -> Unit = {}
) {

    when (screenSize) {
        NowPlayingScreenSize.COMPACT -> {
            CompactPlayerScreen(
                modifier = modifier,
                song = song,
                playbackState = playbackState,
                repeatMode = repeatMode,
                isShuffleOn = isShuffleOn,
                isShowingLyrics = isShowingLyrics,
                nowPlayingActions = nowPlayingActions,
                onOpenQueue = onOpenQueue,
                onToggleLyrics = onToggleLyrics,
                onCollapse = onCollapse
            )
        }

        NowPlayingScreenSize.PORTRAIT -> {
            PortraitPlayerScreen(
                modifier = modifier,
                songs = songs,
                songIndex = songIndex,
                playbackState = playbackState,
                repeatMode = repeatMode,
                isShuffleOn = isShuffleOn,
                isShowingLyrics = isShowingLyrics,
                nowPlayingActions = nowPlayingActions,
                onOpenQueue = onOpenQueue,
                onToggleLyrics = onToggleLyrics,
                onCollapse = onCollapse
            )
        }

        NowPlayingScreenSize.LANDSCAPE -> {
            LandscapePlayerScreen(
                modifier = modifier,
                songs = songs,
                songIndex = songIndex,
                playbackState = playbackState,
                repeatMode = repeatMode,
                isShuffleOn = isShuffleOn,
                isShowingLyrics = isShowingLyrics,
                nowPlayingActions = nowPlayingActions,
                onOpenQueue = onOpenQueue,
                onToggleLyrics = onToggleLyrics,
                onCollapse = onCollapse
            )
        }
    }

}

@Composable
fun CompactPlayerScreen(
    modifier: Modifier,
    song: Song,
    playbackState: PlayerState,
    repeatMode: RepeatMode,
    isShuffleOn: Boolean,
    isShowingLyrics: Boolean,
    nowPlayingActions: INowPlayingViewModel,
    onOpenQueue: () -> Unit,
    onToggleLyrics: () -> Unit,
    onCollapse: () -> Unit
) {

    var controlsCollapsed by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SongTextInfo(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            song = song,
            showArtist = false,
            showAlbum = false
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = { controlsCollapsed = !controlsCollapsed }) {
                        Icon(
                            imageVector = if (controlsCollapsed) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Collapse"
                        )
                    }

                    SongProgressInfo(
                        modifier = Modifier.weight(1f),
                        songDuration = song.metadata.durationMillis,
                        song = song,
                        isCollapsed = controlsCollapsed,
                        songProgressProvider = nowPlayingActions::currentSongProgress,
                        onUserSeek = nowPlayingActions::onUserSeek
                    )

                    IconButton(onClick = onToggleLyrics) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Lyrics",
                            modifier = if (isShowingLyrics) Modifier else Modifier.alpha(0.5f)
                        )
                    }
                }

                AnimatedVisibility(visible = !controlsCollapsed) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SongControls(
                            modifier = Modifier.fillMaxWidth(),
                            isPlaying = playbackState == PlayerState.PLAYING,
                            isShuffleOn = isShuffleOn,
                            repeatMode = repeatMode,
                            playButtonColor = MaterialTheme.colorScheme.primary,
                            onPrevious = nowPlayingActions::previousSong,
                            onTogglePlayback = nowPlayingActions::togglePlayback,
                            onNext = nowPlayingActions::nextSong,
                            onToggleShuffle = nowPlayingActions::toggleShuffleMode,
                            onToggleRepeat = nowPlayingActions::toggleRepeatMode
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                        ) {
                            SuggestionChip(
                                onClick = onOpenQueue,
                                label = { Text("Queue", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) },
                                icon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                shape = CircleShape,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )

                            NowPlayingOverflowChip(options = rememberNowPlayingOptions(songUi = song))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun nowPlayingScreenTint(songAlbumArtModel: SongAlbumArtModel): Color {
    val imageLoader = LocalInefficientThumbnailImageLoader.current
    val context = LocalContext.current

    val defaultColor = LocalContentColor.current
    val color = remember { Animatable(defaultColor) }

    LaunchedEffect(songAlbumArtModel) {
        val result = imageLoader.execute(
            ImageRequest.Builder(context)
                .allowHardware(false)
                .size(240, 240)
                .data(songAlbumArtModel)
                .build()
        )

        val bitmap = result.drawable?.toBitmap()
        if (bitmap == null) {
            color.animateTo(defaultColor)
            return@LaunchedEffect
        }

        // Palette quantization is CPU-heavy; keep it off the main thread so a
        // track change doesn't drop frames.
        val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }

        // Try better swatches with fallback order
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch

        val baseColor = swatch?.let { Color(it.rgb) } ?: defaultColor
        val improvedColor = improveColor(baseColor)

        color.animateTo(improvedColor)
    }

    return color.value
}

fun improveColor(color: Color): Color {
    val intColor = color.toArgb()

    return if (!isColorTooDark(intColor) && !isColorTooUnsaturated(intColor)) {
        color
    } else {
        val lightened = lightenColor(intColor, 0.3f)
        if (!isColorTooDark(lightened.toArgb())) {
            lightened
        } else {
            invertColor(lightened)
        }
    }
}

// Brightness check using perceived brightness
fun isColorTooDark(@ColorInt color: Int): Boolean {
    val r = android.graphics.Color.red(color)
    val g = android.graphics.Color.green(color)
    val b = android.graphics.Color.blue(color)
    val brightness = (r * 299 + g * 587 + b * 114) / 1000
    return brightness < 100 // Tune threshold
}

// Optional: Skip very desaturated (grey-ish) colors
fun isColorTooUnsaturated(@ColorInt color: Int): Boolean {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color, hsl)
    return hsl[1] < 0.15f // saturation below 15%
}

// Lighten color using HSL
fun lightenColor(@ColorInt color: Int, amount: Float = 0.2f): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color, hsl)
    hsl[2] = (hsl[2] + amount).coerceAtMost(1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

// Simple invert
fun invertColor(color: Color): Color =
    Color(1f - color.red, 1f - color.green, 1f - color.blue, color.alpha)


@Composable
fun PortraitPlayerScreen(
    modifier: Modifier,
    songs: List<Song>,
    songIndex: Int,
    playbackState: PlayerState,
    repeatMode: RepeatMode,
    isShuffleOn: Boolean,
    isShowingLyrics: Boolean,
    nowPlayingActions: INowPlayingViewModel,
    onOpenQueue: () -> Unit,
    onToggleLyrics: () -> Unit,
    onCollapse: () -> Unit
) {

    var controlsCollapsed by remember {
        mutableStateOf(false)
    }

    val song = remember(songs, songIndex) { songs[songIndex] }

    // Idle-fade for the bottom controls card, mirroring Spicy Lyrics' fullscreen chrome —
    // fades out after a few seconds of inactivity, any tap on the screen brings it back.
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    Column(
        modifier.detectAnyTouch { controlsVisible = true },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val lyricsWeight by animateFloatAsState(
            targetValue = if (isShowingLyrics) 1f else 0.001f,
            animationSpec = tween(800, easing = LinearOutSlowInEasing),
            label = "LyricsWeight"
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top
        ) {
            val headerBoxModifier = if (!isShowingLyrics) Modifier.weight(1f) else Modifier.wrapContentHeight()
            val headerVerticalBias by animateFloatAsState(
                targetValue = if (isShowingLyrics) -1f else -0.2f,
                animationSpec = tween(800, easing = LinearOutSlowInEasing),
                label = "HeaderVerticalBias"
            )
            Box(
                modifier = Modifier.then(headerBoxModifier),
                contentAlignment = BiasAlignment(0f, headerVerticalBias)
            ) {
                NowPlayingHeader(
                    songs = songs,
                    songIndex = songIndex,
                    isShowingLyrics = isShowingLyrics,
                    onSongSwitched = { newIndex ->
                        if (newIndex != songIndex)
                            nowPlayingActions.playSongAtIndex(newIndex)
                    },
                    onDoubleTapArt = nowPlayingActions::togglePlayback
                )
            }

            Box(modifier = Modifier.weight(lyricsWeight)) {
                if (isShowingLyrics) {
                    val context = LocalContext.current as Activity
                    val keepScreenOn = com.tx24.spicyplayer.ui.common.LocalUserPreferences.current.uiSettings.keepScreenOn
                    DisposableEffect(key1 = Unit) {
                        context.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        onDispose { 
                            if (!keepScreenOn) {
                                context.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) 
                            }
                        }
                    }
                    val fadeBrush = remember {
                        Brush.verticalGradient(
                            0.0f to Color.Red,
                            0.7f to Color.Red,
                            1.0f to Color.Transparent
                        )
                    }
                    LiveLyricsScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .fadingEdge(fadeBrush)
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        controlsVisible = controlsVisible,
                    )
                    BackHandler {
                        onToggleLyrics()
                    }
                }
            }
        }

        val contentColor = nowPlayingScreenTint(songAlbumArtModel = song.toSongAlbumArtModel())

        // Only the minimized (collapsed) bar idle-fades — full controls stay put, matching
        // landscape where only the pill (the minimized affordance) fades, not the maximized card.
        AnimatedVisibility(
            visible = !controlsCollapsed || controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Surface(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        IconButton(onClick = { controlsCollapsed = !controlsCollapsed }) {
                            Icon(
                                imageVector = if (controlsCollapsed) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Collapse"
                            )
                        }

                        SongProgressInfo(
                            modifier = Modifier.weight(1f),
                            songDuration = song.metadata.durationMillis,
                            tint = contentColor,
                            song = song,
                            isCollapsed = controlsCollapsed,
                            songProgressProvider = nowPlayingActions::currentSongProgress,
                            onUserSeek = nowPlayingActions::onUserSeek
                        )

                        IconButton(onClick = onToggleLyrics) {
                            Icon(
                                imageVector = Icons.Rounded.Lyrics,
                                contentDescription = "Lyrics",
                                modifier = if (isShowingLyrics) Modifier else Modifier.alpha(0.5f)
                            )
                        }
                    }

                    AnimatedVisibility(visible = !controlsCollapsed) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SongControls(
                                modifier = Modifier.fillMaxWidth(),
                                isPlaying = playbackState == PlayerState.PLAYING,
                                isShuffleOn = isShuffleOn,
                                repeatMode = repeatMode,
                                playButtonColor = contentColor,
                                onPrevious = nowPlayingActions::previousSong,
                                onTogglePlayback = nowPlayingActions::togglePlayback,
                                onNext = nowPlayingActions::nextSong,
                                onToggleShuffle = nowPlayingActions::toggleShuffleMode,
                                onToggleRepeat = nowPlayingActions::toggleRepeatMode
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = onOpenQueue,
                                    label = { Text("Queue", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) },
                                    icon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    shape = CircleShape,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )

                                TechnicalMetadataChip(song = song)

                                NowPlayingOverflowChip(options = rememberNowPlayingOptions(songUi = song))
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun TechnicalMetadataChip(song: Song) {
    var metadata by remember(song.filePath) { mutableStateOf<NowPlayingMetadata?>(null) }
    LaunchedEffect(song.filePath) {
        metadata = NowPlayingMetadataCache.getMetadata(song)
    }

    val currentFormat = metadata?.format ?: song.filePath.substringAfterLast('.', "").uppercase()
    val bitrate = metadata?.bitrate ?: if (song.metadata.durationMillis > 0) {
        "${(song.metadata.sizeBytes / song.metadata.durationMillis) * 8} kbps"
    } else ""
    val sampleRate = metadata?.sampleRate ?: ""

    if (currentFormat.isNotEmpty() || bitrate.isNotEmpty() || sampleRate.isNotEmpty()) {
        Text(
            text = listOfNotNull(
                currentFormat.takeIf { it.isNotEmpty() },
                bitrate.replace(" kbps", "").takeIf { it.isNotEmpty() },
                sampleRate.takeIf { it.isNotEmpty() }
            ).joinToString(" • "),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}


@Composable
fun LandscapePlayerScreen(
    modifier: Modifier,
    songs: List<Song>,
    songIndex: Int,
    playbackState: PlayerState,
    repeatMode: RepeatMode,
    isShuffleOn: Boolean,
    isShowingLyrics: Boolean,
    nowPlayingActions: INowPlayingViewModel,
    onOpenQueue: () -> Unit,
    onToggleLyrics: () -> Unit,
    onCollapse: () -> Unit
) {

    val song = remember(songs, songIndex) { songs[songIndex] }

    // Minimizing the controls card is landscape-only UI state (mirrors Spicy Lyrics'
    // NowBar/lyrics split); it doesn't need to persist across sessions or screens.
    var controlsMinimized by remember {
        mutableStateOf(false)
    }

    // Idle-fade for the pill, mirroring Spicy Lyrics' fullscreen chrome — fades out after
    // a few seconds of inactivity, any tap on the screen brings it back.
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    BackHandler(enabled = isShowingLyrics) {
        onToggleLyrics()
    }

    BoxWithConstraints(
        modifier = modifier.detectAnyTouch { controlsVisible = true }
    ) {
        val artworkSize = minOf(maxWidth * 0.40f, maxHeight * 0.75f)
        val rightSpacing = artworkSize / 4
        val nowBarLeftX = maxWidth * 0.5f - artworkSize - rightSpacing
        val nowBarCenteredX = (maxWidth - artworkSize) / 2
        // Lyrics pane starts right where the NowBar column actually ends (plus a small
        // breathing gap), not at an abstract screen-relative fraction — so it stays flush
        // against the artwork regardless of how artworkSize is tuned.
        val rightPaneStart = nowBarLeftX + artworkSize + 24.dp
        val rightPaneWidth = maxWidth - rightPaneStart

        val nowBarCentered = controlsMinimized && !isShowingLyrics

        val nowBarX by animateDpAsState(
            targetValue = if (nowBarCentered) nowBarCenteredX else nowBarLeftX,
            animationSpec = tween(400),
            label = "NowBarX"
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = nowBarX)
                .width(artworkSize),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(32.dp, shape = RoundedCornerShape(12.dp), clip = true)
            ) {
                AlbumArtPager(
                    modifier = Modifier.fillMaxSize(),
                    songs = songs,
                    currentSongIndex = songIndex,
                    onSongSwitched = { newIndex ->
                        if (newIndex != songIndex) nowPlayingActions.playSongAtIndex(newIndex)
                    },
                    onDoubleTapArt = nowPlayingActions::togglePlayback
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SongTextInfo(
                modifier = Modifier.fillMaxWidth(),
                song = song,
                showAlbum = false,
                // Match NowPlayingHeader's marquee title/artist in portrait — the compact-state
                // sizes, since landscape's NowBar column is as narrow as portrait's small header.
                titleStyle = MaterialTheme.typography.headlineMedium,
                titleFontWeight = FontWeight.Bold,
                titleFontSize = TextUnit.Unspecified,
                titleColor = Color.White,
                artistStyle = MaterialTheme.typography.titleSmall,
                artistFontWeight = FontWeight.Normal,
                artistFontSize = TextUnit.Unspecified,
                artistColor = Color.White.copy(alpha = 0.7f),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = rightPaneStart)
                .width(rightPaneWidth)
                .fillMaxHeight()
        ) {
            // Reserve a strip on the right for the pill so it never overlaps the
            // controls card or the lyrics fading edge.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                // No size modifier here on purpose: AnimatedContent follows its current
                // content's own measured size (the card wraps, lyrics fill via their own
                // fillMaxSize modifier) — forcing fillMaxSize here would stretch the card too.
                AnimatedContent(
                    targetState = controlsMinimized to isShowingLyrics,
                    label = "LandscapeRightPane"
                ) { (minimized, lyricsOn) ->
                    when {
                        !minimized -> {
                            LandscapeControlsCard(
                                modifier = Modifier.fillMaxWidth(),
                                song = song,
                                playbackState = playbackState,
                                repeatMode = repeatMode,
                                isShuffleOn = isShuffleOn,
                                nowPlayingActions = nowPlayingActions,
                                onOpenQueue = onOpenQueue
                            )
                        }

                        lyricsOn -> {
                            val context = LocalContext.current as Activity
                            val keepScreenOn = com.tx24.spicyplayer.ui.common.LocalUserPreferences.current.uiSettings.keepScreenOn
                            DisposableEffect(key1 = Unit) {
                                context.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                onDispose {
                                    if (!keepScreenOn) {
                                        context.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    }
                                }
                            }
                            val fadeBrush = remember {
                                Brush.verticalGradient(
                                    0.0f to Color.Red,
                                    0.7f to Color.Red,
                                    1.0f to Color.Transparent
                                )
                            }
                            LiveLyricsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .fadingEdge(fadeBrush)
                                    .padding(vertical = 4.dp),
                                // Landscape dedicates the whole pane to lyrics (no header/card
                                // stacked above it), so anchor the active line near mid-height
                                // instead of the ~25%-from-top used in the shorter portrait pane.
                                focusAnchorFraction = 0.45f,
                                controlsVisible = controlsVisible,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 8.dp, end = 8.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = { controlsMinimized = !controlsMinimized }
                        ) {
                            Icon(
                                imageVector = if (controlsMinimized) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (controlsMinimized) "Show controls" else "Minimize controls"
                            )
                        }

                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = {
                                if (!isShowingLyrics) controlsMinimized = true
                                onToggleLyrics()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lyrics,
                                contentDescription = "Lyrics",
                                modifier = if (isShowingLyrics) Modifier else Modifier.alpha(0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeControlsCard(
    modifier: Modifier,
    song: Song,
    playbackState: PlayerState,
    repeatMode: RepeatMode,
    isShuffleOn: Boolean,
    nowPlayingActions: INowPlayingViewModel,
    onOpenQueue: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SongProgressInfo(
                modifier = Modifier.fillMaxWidth(),
                songDuration = song.metadata.durationMillis,
                song = song,
                isCollapsed = false,
                songProgressProvider = nowPlayingActions::currentSongProgress,
                onUserSeek = nowPlayingActions::onUserSeek
            )

            SongControls(
                modifier = Modifier.fillMaxWidth(),
                isPlaying = playbackState == PlayerState.PLAYING,
                isShuffleOn = isShuffleOn,
                repeatMode = repeatMode,
                playButtonColor = MaterialTheme.colorScheme.primary,
                onPrevious = nowPlayingActions::previousSong,
                onTogglePlayback = nowPlayingActions::togglePlayback,
                onNext = nowPlayingActions::nextSong,
                onToggleShuffle = nowPlayingActions::toggleShuffleMode,
                onToggleRepeat = nowPlayingActions::toggleRepeatMode
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = onOpenQueue,
                    label = { Text("Queue", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) },
                    icon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    shape = CircleShape,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                TechnicalMetadataChip(song = song)

                NowPlayingOverflowChip(options = rememberNowPlayingOptions(songUi = song))
            }
        }
    }
}