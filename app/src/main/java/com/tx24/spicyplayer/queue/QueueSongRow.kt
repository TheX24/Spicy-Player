package com.tx24.spicyplayer.queue


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tx24.spicyplayer.library.store.model.song.Song
import com.tx24.spicyplayer.ui.common.LocalUserPreferences
import com.tx24.spicyplayer.ui.songs.SongInfoRow
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItemScope
import kotlin.math.abs
import kotlin.math.roundToInt



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueSongRow(
    modifier: Modifier,
    songUi: Song,
    isPlaying: Boolean,
    swipeToDeleteDelay: Int,
    isDragging: Boolean,
    reorderScope: ReorderableItemScope,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    onRemoveFromQueue: () -> Unit,
) {

    val density = LocalDensity.current
    val swipeDistance = with(density) { 300.dp.toPx() }
    val anchorState = remember {
        AnchoredDraggableState(
            SwipeToDeleteState.IDLE,
            anchors = DraggableAnchors {
                SwipeToDeleteState.LEFT at -swipeDistance
                SwipeToDeleteState.IDLE at 0.0f
            },
            positionalThreshold = { 0.85f * it },
            velocityThreshold = { with(density) { 150.dp.toPx() } },
            snapAnimationSpec = spring(stiffness = Spring.StiffnessLow),
            decayAnimationSpec = exponentialDecay()
        )
    }

    var rowWidth by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(key1 = anchorState.settledValue) {
        if (anchorState.settledValue != SwipeToDeleteState.IDLE) {
            delay(swipeToDeleteDelay.toLong())
            onRemoveFromQueue()
        }
    }

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f), label = "drag_elevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f), label = "drag_scale"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
            isPlaying -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = spring(),
        label = "container_color"
    )

    Box(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onSizeChanged {
                val width = it.width
                rowWidth = width
                anchorState.updateAnchors(
                    DraggableAnchors {
                        SwipeToDeleteState.LEFT at -width.toFloat()
                        SwipeToDeleteState.IDLE at 0.0f
                    },
                    SwipeToDeleteState.IDLE
                )
            }
    ) {

        DeleteBackground(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium),
            swipeProgress = { anchorState.offset / rowWidth }
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(
                        anchorState.offset.roundToInt(), 0
                    )
                }
                .anchoredDraggable(anchorState, Orientation.Horizontal, enabled = !isDragging),
            shape = MaterialTheme.shapes.medium,
            color = containerColor,
            tonalElevation = if (isPlaying && !isDragging) 2.dp else 0.dp,
            shadowElevation = elevation
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongInfoRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    song = songUi,
                    efficientThumbnailLoading = LocalUserPreferences.current.librarySettings.cacheAlbumCoverArt
                )
                IconButton(
                    onClick = {},
                    modifier = with(reorderScope) {
                        Modifier
                            .draggableHandle(
                                onDragStarted = { onDragStarted() },
                                onDragStopped = { onDragStopped() }
                            )
                            .padding(horizontal = 8.dp)
                    }) {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = "Drag to Reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteBackground(
    modifier: Modifier,
    swipeProgress: () -> Float, // -1 to 1
) {

    Row(
        modifier = modifier.background(MaterialTheme.colorScheme.errorContainer),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Icon(
            modifier = Modifier
                .padding(start = 32.dp)
                .graphicsLayer {
                    val progress = abs(swipeProgress())
                    val scale = (2.0f * progress + 0.5f).coerceAtMost(1.0f)
                    alpha = if (swipeProgress() > 0.0f) scale else 0.0f
                    scaleX = if (swipeProgress() > 0.0f) scale else 0.0f
                    scaleY = if (swipeProgress() > 0.0f) scale else 0.0f
                },
            imageVector = Icons.Rounded.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )

        Icon(
            modifier = Modifier
                .padding(end = 32.dp)
                .graphicsLayer {
                    val progress = abs(swipeProgress())
                    val scale = (2.0f * progress + 0.5f).coerceAtMost(1.0f)
                    alpha = if (swipeProgress() < 0.0f) scale else 0.0f
                    scaleX = if (swipeProgress() < 0.0f) scale else 0.0f
                    scaleY = if (swipeProgress() < 0.0f) scale else 0.0f
                },
            imageVector = Icons.Rounded.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }

}

enum class SwipeToDeleteState {
    LEFT, IDLE, RIGHT
}