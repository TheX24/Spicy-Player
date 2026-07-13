package com.tx24.spicyplayer.ui.actions

import android.content.Context
import com.tx24.spicyplayer.library.store.model.song.Song


fun interface SongShareAction {

    fun share(context: Context, songs: List<Song>)

}