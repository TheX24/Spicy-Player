package com.tx24.spicyplayer.library.database.model

import androidx.room.Embedded
import com.tx24.spicyplayer.library.database.entities.playlist.PlaylistEntity


data class PlaylistInfoWithNumberOfSongs(
    @Embedded
    val playlistEntity: PlaylistEntity,
    val numberOfSongs: Int
)