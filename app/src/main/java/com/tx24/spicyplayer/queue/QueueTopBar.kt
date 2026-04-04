package com.tx24.spicyplayer.queue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tx24.spicyplayer.ui.menu.MenuActionItem
import com.tx24.spicyplayer.ui.millisToTime
import com.tx24.spicyplayer.ui.topbar.OverflowMenu


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueTopBar(
    color: Color,
    numberOfSongsRemaining: Int,
    durationMillisRemaining: Long,
    onClose: () -> Unit = {},
    onSaveAsPlaylist: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(text = "Queue")
                Text(
                    text = "$numberOfSongsRemaining tracks • ${durationMillisRemaining.millisToTime()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = color),
        windowInsets = TopAppBarDefaults.windowInsets,
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close Queue")
            }
        },
        actions = {
            OverflowMenu(actionItems = listOf(
                MenuActionItem(Icons.Rounded.SaveAs, "Save as Playlist", onSaveAsPlaylist)
            ))
        }
    )
}