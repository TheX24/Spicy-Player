package com.tx24.spicyplayer.library.store.model.playlist

import com.tx24.spicyplayer.model.playlist.PlaylistInfo
import com.tx24.spicyplayer.library.store.model.song.Song


data class Playlist(
    val playlistInfo: PlaylistInfo,
    val songs: List<Song>
)