package com.tx24.spicyplayer.library.database.entities.playlist

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.tx24.spicyplayer.library.database.entities.PLAYLIST_ID_COLUMN
import com.tx24.spicyplayer.library.database.entities.PLAYLIST_SONG_ENTITY
import com.tx24.spicyplayer.library.database.entities.SONG_URI_STRING_COLUMN


@Entity(tableName = PLAYLIST_SONG_ENTITY, primaryKeys = [PLAYLIST_ID_COLUMN, SONG_URI_STRING_COLUMN])
data class PlaylistsSongsEntity(
    @ColumnInfo(name = PLAYLIST_ID_COLUMN)
    val playlistId: Int,

    @ColumnInfo(name = SONG_URI_STRING_COLUMN)
    val songUriString: String
)