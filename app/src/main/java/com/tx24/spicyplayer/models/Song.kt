package com.tx24.spicyplayer.models

import java.io.File

/**
 * Represents a song with its associated files and metadata.
 */
data class Song(
    val file: File,
    val ttmlFile: File?,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val year: String = "",
    val trackNumber: Int = 0
)
