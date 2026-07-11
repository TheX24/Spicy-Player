package com.tx24.spicyplayer.model.prefs

import com.tx24.spicyplayer.model.AlbumsSortOption
import com.tx24.spicyplayer.model.SongSortOption


typealias IsAscending = Boolean

/**
 * Settings applied to the library and main screen.
 */
data class LibrarySettings(

    /**
     * The order of the songs on the main screen
     */
    val songsSortOrder: Pair<SongSortOption, IsAscending>,

    /**
     * The order of the songs on the main screen
     */
    val albumsSortOrder: Pair<AlbumsSortOption, IsAscending>,

    /**
     * How many columns to show in the album
     * screen
     */
    val albumsGridSize: Int = 2,

    /**
     * Whether to load the actual album art of the song or
     * to cache album arts of songs of the same album
     */
    val cacheAlbumCoverArt: Boolean,

    /**
     * Files in these folders should not appear in the app
     */
    val excludedFolders: List<String>,

    /**
     * Directory used for manual deep-scanning of music and TTML files
     */
    val scanDirectory: String = "/sdcard/Music/"
)
