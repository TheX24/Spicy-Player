package com.tx24.spicyplayer.uiNowPlaying.floating

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import com.tx24.spicyplayer.ui.albumart.BlurTransformation
import com.tx24.spicyplayer.ui.albumart.SongAlbumArtModel
import com.tx24.spicyplayer.ui.albumart.toSongAlbumArtModel
import com.tx24.spicyplayer.uiNowPlaying.NowPlayingState
import com.tx24.spicyplayer.uiNowPlaying.ui.CrossFadingAlbumArt
import com.tx24.spicyplayer.uiNowPlaying.ui.ErrorPainterType


@Composable
fun FloatingMiniPlayer(
    modifier: Modifier,
    nowPlayingState: NowPlayingState,
    showExtraControls: Boolean,
    songProgressProvider: () -> Float,
    enabled: Boolean,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    if (nowPlayingState !is NowPlayingState.Playing) return
    val song = nowPlayingState.queue[nowPlayingState.songIndex]
    Box(modifier, contentAlignment = Alignment.Center) {

        // show background

        SongBlurredBackground(
            modifier = Modifier.fillMaxSize(),
            songAlbumArtModel = song.toSongAlbumArtModel()
        )

        // draw content
    }
}

@Composable
fun SongBlurredBackground(
    modifier: Modifier,
    songAlbumArtModel: SongAlbumArtModel
) {
    CrossFadingAlbumArt(
        modifier = modifier,
        songAlbumArtModel = songAlbumArtModel,
        errorPainterType = ErrorPainterType.SOLID_COLOR,
        blurTransformation = remember { BlurTransformation() },
        colorFilter = ColorFilter.tint(
            Color(0xFFBBBBBB), BlendMode.Multiply
        )
    )
}