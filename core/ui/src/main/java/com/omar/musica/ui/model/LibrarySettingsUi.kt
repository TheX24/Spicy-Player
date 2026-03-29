package com.omar.musica.ui.model

import androidx.compose.runtime.Stable
import com.omar.musica.model.AlbumsSortOption
import com.omar.musica.model.SongSortOption
import com.omar.musica.model.prefs.IsAscending
import com.omar.musica.model.prefs.LibrarySettings


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