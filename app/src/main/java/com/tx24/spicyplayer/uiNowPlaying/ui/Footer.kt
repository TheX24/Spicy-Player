package com.tx24.spicyplayer.uiNowPlaying.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.tx24.spicyplayer.model.playback.RepeatMode
import com.tx24.spicyplayer.library.store.model.song.Song


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerFooter(
    modifier: Modifier,
    songUi: Song,
    isShuffleOn: Boolean,
    repeatMode: RepeatMode,
    isLyricsOpen: Boolean,
    onOpenQueue: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleRepeatMode: () -> Unit,
    onToggleShuffle: () -> Unit,
) {

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        // queue button
        TooltipBox(
            tooltip = {
                PlainTooltip { Text("Queue") }
            },
            state = rememberTooltipState(),
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider()
        ) {
            IconButton(
                onClick = onOpenQueue
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Queue"
                )
            }
        }

        // spacer
        //Spacer(modifier = Modifier.weight(1f))

        // icons
        TooltipBox(
            tooltip = {
                PlainTooltip { Text("Lyrics") }
            },
            state = rememberTooltipState(),
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider()
        ) {
            IconButton(onClick = onToggleLyrics) {
                Icon(
                    modifier = if (isLyricsOpen) Modifier else Modifier.alpha(0.5f),
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = "Lyrics"
                )
            }
        }
    }
}