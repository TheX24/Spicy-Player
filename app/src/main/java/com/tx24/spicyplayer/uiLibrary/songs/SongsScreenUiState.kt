package com.tx24.spicyplayer.uiLibrary.songs

import androidx.compose.runtime.Immutable
import com.tx24.spicyplayer.model.SongSortOption
import com.tx24.spicyplayer.library.store.model.song.Song


sealed interface SongsScreenUiState {

    @Immutable
    data class Success(
        val songs: List<Song>,
        val songSortOption: SongSortOption = SongSortOption.TITLE,
        val isSortedAscendingly: Boolean = true
    ) : SongsScreenUiState
}