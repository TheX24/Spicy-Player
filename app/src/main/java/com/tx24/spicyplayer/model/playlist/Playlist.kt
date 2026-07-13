package com.tx24.spicyplayer.model.playlist


/**
 * Represents the details of a playlist without containing the actual songs.
 * The playlist object which contains the actual songs are inside the data layer.
 */
data class PlaylistInfo(
    val id: Int,
    val name: String,
    val numberOfSongs: Int,
)

