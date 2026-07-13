package com.tx24.spicyplayer.library.store.model.tags

import android.graphics.Bitmap
import android.net.Uri
import com.tx24.spicyplayer.model.song.ExtendedSongMetadata


data class SongTags(
    val uri: Uri,
    val artwork: Bitmap? = null,
    val metadata: ExtendedSongMetadata
)