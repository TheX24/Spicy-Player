package com.tx24.spicyplayer.uiLibrary.playlists.playlists

import com.tx24.spicyplayer.model.playlist.PlaylistInfo

sealed interface PlaylistsScreenState {

    data object Loading : PlaylistsScreenState
    data class Success(val playlists: List<PlaylistInfo>) : PlaylistsScreenState

}