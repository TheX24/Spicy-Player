package com.tx24.spicyplayer.uiLibrary.songs

import androidx.compose.runtime.Immutable
import com.tx24.spicyplayer.model.album.BasicAlbumInfo
import com.tx24.spicyplayer.library.store.model.album.BasicAlbum
import com.tx24.spicyplayer.library.store.model.song.Song


@Immutable
data class SearchScreenUiState(
    val searchQuery: String,
    val songs: List<Song>,
    val albums: List<BasicAlbum>
) {
    companion object {
        val emptyState = SearchScreenUiState("", listOf() , listOf())
    }
}