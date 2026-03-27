package com.omar.nowplaying.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.omar.musica.store.model.song.Song

/**
 * Animated header displaying AlbumArtPager and track info.
 * Positions and scales elements based on [isShowingLyrics].
 */
@Composable
fun NowPlayingHeader(
    modifier: Modifier = Modifier,
    songs: List<Song>,
    songIndex: Int,
    isShowingLyrics: Boolean,
    onSongSwitched: (Int) -> Unit
) {
    val expansionProgress by animateFloatAsState(
        targetValue = if (isShowingLyrics) 0f else 1f,
        animationSpec = tween(800, easing = LinearOutSlowInEasing),
        label = "ExpansionProgress"
    )

    val verticalBias by animateFloatAsState(
        targetValue = if (isShowingLyrics) 0f else -1f,
        animationSpec = tween(800, easing = LinearOutSlowInEasing),
        label = "VerticalBias"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        val maxImageSize = maxWidth
        val smallImageSize = 120.dp
        val imageSize = lerp(smallImageSize, maxImageSize, expansionProgress)
        val horizontalBias = -1f + expansionProgress // -1f (Left) to 0f (Center)

        val song = if (songIndex in songs.indices) songs[songIndex] else null

        // 1. Track Info Column (Orbiting) - Drawn FIRST (Under)
        val offsetX = lerp(smallImageSize + 16.dp, 0.dp, expansionProgress)
        val offsetY = lerp(0.dp, imageSize + 12.dp, expansionProgress)
        val availableWidth = maxWidth - offsetX

        Column(
            modifier = Modifier
                .align(BiasAlignment(horizontalBias, verticalBias))
                .offset(x = offsetX, y = offsetY)
                .widthIn(max = availableWidth)
                .graphicsLayer {
                    // Slight fade if transitioning? For now keep it solid.
                },
            horizontalAlignment = if (expansionProgress > 0.5f) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (song != null) {
                @OptIn(ExperimentalFoundationApi::class)
                Text(
                    text = song.metadata.title,
                    color = Color.White,
                    style = if (expansionProgress > 0.5f) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 2000
                    )
                )
                @OptIn(ExperimentalFoundationApi::class)
                Text(
                    text = song.metadata.artistName ?: "Unknown Artist",
                    color = Color.White.copy(alpha = 0.7f),
                    style = if (expansionProgress > 0.5f) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 2000
                    )
                )
            }
        }

        // 2. Album Art Pager - Drawn SECOND (Over)
        Box(
            modifier = Modifier
                .size(imageSize)
                .align(BiasAlignment(horizontalBias, verticalBias))
        ) {
            AlbumArtPager(
                modifier = Modifier.fillMaxSize(),
                songs = songs,
                currentSongIndex = songIndex,
                onSongSwitched = onSongSwitched
            )
        }
    }
}
