package com.tx24.spicyplayer.lyrics

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import com.tx24.spicyplayer.model.lyrics.PlainLyrics
import com.tx24.spicyplayer.model.lyrics.SynchronizedLyrics
import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import com.tx24.spicyplayer.lyrics.spicy.canvas.SpicyLyricsView
import com.tx24.spicyplayer.lyrics.spicy.romanization.RomanizationService
import com.tx24.spicyplayer.lyrics.toSpicyLines
import com.tx24.spicyplayer.lyrics.toSpicyStaticParsed
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType
import com.tx24.spicyplayer.lyrics.spicy.models.ParsedLyrics
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsDocument
import com.tx24.spicyplayer.lyrics.spicy.models.buildDisplayTimeline
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import com.tx24.spicyplayer.lyrics.spicy.PlaybackClock
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableLongStateOf


@Composable
fun LiveLyricsScreen(
    modifier: Modifier,
    focusAnchorFraction: Float = 0.25f,
    controlsVisible: Boolean = true,
    lyricsViewModel: LiveLyricsViewModel = hiltViewModel()
) {

    val state by lyricsViewModel.state.collectAsState()
    LiveLyricsScreen(
        modifier = modifier,
        state,
        lyricsViewModel::songProgressMillis,
        lyricsViewModel::setSongProgressMillis,
        lyricsViewModel::onRetry,
        lyricsViewModel::isPlaying,
        lyricsViewModel::playbackSpeed,
        lyricsViewModel::songDurationMillis,
        focusAnchorFraction = focusAnchorFraction,
        controlsVisible = controlsVisible,
    )
}

@Composable
fun LiveLyricsScreen(
    modifier: Modifier,
    state: LyricsScreenState,
    songProgressMillis: () -> Long,
    onSeekToPositionMillis: (Long) -> Unit,
    onRetry: () -> Unit,
    isPlaying: () -> Boolean = { true },
    playbackSpeed: () -> Float = { 1f },
    songDurationMillis: () -> Long = { 0L },
    focusAnchorFraction: Float = 0.25f,
    controlsVisible: Boolean = true,
) {
    when (state) {
        is LyricsScreenState.NoLyrics ->
            NoLyricsState(modifier = modifier, reason = state.reason, onRetry)

        is LyricsScreenState.Loading, is LyricsScreenState.SearchingLyrics ->
            LoadingState(modifier = modifier)

        is LyricsScreenState.NotPlaying ->
            NotPlayingState(modifier = modifier)

        is LyricsScreenState.Ready ->
            LyricsDocumentState(
                modifier = modifier,
                document = state.document,
                onSeekToPositionMillis = onSeekToPositionMillis,
                songProgressMillis = songProgressMillis,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                songDurationMillis = songDurationMillis,
                focusAnchorFraction = focusAnchorFraction,
                controlsVisible = controlsVisible,
            )
    }
}

@Composable
fun LyricLine(
    modifier: Modifier,
    line: String,
    isCurrentLine: Boolean = false,
    isShowingContextMenu: Boolean = false,
    onDismissContextMenu: () -> Unit = {}
) {

    val context = LocalContext.current
    val localClipboardManager = LocalClipboardManager.current
    Box(modifier = modifier) {
        if (isShowingContextMenu) {
            Popup(
                popupPositionProvider = ContextMenuPopupProvider(),
                onDismissRequest = onDismissContextMenu
            ) {
                LineContextMenu(
                    Modifier
                        .width(IntrinsicSize.Max)
                        .height(IntrinsicSize.Max),
                    onCopy = {
                        localClipboardManager.setText(AnnotatedString(line))
                        onDismissContextMenu()
                    },
                    onShare = {
                        context.shareText(line)
                        onDismissContextMenu()
                    }
                )
            }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = if (isCurrentLine || isShowingContextMenu) 1.0f else 0.35f
                }
                .then(if (isShowingContextMenu) Modifier.shimmerLoadingAnimation() else Modifier),
            text = line,
            fontSize = when (com.tx24.spicyplayer.ui.common.LocalUserPreferences.current.uiSettings.lyricsFontSize) {
                "SMALL" -> 20.sp
                "LARGE" -> 30.sp
                else -> 25.sp
            },
            fontWeight = FontWeight.ExtraBold
        )
    }
}

fun Modifier.fadingEdge(brush: Brush) =
    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.DstIn)
        }


@Composable
fun NoLyricsState(
    modifier: Modifier,
    reason: NoLyricsReason,
    onRetry: () -> Unit,
) {
    when (reason) {
        NoLyricsReason.NOT_FOUND -> {
            Box(modifier = modifier) {
                Text(modifier = Modifier.align(Alignment.Center), text = "No lyrics available")
            }
        }

        NoLyricsReason.NETWORK_ERROR -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Check your network connection")
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onRetry) {
                    Text(text = "Try Again")
                }
            }
        }
    }
}

@Composable
fun LoadingState(
    modifier: Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun NotPlayingState(
    modifier: Modifier
) {
    Box(modifier = modifier) {
        Text(modifier = Modifier.align(Alignment.Center), text = "No song is being played.")
    }
}

@Composable
fun PlainLyricsState(
    modifier: Modifier,
    plainLyrics: PlainLyrics
) {
    val itemsSpacing = 12.dp

    var contextMenuShownIndex by remember {
        mutableStateOf(-1)
    }

    val vibrationManager = LocalHapticFeedback.current
    LazyColumn(
        modifier
    ) {
        itemsIndexed(plainLyrics.lines) { index, s ->
            LyricLine(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                contextMenuShownIndex = index
                                vibrationManager.performHapticFeedback(HapticFeedbackType.LongPress)
                            }) { }
                    },
                line = s,
                isCurrentLine = true,
                isShowingContextMenu = index == contextMenuShownIndex,
                onDismissContextMenu = { contextMenuShownIndex = -1 }
            )
            Spacer(modifier = Modifier.height(itemsSpacing))
        }
    }
}

@Composable
fun SyncedLyricsState(
    modifier: Modifier,
    synchronizedLyrics: SynchronizedLyrics,
    onSeekToPositionMillis: (Long) -> Unit,
    songProgressMillis: () -> Long,
    isPlaying: () -> Boolean = { true },
    focusAnchorFraction: Float = 0.25f,
    controlsVisible: Boolean = true,
) {
    val spicyLines = remember(synchronizedLyrics) {
        synchronizedLyrics.toSpicyLines()
    }
    SpicyLyricsPlayer(
        modifier, spicyLines, LyricsType.Line, onSeekToPositionMillis, songProgressMillis, isPlaying,
        focusAnchorFraction = focusAnchorFraction, controlsVisible = controlsVisible,
    )
}

@Composable
fun StaticLyricsState(
    modifier: Modifier,
    plainLyrics: PlainLyrics,
    onSeekToPositionMillis: (Long) -> Unit,
    songProgressMillis: () -> Long,
    isPlaying: () -> Boolean = { true },
    focusAnchorFraction: Float = 0.25f,
    controlsVisible: Boolean = true,
) {
    val staticLines = remember(plainLyrics) { plainLyrics.toSpicyStaticParsed().lines }
    SpicyLyricsPlayer(
        modifier, staticLines, LyricsType.Static, onSeekToPositionMillis, songProgressMillis, isPlaying,
        focusAnchorFraction = focusAnchorFraction, controlsVisible = controlsVisible,
    )
}

@Composable
fun TtmlLyricsState(
    modifier: Modifier,
    parsedLyrics: ParsedLyrics,
    onSeekToPositionMillis: (Long) -> Unit,
    songProgressMillis: () -> Long,
    isPlaying: () -> Boolean = { true },
    focusAnchorFraction: Float = 0.25f,
    controlsVisible: Boolean = true,
) {
    SpicyLyricsPlayer(modifier, parsedLyrics.lines, parsedLyrics.type, onSeekToPositionMillis, songProgressMillis, isPlaying,
        documentId = parsedLyrics.documentId, focusAnchorFraction = focusAnchorFraction, controlsVisible = controlsVisible)
}

@Composable
fun LyricsDocumentState(
    modifier: Modifier,
    document: LyricsDocument,
    onSeekToPositionMillis: (Long) -> Unit,
    songProgressMillis: () -> Long,
    isPlaying: () -> Boolean,
    playbackSpeed: () -> Float,
    songDurationMillis: () -> Long,
    focusAnchorFraction: Float,
    controlsVisible: Boolean,
) {
    SpicyLyricsPlayer(
        modifier, document.lines, document.type, onSeekToPositionMillis, songProgressMillis, isPlaying,
        playbackSpeed, songDurationMillis, document.documentId, focusAnchorFraction, controlsVisible,
    )
}

/**
 * Shared karaoke lyrics surface for both the TTML and synced (.lrc) sources —
 * they differ only in how the [lines] are produced. Polls the playback position
 * into the animated [SpicyLyricsView].
 */
@Composable
private fun SpicyLyricsPlayer(
    modifier: Modifier,
    lines: List<Line>,
    lyricsType: LyricsType,
    onSeekToPositionMillis: (Long) -> Unit,
    songProgressMillis: () -> Long,
    isPlaying: () -> Boolean = { true },
    playbackSpeed: () -> Float = { 1f },
    songDurationMillis: () -> Long = { 0L },
    documentId: String = "",
    focusAnchorFraction: Float = 0.25f,
    controlsVisible: Boolean = true,
) {
    var currentTimeMs by remember { mutableLongStateOf(0L) }

    val uiSettings = com.tx24.spicyplayer.ui.common.LocalUserPreferences.current.uiSettings
    val lyricsOffsetMs = uiSettings.lyricsOffsetMs
    val fontSizeScale = when (uiSettings.lyricsFontSize) {
        "SMALL" -> 0.85f
        "LARGE" -> 1.15f
        else -> 1.0f
    }
    val renderConfig = remember(uiSettings.lyricsQualityMode) {
        RenderConfig.forModeName(uiSettings.lyricsQualityMode)
    }
    val timelineLines = remember(lines, renderConfig.mode) {
        buildDisplayTimeline(lines, minimalMode = renderConfig.mode.name == "MINIMAL")
    }

    // Populate romanization off the main thread; TTML-supplied romanization is preserved.
    val romanizedLines by produceState(initialValue = timelineLines, timelineLines) {
        value = RomanizationService.romanize(timelineLines)
    }
    val hasRomanization = remember(romanizedLines) {
        romanizedLines.any { line -> line.words.any { it.romanizedText != null } }
    }
    // In-view toggle, seeded from the default setting.
    var romanizeEnabled by remember(documentId) { mutableStateOf(uiSettings.lyricsRomanize) }
    val romanize = romanizeEnabled && hasRomanization

    // Frame-synced predicted playback clock (port of the reference's GetProgress pipeline):
    // the player position is sampled every frame, extrapolated on wall-clock time, and
    // jitter-smoothed, so the karaoke sweep advances continuously instead of stepping with
    // the controller's coarse position updates. The +100ms forward lead lives in the clock.
    val playbackClock = remember { PlaybackClock() }
    LaunchedEffect(documentId) {
        playbackClock.reset()
    }
    // Diagnostic jump-detection state, reset alongside the clock so a song swap doesn't log a
    // false jump against the previous song's last sample.
    val clockJumpTracker = remember(documentId) { ClockJumpTracker() }

    // Folded into SpicyLyricsView's own withFrameNanos loop via onFrameTick instead of running a
    // second, independent frame loop here — halves the Choreographer callbacks registered while a
    // lyrics screen is on-screen. Recreated on the same keys the old loop restarted on.
    val onFrameTick = remember(documentId, lyricsOffsetMs) {
        { frameNanos: Long ->
            val measured = songProgressMillis()
            val playing = isPlaying()
            val smoothed = playbackClock.positionMs(
                measuredMs = measured,
                isPlaying = playing,
                playbackSpeed = playbackSpeed(),
                nowMs = frameNanos / 1_000_000L,
                durationMs = songDurationMillis(),
            )
            // Diagnostic: any frame-to-frame jump beyond ~4 frames means the sweep teleports.
            if (clockJumpTracker.lastOut != Long.MIN_VALUE && playing && clockJumpTracker.lastPlaying &&
                kotlin.math.abs(smoothed - clockJumpTracker.lastOut) > 64L
            ) {
                timber.log.Timber.tag("SpicyClock").d(
                    "JUMP out %d -> %d (Δ%d) measured=%d",
                    clockJumpTracker.lastOut, smoothed, smoothed - clockJumpTracker.lastOut, measured,
                )
            }
            clockJumpTracker.lastOut = smoothed
            clockJumpTracker.lastPlaying = playing
            currentTimeMs = smoothed - lyricsOffsetMs
        }
    }

    Box(modifier.fillMaxSize()) {
        SpicyLyricsView(
            lines = romanizedLines,
            documentId = documentId,
            // Pass the clock as a provider rather than a value: reading it here would recompose
            // this composable every frame. The view invokes it inside its own frame loop instead.
            currentTimeMs = { currentTimeMs },
            onSeekWord = remember(lyricsOffsetMs, onSeekToPositionMillis) {
                { seekTarget: Long -> onSeekToPositionMillis(seekTarget + lyricsOffsetMs - 100L) }
            },
            modifier = Modifier.fillMaxSize(),
            fontSizeScale = fontSizeScale,
            config = renderConfig,
            lyricsType = lyricsType,
            romanize = romanize,
            focusAnchorFraction = focusAnchorFraction,
            onFrameTick = onFrameTick,
        )

        // In-view romanization toggle: fades with the rest of the screen's controls
        // (caller-driven — see LandscapePlayerScreen/PortraitPlayerScreen's idle timer).
        AnimatedVisibility(
            visible = hasRomanization && controlsVisible,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Surface(
                onClick = { romanizeEnabled = !romanizeEnabled },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = if (romanize) androidx.compose.material3.MaterialTheme.colorScheme.primary
                    else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "あ→A",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = if (romanize) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                        else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Jump-detection state for [SpicyLyricsPlayer]'s onFrameTick, held across frames via `remember`. */
private class ClockJumpTracker {
    var lastOut: Long = Long.MIN_VALUE
    var lastPlaying: Boolean = false
}


