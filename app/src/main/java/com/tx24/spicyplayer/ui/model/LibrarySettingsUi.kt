package com.tx24.spicyplayer.ui.model

import androidx.compose.runtime.Stable
import com.tx24.spicyplayer.model.AlbumsSortOption
import com.tx24.spicyplayer.model.SongSortOption
import com.tx24.spicyplayer.model.prefs.IsAscending
import com.tx24.spicyplayer.model.prefs.LibrarySettings


@Stable
data class LibrarySettingsUi(
    val songsSortOrder: Pair<SongSortOption, IsAscending> = SongSortOption.TITLE to true,
    val albumsSortOrder: Pair<AlbumsSortOption, IsAscending> = AlbumsSortOption.NAME to true,
    val albumsGridSize: Int = 2,
    val cacheAlbumCoverArt: Boolean = true,
    val excludedFolders: List<String> = emptyList(),
    val scanDirectory: String = "/sdcard/Music/"
)

fun LibrarySettings.toLibrarySettingsUi() =
    LibrarySettingsUi(
        songsSortOrder, albumsSortOrder, albumsGridSize, cacheAlbumCoverArt, excludedFolders, scanDirectory
    )