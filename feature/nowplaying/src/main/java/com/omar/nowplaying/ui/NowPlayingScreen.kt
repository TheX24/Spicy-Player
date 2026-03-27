package com.omar.nowplaying.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.ui.draw.alpha
import com.omar.musica.model.playback.RepeatMode
import kotlin.math.abs
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.zIndex
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.omar.musica.ui.common.LocalUserPreferences
import com.omar.musica.ui.model.AppThemeUi
import com.omar.musica.ui.model.PlayerThemeUi
import com.omar.nowplaying.NowPlayingState
import com.omar.nowplaying.queue.QueueScreen
import com.omar.nowplaying.song
import com.omar.nowplaying.viewmodel.INowPlayingViewModel
import com.omar.nowplaying.viewmodel.NowPlayingViewModel
import com.omar.nowplaying.ui.SpicyDynamicBackground
import kotlin.math.abs


@Composable
fun NowPlayingScreen(
    modifier: Modifier,
    nowPlayingBarPadding: PaddingValues,
    barHeight: Dp,
    isExpanded: Boolean,
    onCollapseNowPlaying: () -> Unit,
    onExpandNowPlaying: () -> Unit,
    progressProvider: () -> Float,
    viewModel: NowPlayingViewModel = hiltViewModel()
) {

    val focusManager = LocalFocusManager.current
    LaunchedEffect(key1 = isExpanded) {
        if (isExpanded) {
            focusManager.clearFocus(true)
        }
    }

    if (isExpanded) {
        BackHandler(true) {
            onCollapseNowPlaying()
        }
    }

    val uiState by viewModel.state.collectAsState()

    if (uiState is NowPlayingState.Playing)
        NowPlayingScreen(
            modifier = modifier.clip(MaterialTheme.shapes.large),
            nowPlayingBarPadding = nowPlayingBarPadding,
            uiState = uiState as NowPlayingState.Playing,
            barHeight = barHeight,
            isExpanded = isExpanded,
            onCollapseNowPlaying = onCollapseNowPlaying,
            onExpandNowPlaying = onExpandNowPlaying,
            progressProvider = progressProvider,
            nowPlayingActions = viewModel
        )
}

@Composable
internal fun NowPlayingScreen(
    modifier: Modifier,
    nowPlayingBarPadding: PaddingValues,
    uiState: NowPlayingState.Playing,
    barHeight: Dp,
    isExpanded: Boolean,
    onCollapseNowPlaying: () -> Unit,
    onExpandNowPlaying: () -> Unit,
    progressProvider: () -> Float,
    nowPlayingActions: INowPlayingViewModel
) {

    val playerTheme = LocalUserPreferences.current.uiSettings.playerThemeUi
    val isDarkTheme = when (LocalUserPreferences.current.uiSettings.theme) {
        AppThemeUi.DARK -> true
        AppThemeUi.LIGHT -> false
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val window = remember(context) {
        var c = context
        while (c is ContextWrapper) {
            if (c is Activity) break
            c = c.baseContext
        }
        (c as? Activity)?.window
    }
    val insetsController = remember(window) { window?.let { WindowCompat.getInsetsController(it, it.decorView) } }

    SideEffect {
        if (isExpanded) {
            insetsController?.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController?.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Restore bars when this screen is completely removed from the hierarchy
            insetsController?.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        }
    }

    // Since we use a darker background image for the NowPlaying screen
    // we need to make the status bar icons lighter
    if (isExpanded && (isDarkTheme || playerTheme == PlayerThemeUi.BLUR))
        DarkStatusBarEffect()


    Surface(
        modifier = modifier, //if (MaterialTheme.colorScheme.background == Color.Black) 0.dp else 3.dp,

    ) {

        Box(modifier = Modifier.fillMaxSize()) {

            var isShowingQueue by remember {
                mutableStateOf(false)
            }
            NowPlayingMaterialTheme(playerThemeUi = playerTheme) {

                FullScreenNowPlaying(
                    Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .graphicsLayer {
                            alpha = ((progressProvider() - 0.15f) * 2.0f).coerceIn(0.0f, 1.0f)
                        },
                    isShowingQueue,
                    { isShowingQueue = false },
                    { isShowingQueue = true },
                    onCollapseNowPlaying,
                    progressProvider,
                    uiState,
                    nowPlayingActions = nowPlayingActions
                )
            }
            LaunchedEffect(key1 = isExpanded) {
                if (!isExpanded) isShowingQueue = false
            }
            MiniPlayer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .padding(nowPlayingBarPadding)
                    .pointerInput(isExpanded) {
                        if (!isExpanded) {
                            detectTapGestures { onExpandNowPlaying() }
                        }
                    }
                    .graphicsLayer {
                        alpha = (1 - (progressProvider() * 6.66f).coerceAtMost(1.0f))
                    },
                nowPlayingState = uiState,
                showExtraControls = LocalUserPreferences.current.uiSettings.showMiniPlayerExtraControls,
                songProgressProvider = nowPlayingActions::currentSongProgress,
                enabled = !isExpanded, // if the view is expanded then disable the header
                nowPlayingActions::togglePlayback,
                nowPlayingActions::nextSong,
                nowPlayingActions::previousSong
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalFoundationApi::class)
@Composable
fun FullScreenNowPlaying(
    modifier: Modifier,
    isShowingQueue: Boolean,
    onCloseQueue: () -> Unit,
    onOpenQueue: () -> Unit,
    onCollapse: () -> Unit,
    progressProvider: () -> Float,
    uiState: NowPlayingState.Playing,
    nowPlayingActions: INowPlayingViewModel,
) {

    val song = uiState.song

    val pagerState = rememberPagerState(remember { uiState.songIndex }) { uiState.queue.size }

    var isShowingLyrics by remember {
        mutableStateOf(false)
    }

    val currentSongIndex = uiState.songIndex
    LaunchedEffect(currentSongIndex) {
        if (currentSongIndex == pagerState.targetPage || currentSongIndex == pagerState.currentPage) return@LaunchedEffect

        if (abs(currentSongIndex - pagerState.targetPage) == 1)
            pagerState.animateScrollToPage(currentSongIndex, animationSpec = spring(stiffness = Spring.StiffnessMedium))
        else
            pagerState.scrollToPage(currentSongIndex)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        val playerTheme = LocalUserPreferences.current.uiSettings.playerThemeUi
        AnimatedVisibility(
            visible = playerTheme == PlayerThemeUi.BLUR,
            enter = fadeIn(), exit = fadeOut()
        ) {

            Box(modifier = Modifier.matchParentSize()) {

                SpicyDynamicBackground(
                    modifier = Modifier.fillMaxSize(),
                    song = uiState.song
                )


            }
        }


        val activity = LocalContext.current as Activity
        val windowSizeClass = calculateWindowSizeClass(activity = activity)
        val heightClass = windowSizeClass.heightSizeClass
        val widthClass = windowSizeClass.widthSizeClass


        val screenSize = when {
            heightClass == WindowHeightSizeClass.Compact && widthClass == WindowWidthSizeClass.Compact -> NowPlayingScreenSize.COMPACT
            heightClass == WindowHeightSizeClass.Compact && widthClass != WindowWidthSizeClass.Compact -> NowPlayingScreenSize.LANDSCAPE
            else -> NowPlayingScreenSize.PORTRAIT
        }


        val paddingModifier = remember(screenSize) {
            if (screenSize == NowPlayingScreenSize.LANDSCAPE)
                Modifier.padding(16.dp)
            else
                Modifier
        }

        val playerScreenModifier = remember(paddingModifier) {
            Modifier
                .fillMaxSize()
                .zIndex(1f)
                .graphicsLayer {
                    alpha = ((progressProvider() - 0.15f) * 2.0f).coerceIn(0.0f, 1.0f)
                }
                .then(paddingModifier)
                .safeDrawingPadding()
        }

        AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            targetState = isShowingQueue, label = "",
            transitionSpec = {
                if (this.targetState)
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith 
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                else
                    scaleIn(initialScale = 1.1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) + 
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith 
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
            }
        ) {
            if (it) {
                QueueScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .graphicsLayer { alpha = progressProvider() * 2 },
                    onClose = onCloseQueue
                )
            } else {

                CompositionLocalProvider(
                    LocalPagerState provides pagerState
                ) {
                    PlayingScreen2(
                        modifier = playerScreenModifier.navigationBarsPadding(),
                        songs = uiState.queue,
                        songIndex = uiState.songIndex,
                        song = song,
                        playbackState = uiState.playbackState,
                        repeatMode = uiState.repeatMode,
                        isShuffleOn = uiState.isShuffleOn,
                        isShowingLyrics = isShowingLyrics,
                        onToggleLyrics = { isShowingLyrics = !isShowingLyrics },
                        screenSize = screenSize,
                        nowPlayingActions = nowPlayingActions,
                        onOpenQueue = onOpenQueue,
                        onCollapse = onCollapse
                    )
                }
            }
        }


    }
}


@Composable
fun SongControls(
    modifier: Modifier,
    isPlaying: Boolean,
    isShuffleOn: Boolean,
    repeatMode: RepeatMode,
    playButtonColor: Color,
    onPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Shuffle
        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(48.dp)) {
            Icon(
                modifier = if (isShuffleOn) Modifier else Modifier.alpha(0.5f),
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Prev
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }

        // Play/Pause (Squircle, Giant)
        FilledIconButton(
            onClick = onTogglePlayback,
            modifier = Modifier.size(88.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = playButtonColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Play/Pause",
                modifier = Modifier.size(48.dp)
            )
        }

        // Next
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }

        // Repeat
        IconButton(onClick = onToggleRepeat, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = repeatMode.getIconVector(),
                contentDescription = "Repeat",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

    }
}

@Composable
fun ControlButton(
    modifier: Modifier,
    icon: ImageVector,
    tint: Color? = null,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    val iconModifier = remember {
        modifier.clickable { onClick() }
    }
    Icon(
        modifier = iconModifier,
        imageVector = icon,
        tint = tint ?: LocalContentColor.current,
        contentDescription = contentDescription
    )

}

enum class NowPlayingScreenSize {
    LANDSCAPE, PORTRAIT, COMPACT
}