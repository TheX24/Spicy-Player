package com.tx24.spicyplayer.uiLibrary.albums.viewmodel

import androidx.compose.runtime.Stable
import com.tx24.spicyplayer.library.store.model.album.AlbumWithSongs
import com.tx24.spicyplayer.library.store.model.album.BasicAlbum


sealed interface AlbumDetailsScreenState {
    data object Loading : AlbumDetailsScreenState

    @Stable
    data class Loaded(
        val albumWithSongs: AlbumWithSongs,
        val otherAlbums: List<BasicAlbum>
    ) : AlbumDetailsScreenState
}