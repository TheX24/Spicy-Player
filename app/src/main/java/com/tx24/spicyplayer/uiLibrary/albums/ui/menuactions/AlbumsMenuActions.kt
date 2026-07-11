package com.tx24.spicyplayer.uiLibrary.albums.ui.menuactions

import com.tx24.spicyplayer.ui.menu.MenuActionItem
import com.tx24.spicyplayer.ui.menu.addToPlaylists
import com.tx24.spicyplayer.ui.menu.addToQueue
import com.tx24.spicyplayer.ui.menu.play
import com.tx24.spicyplayer.ui.menu.playNext
import com.tx24.spicyplayer.ui.menu.shuffle
import com.tx24.spicyplayer.ui.menu.shuffleNext


fun buildAlbumsMenuActions(
    onPlay: () -> Unit,
    addToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onShuffle: () -> Unit,
    onShuffleNext: () -> Unit,
    onAddToPlaylists: () -> Unit
): List<MenuActionItem> {
    return mutableListOf<MenuActionItem>()
        .apply {
            play(onPlay)
            addToQueue(addToQueue)
            playNext(onPlayNext)
            shuffle(onShuffle)
            shuffleNext(onShuffleNext)
            addToPlaylists(onAddToPlaylists)
        }
}