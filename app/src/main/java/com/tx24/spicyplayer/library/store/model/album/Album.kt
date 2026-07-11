package com.tx24.spicyplayer.library.store.model.album

import com.tx24.spicyplayer.model.album.BasicAlbumInfo
import com.tx24.spicyplayer.library.store.model.song.Song


data class BasicAlbum(
    val albumInfo: BasicAlbumInfo,

    /**
     * Used to get the album cover art
     */
    val firstSong: Song? = null
)

data class AlbumWithSongs(
    val albumInfo: BasicAlbumInfo,
    val songs: List<AlbumSong>
)