package com.tx24.spicyplayer.models

import android.graphics.Bitmap

/**
 * Data class for the Now Playing header.
 */
data class NowPlayingData(
    val index: Int,
    val trackName: String = "",
    val artistName: String = "",
    val coverArt: Bitmap? = null,
    val bitrate: String = "",
    val format: String = ""
)

/**
 * Metadata extracted from the song file.
 */
data class NowPlayingMetadata(
    val t: String,
    val a: String,
    val art: Bitmap?,
    val bitrate: String,
    val ext: String
)
