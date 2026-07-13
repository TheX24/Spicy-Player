package com.tx24.spicyplayer.uiLibrary.albums.viewmodel

import com.tx24.spicyplayer.model.AlbumsSortOption
import com.tx24.spicyplayer.model.prefs.IsAscending
import com.tx24.spicyplayer.library.store.model.album.BasicAlbum

interface AlbumsScreenActions {

    fun changeGridSize(newSize: Int)
    fun changeSortOptions(sortOption: AlbumsSortOption, isAscending: IsAscending)

    fun playAlbums(albumNames: List<BasicAlbum>)
    fun playAlbumsNext(albumNames: List<BasicAlbum>)
    fun addAlbumsToQueue(albumNames: List<BasicAlbum>)

    fun shuffleAlbums(albumNames: List<BasicAlbum>)
    fun shuffleAlbumsNext(albumNames: List<BasicAlbum>)

    fun addAlbumsToPlaylist(albumNames: List<BasicAlbum>, playlistName: String)
}