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
import androidx.compose.runtime.rememberUpdatedState
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
import com.tx24.spicyplayer.model.lyrics.LyricsFetchSource
import com.tx24.spicyplayer.model.lyrics.PlainLyrics
import com.tx24.spicyplayer.model.lyrics.SynchronizedLyrics
import com.tx24.spicyplayer.uiNowPlaying.spicy.canvas.SpicyLyricsView
import com.tx24.spicyplayer.lyrics.toSpicyLines
import com.tx24.spicyplayer.uiNowPlaying.spicy.models.ParsedLyrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableLongStateOf


@Composable
fun LiveLyricsScreen(
    modifier: Modifier,
    lyricsViewModel: LiveLyricsViewModel = hiltViewModel()
) {

    val state by lyricsViewModel.state.collectAsState()
    LiveLyricsScreen(
        modifier = modifier,
        state,
        lyricsViewModel::songProgressMillis,
        lyricsViewModel::setSongProgressMillis,
        lyricsViewModel::onRetry
    )
}

@Composable
fun LiveLyricsScreen(
    modifier: Modifier,
    state: LyricsScreenState,
    songProgressMillis: () -> Long,
    onSeekToPositionMillis: (Long) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is LyricsScreenState.NoLyrics ->
            NoLyricsState(modifier = modifier, reason = state.reason, onRetry)

        is LyricsScreenState.Loading, is LyricsScreenState.SearchingLyrics ->
            LoadingState(modifier = modifier)

        is LyricsScreenState.NotPlaying ->
            NotPlayingState(modifier = modifier)

        is LyricsScreenState.TextLyrics ->
            PlainLyricsState(modifier = modifier, plainLyrics = state.plainLyrics)

        is LyricsScreenState.SyncedLyrics ->
            SyncedLyricsState(
                modifier = modifier,
                synchronizedLyrics = state.syncedLyrics,
                lyricsFetchSource = state.lyricsSource,
                onSeekToPositionMillis = onSeekToPositionMillis,
                songProgressMillis = songProgressMillis
            )

        is LyricsScreenState.TtmlLyrics ->
            TtmlLyricsState(
                modifier = modifier,
                parsedLyrics = state.parsedLyrics,
                lyricsFetchSource = state.lyricsSource,
                onSeekToPositionMillis = onSeekToPositionMillis,
                songProgressMillis = songProgressMillis
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
    lyricsFetchSource: LyricsFetchSource,
    onSeekToPositionMillis: (Long) -> Unit,
    songProgressMillis: () -> Long
) {
    val spicyLines = remember(synchronizedLyrics) {
        synchronizedLyrics.toSpicyLines()
    }

    var currentTimeMs by remember { mutableLongStateOf(0L) }

    val uiSettings = com.tx24.spicyplayer.ui.common.LocalUserPreferences.current.uiSettings
    val lyricsOffsetMs = uiSettings.lyricsOffsetMs
    val fontSizeScale = when (uiSettings.lyricsFontSize) {
        "SMALL" -> 0.85f
        "LARGE" -> 1.15f
        else -> 1.0f
    }

    LaunchedEffect(synchronizedLyrics, lyricsOffsetMs) {
        while (isActive) {
            currentTimeMs = songProgressMillis() + lyricsOffsetMs
            delay(16)
        }
    }

    var actionsShown by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(key1 = actionsShown) {
        if (actionsShown) {
            delay(3000)
            actionsShown = false
        }
    }

    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { actionsShown = !actionsShown })
            }
    ) {
        SpicyLyricsView(
            lines = spicyLines,
            currentTimeMs = currentTimeMs,
            onSeekWord = {
                onSeekToPositionMillis(it - lyricsOffsetMs)
                actionsShown = false
            },
            modifier = Modifier.fillMaxSize(),
            fontSizeScale = fontSizeScale
        )


    }
}

@Composable
fun TtmlLyricsState(
    modifier: Modifier,
    parsedLyrics: ParsedLyrics,
    lyricsFetchSource: LyricsFetchSource,
    onSeekToPositionMillis: (Long) -> Unit,
    songProgressMillis: () -> Long
) {
    var currentTimeMs by remember { mutableLongStateOf(0L) }

    val uiSettings = com.tx24.spicyplayer.ui.common.LocalUserPreferences.current.uiSettings
    val lyricsOffsetMs = uiSettings.lyricsOffsetMs
    val fontSizeScale = when (uiSettings.lyricsFontSize) {
        "SMALL" -> 0.85f
        "LARGE" -> 1.15f
        else -> 1.0f
    }

    LaunchedEffect(parsedLyrics, lyricsOffsetMs) {
        while (isActive) {
            currentTimeMs = songProgressMillis() + lyricsOffsetMs
            delay(16)
        }
    }

    var actionsShown by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(key1 = actionsShown) {
        if (actionsShown) {
            delay(3000)
            actionsShown = false
        }
    }

    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { actionsShown = !actionsShown })
            }
    ) {
        SpicyLyricsView(
            lines = parsedLyrics.lines,
            currentTimeMs = currentTimeMs,
            onSeekWord = {
                onSeekToPositionMillis(it - lyricsOffsetMs)
                actionsShown = false
            },
            modifier = Modifier.fillMaxSize(),
            fontSizeScale = fontSizeScale
        )


    }
}

@Composable
fun LyricSynchronizerEffect(
    synchronizedLyrics: SynchronizedLyrics,
    songProgressMillis: () -> Long,
    onLyricsIndexCalculated: suspend (Int) -> Unit,
) {
    val updateLambda by rememberUpdatedState(newValue = onLyricsIndexCalculated)
    LaunchedEffect(synchronizedLyrics) {
        while (isActive) {
            val currentMillis = songProgressMillis()
            var index =
                synchronizedLyrics.segments.binarySearch { it.durationMillis - currentMillis.toInt() }
            if (index < 0) {
                index = (-(index + 1) - 1).coerceIn(0, synchronizedLyrics.segments.size - 1)
            }
            updateLambda.invoke(index)
            delay(200)
        }
    }
}


