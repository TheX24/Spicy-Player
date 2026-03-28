package com.omar.nowplaying.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.omar.musica.store.model.song.Song
import com.omar.musica.ui.albumart.toSongAlbumArtModel
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
    var lastReportedPage by remember { mutableIntStateOf(pagerState.targetPage) }




    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage }
            .distinctUntilChanged()
            .collect { page ->
                if (lastReportedPage != page) {
                    lastReportedPage = page
                    onSongSwitched(page)
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        key = { songs[it].uri }, // optional, improves performance
        contentPadding = PaddingValues(horizontal = 0.dp),
        pageSpacing = 24.dp,
        beyondViewportPageCount = 1,
    ) { index ->
        val song = songs[index]
        
        var highResMetadata by remember(song.filePath) { mutableStateOf<NowPlayingMetadata?>(null) }
        
        LaunchedEffect(song.filePath) {
            highResMetadata = NowPlayingMetadataCache.getMetadata(song)
        }

        if (highResMetadata?.art != null) {
            Image(
                bitmap = highResMetadata!!.art!!.asImageBitmap(),
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
