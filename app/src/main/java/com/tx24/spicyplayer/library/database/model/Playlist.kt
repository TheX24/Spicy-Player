package com.tx24.spicyplayer.library.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.tx24.spicyplayer.library.database.entities.PLAYLIST_ID_COLUMN
import com.tx24.spicyplayer.library.database.entities.playlist.PlaylistEntity
import com.tx24.spicyplayer.library.database.entities.playlist.PlaylistsSongsEntity


data class PlaylistWithSongsUri(
    @Embedded
    val playlistEntity: PlaylistEntity,

    @Relation(entity = PlaylistsSongsEntity::class, parentColumn = PLAYLIST_ID_COLUMN, entityColumn = PLAYLIST_ID_COLUMN)
    val songUris: List<PlaylistsSongsEntity>
)
