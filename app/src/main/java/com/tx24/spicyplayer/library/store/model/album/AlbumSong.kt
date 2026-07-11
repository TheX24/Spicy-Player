package com.tx24.spicyplayer.library.store.model.album

import com.tx24.spicyplayer.library.store.model.song.Song


data class AlbumSong(
    val song: Song,
    val trackNumber: Int? = null
)