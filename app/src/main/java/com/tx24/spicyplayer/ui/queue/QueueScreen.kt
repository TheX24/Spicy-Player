package com.tx24.spicyplayer.ui.queue

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tx24.spicyplayer.ui.canvas.DynamicBackgroundView
import com.tx24.spicyplayer.models.Song
import java.io.File

@Composable
fun QueueScreen(
    playQueue: List<Song>,
    playQueueIndex: Int,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onTrackSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    
    // Scroll to current track when queue is displayed
    LaunchedEffect(playQueueIndex) {
        if (playQueueIndex in playQueue.indices) {
            listState.animateScrollToItem(playQueueIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 32.dp)
            .padding(bottom = bottomPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                itemsIndexed(playQueue) { index, song ->
                    val isPlaying = index == playQueueIndex
                    ListItem(
                        headlineContent = {
                            Text(
                                text = song.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isPlaying) colorScheme.primary else colorScheme.onSurface,
                                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                                style = if (isPlaying) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
                            )
                        },
                        supportingContent = {
                            Text(
                                text = song.artist,
                                color = colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            if (isPlaying) {
                                Icon(
                                    Icons.Rounded.GraphicEq, 
                                    contentDescription = "Playing", 
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.MusicNote, 
                                    contentDescription = null, 
                                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        modifier = Modifier.clickable { onTrackSelected(index) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
                }
            }
        }
    }
}
