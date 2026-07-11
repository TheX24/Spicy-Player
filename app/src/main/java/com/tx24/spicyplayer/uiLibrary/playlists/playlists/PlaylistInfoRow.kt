package com.tx24.spicyplayer.uiLibrary.playlists.playlists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tx24.spicyplayer.model.playlist.PlaylistInfo
import com.tx24.spicyplayer.ui.common.RenamableTextView


@Composable
fun PlaylistInfoRow(
    modifier: Modifier,
    playlistInfo: PlaylistInfo,
    inRenameMode: Boolean,
    onRename: (String) -> Unit,
    onEnableRenameMode: () -> Unit,
) {

    Row(
        modifier = modifier.padding(
            start = 12.dp,
            end = 12.dp,
            top = 12.dp,
            bottom = 12.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        PlaylistImage()
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            RenamableTextView(
                modifier = Modifier,
                text = playlistInfo.name,
                fontWeight = FontWeight.Normal,
                fontSize = 16,
                inRenameMode = inRenameMode,
                onEnableRenameMode = onEnableRenameMode,
                onRename = onRename,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = playlistInfo.numberOfSongs.toString() + " songs",
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

}

@Composable
fun PlaylistImage() {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = "", modifier = Modifier.size(24.dp))
        }
    }
}
