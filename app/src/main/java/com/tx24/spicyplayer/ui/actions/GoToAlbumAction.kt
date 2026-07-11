package com.tx24.spicyplayer.ui.actions

import android.net.Uri
import com.tx24.spicyplayer.library.store.model.song.Song

interface GoToAlbumAction {
    fun open(song: Song)
}