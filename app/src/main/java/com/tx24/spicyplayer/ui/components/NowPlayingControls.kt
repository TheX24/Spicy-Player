package com.tx24.spicyplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Floating bottom controls for playback, seekbar, and technical information.
 */
@Composable
fun NowPlayingControls(
    controlsCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    currentTimeMs: Long,
    currentDurationMs: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    isDraggingSlider: Boolean,
    onDraggingChanged: (Boolean) -> Unit,
    currentBitrate: String,
    currentFormat: String,
    primaryTint: Color,
    onShowMenu: () -> Unit,
    onShuffle: () -> Unit,
    isShuffleActive: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    loopMode: Int,
    onToggleLoop: () -> Unit,
    colorScheme: ColorScheme,
    formatTime: (Long) -> String,
    isExpressive: Boolean = false
) {
    if (isExpressive) {
        NowPlayingControlsExpressive(
            controlsCollapsed = controlsCollapsed,
            onToggleCollapse = onToggleCollapse,
            currentTimeMs = currentTimeMs,
            currentDurationMs = currentDurationMs,
            onSeek = onSeek,
            onSeekFinished = onSeekFinished,
            isDraggingSlider = isDraggingSlider,
            onDraggingChanged = onDraggingChanged,
            currentBitrate = currentBitrate,
            currentFormat = currentFormat,
            primaryTint = primaryTint,
            onShowMenu = onShowMenu,
            onShuffle = onShuffle,
            isShuffleActive = isShuffleActive,
            onPrevious = onPrevious,
            onNext = onNext,
            isPlaying = isPlaying,
            onTogglePlay = onTogglePlay,
            loopMode = loopMode,
            onToggleLoop = onToggleLoop,
            colorScheme = colorScheme,
            formatTime = formatTime
        )
    } else {
        NowPlayingControlsClassic(
            controlsCollapsed = controlsCollapsed,
            onToggleCollapse = onToggleCollapse,
            currentTimeMs = currentTimeMs,
            currentDurationMs = currentDurationMs,
            onSeek = onSeek,
            onSeekFinished = onSeekFinished,
            isDraggingSlider = isDraggingSlider,
            onDraggingChanged = onDraggingChanged,
            currentBitrate = currentBitrate,
            currentFormat = currentFormat,
            primaryTint = primaryTint,
            onShowMenu = onShowMenu,
            onShuffle = onShuffle,
            isShuffleActive = isShuffleActive,
            onPrevious = onPrevious,
            onNext = onNext,
            isPlaying = isPlaying,
            onTogglePlay = onTogglePlay,
            loopMode = loopMode,
            onToggleLoop = onToggleLoop,
            colorScheme = colorScheme,
            formatTime = formatTime
        )
    }
}

@Composable
fun NowPlayingControlsClassic(
    controlsCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    currentTimeMs: Long,
    currentDurationMs: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    isDraggingSlider: Boolean,
    onDraggingChanged: (Boolean) -> Unit,
    currentBitrate: String,
    currentFormat: String,
    primaryTint: Color,
    onShowMenu: () -> Unit,
    onShuffle: () -> Unit,
    isShuffleActive: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    loopMode: Int,
    onToggleLoop: () -> Unit,
    colorScheme: ColorScheme,
    formatTime: (Long) -> String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.Black.copy(alpha = 0.5f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Row 1: Collapse | Seekbar Area | Menu ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleCollapse) {
                    Icon(
                        if (controlsCollapsed) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = (currentTimeMs.toFloat() / currentDurationMs.toFloat()).coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            onDraggingChanged(true)
                            onSeek((fraction * currentDurationMs).toLong())
                        },
                        onValueChangeFinished = {
                            onSeekFinished()
                            onDraggingChanged(false)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = primaryTint,
                            activeTrackColor = primaryTint,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Time & Tech Info row
                    AnimatedVisibility(visible = !controlsCollapsed) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentTimeMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            if (currentBitrate.isNotEmpty() || currentFormat.isNotEmpty()) {
                                Surface(
                                    color = primaryTint.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        primaryTint.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = "${currentFormat} • ${currentBitrate}",
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 2.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = formatTime(currentDurationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                IconButton(onClick = onShowMenu) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }
            }

            // ── Row 2: Playback Section [Shuffle | [Prev | Play | Next] | Loop] ──
            AnimatedVisibility(visible = !controlsCollapsed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = onShuffle) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleActive) primaryTint else Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Playback Pill
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            FilledIconButton(
                                onClick = onTogglePlay,
                                modifier = Modifier.size(64.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = primaryTint,
                                    contentColor = colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = onNext,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Loop
                    IconButton(onClick = onToggleLoop) {
                        Icon(
                            if (loopMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (loopMode != 0) primaryTint else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NowPlayingControlsExpressive(
    controlsCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    currentTimeMs: Long,
    currentDurationMs: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    isDraggingSlider: Boolean,
    onDraggingChanged: (Boolean) -> Unit,
    currentBitrate: String,
    currentFormat: String,
    primaryTint: Color,
    onShowMenu: () -> Unit,
    onShuffle: () -> Unit,
    isShuffleActive: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    loopMode: Int,
    onToggleLoop: () -> Unit,
    colorScheme: ColorScheme,
    formatTime: (Long) -> String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(36.dp), // M3 Expressive max corner
        color = colorScheme.surfaceVariant,
        contentColor = colorScheme.onSurfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Row 1: Collapse | Seekbar Area | Menu ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleCollapse, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (controlsCollapsed) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    val progress = if (currentDurationMs > 0) {
                        (currentTimeMs.toFloat() / currentDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    
                    Slider(
                        value = progress,
                        onValueChange = { fraction ->
                            onDraggingChanged(true)
                            onSeek((fraction * currentDurationMs).toLong())
                        },
                        onValueChangeFinished = {
                            onSeekFinished()
                            onDraggingChanged(false)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = primaryTint,
                            activeTrackColor = primaryTint,
                            inactiveTrackColor = colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                    // Time & Tech Info row
                    AnimatedVisibility(visible = !controlsCollapsed) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentTimeMs),
                                style = MaterialTheme.typography.titleSmall, // Expressive bold time
                                color = colorScheme.onSurfaceVariant
                            )
                            if (currentBitrate.isNotEmpty() || currentFormat.isNotEmpty()) {
                                Surface(
                                    color = colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "${currentFormat} • ${currentBitrate}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Text(
                                text = formatTime(currentDurationMs),
                                style = MaterialTheme.typography.titleSmall, // Expressive bold time
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = onShowMenu, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Menu",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // ── Row 2: Playback Section [Shuffle | [Prev | Play | Next] | Loop] ──
            AnimatedVisibility(visible = !controlsCollapsed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = onShuffle) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleActive) colorScheme.primary else colorScheme.onSurfaceVariant
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
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    // Play/Pause Expressive (Squircle, Giant)
                    FilledIconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier.size(88.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(28.dp) // Squircle
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
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Loop
                    IconButton(onClick = onToggleLoop) {
                        Icon(
                            if (loopMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (loopMode != 0) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
