package com.tx24.spicyplayer.uiNowPlaying.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.core.tween
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.tx24.spicyplayer.library.store.model.song.Song
import com.tx24.spicyplayer.ui.albumart.toSongAlbumArtModel
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlin.math.abs


@OptIn(ExperimentalFoundationApi::class)
val LocalPagerState = compositionLocalOf<PagerState> { throw IllegalStateException() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumArtPager(
    modifier: Modifier,
    songs: List<Song>,
    currentSongIndex: Int,
    onSongSwitched: (Int) -> Unit
) {
    val pagerState = LocalPagerState.current
    val updatedOnSongSwitched by rememberUpdatedState(onSongSwitched)
    var lastReportedPage by remember { mutableIntStateOf(pagerState.currentPage) }

    // Synchronize lastReportedPage with currentSongIndex to avoid triggering 
    // a callback when the index changes programmatically.
    SideEffect {
        if (!pagerState.isScrollInProgress) {
            lastReportedPage = currentSongIndex
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (page, isScrolling) ->
                if (!isScrolling && lastReportedPage != page) {
                    lastReportedPage = page
                    updatedOnSongSwitched(page)
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        key = { songs.getOrNull(it)?.uri ?: it }, // optional, improves performance
        contentPadding = PaddingValues(horizontal = 0.dp),
        pageSpacing = 24.dp,
        beyondViewportPageCount = 1,
    ) { index ->
        val song = songs.getOrNull(index) ?: return@HorizontalPager
        
        var highResMetadata by remember(song.filePath) { mutableStateOf<NowPlayingMetadata?>(null) }
        
        LaunchedEffect(song.filePath) {
            highResMetadata = NowPlayingMetadataCache.getMetadata(song)
        }

        androidx.compose.animation.Crossfade(
            targetState = highResMetadata?.art,
            label = "art_crossfade",
            animationSpec = tween(500)
        ) { art ->
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                NowPlayingSquareAlbumArt(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    song = song.toSongAlbumArtModel()
                )
            }
        }
    }
}
