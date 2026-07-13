package com.tx24.spicyplayer.uiLibrary.albums.ui.menuactions

import com.tx24.spicyplayer.ui.menu.MenuActionItem
import com.tx24.spicyplayer.ui.menu.addShortcutToHomeScreen
import com.tx24.spicyplayer.ui.menu.addToPlaylists
import com.tx24.spicyplayer.ui.menu.addToQueue
import com.tx24.spicyplayer.ui.menu.playNext
import com.tx24.spicyplayer.ui.menu.shuffleNext


fun buildSingleAlbumMenuActions(
    onPlayNext: () -> Unit,
    addToQueue: () -> Unit,
    onShuffleNext: () -> Unit,
    onAddToPlaylists: () -> Unit,
    onCreateShortcut: () -> Unit,
): List<MenuActionItem> {
    return mutableListOf<MenuActionItem>()
        .apply {
            playNext(onPlayNext)
            addToQueue(addToQueue)
            shuffleNext(onShuffleNext)
            addToPlaylists(onAddToPlaylists)
            addShortcutToHomeScreen(onCreateShortcut)
        }
}