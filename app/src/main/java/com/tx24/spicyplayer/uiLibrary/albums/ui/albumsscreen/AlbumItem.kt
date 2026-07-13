package com.tx24.spicyplayer.uiLibrary.albums.ui.albumsscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tx24.spicyplayer.library.store.model.album.BasicAlbum
import com.tx24.spicyplayer.ui.albumart.SongAlbumArtImage
import com.tx24.spicyplayer.ui.albumart.toSongAlbumArtModel


@Composable
fun AlbumRow(
    modifier: Modifier,
    album: BasicAlbum,
    isSelected: Boolean = false,
) {

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        SongAlbumArtImage(
            modifier = Modifier
                .size(56.dp)
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.large),
            songAlbumArtModel = album.firstSong.toSongAlbumArtModel(),
            crossFadeDuration = 150
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.albumInfo.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = album.albumInfo.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${album.albumInfo.numberOfSongs} songs",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelected) {
            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = "Selected")
        }
    }

}
