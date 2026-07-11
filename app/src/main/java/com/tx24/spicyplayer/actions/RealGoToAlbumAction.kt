package com.tx24.spicyplayer.actions

import androidx.navigation.NavController
import com.tx24.spicyplayer.uiLibrary.albums.navigation.navigateToAlbumDetail
import com.tx24.spicyplayer.library.store.AlbumsRepository
import com.tx24.spicyplayer.library.store.model.song.Song
import com.tx24.spicyplayer.ui.actions.GoToAlbumAction

class RealGoToAlbumAction(
    private val albumsRepository: AlbumsRepository,
    private val navController: NavController
) : GoToAlbumAction {

    override fun open(song: Song) {
        val albumId = albumsRepository.getSongAlbumId(song) ?: return
        navController.navigateToAlbumDetail(albumId)
    }

}