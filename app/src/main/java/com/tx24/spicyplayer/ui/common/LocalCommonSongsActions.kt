package com.tx24.spicyplayer.ui.common

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.tx24.spicyplayer.playback.PlaybackManager
import com.tx24.spicyplayer.library.store.MediaRepository
import com.tx24.spicyplayer.ui.actions.EqualizerOpener
import com.tx24.spicyplayer.ui.actions.GoToAlbumAction
import com.tx24.spicyplayer.ui.actions.OpenTagEditorAction
import com.tx24.spicyplayer.ui.actions.SetRingtone
import com.tx24.spicyplayer.ui.actions.SetRingtoneAction
import com.tx24.spicyplayer.ui.actions.SongDeleteAction
import com.tx24.spicyplayer.ui.actions.SongPlaybackActions
import com.tx24.spicyplayer.ui.actions.SongPlaybackActionsImpl
import com.tx24.spicyplayer.ui.actions.SongShareAction
import com.tx24.spicyplayer.ui.actions.SongsSharer
import com.tx24.spicyplayer.ui.actions.rememberCreatePlaylistShortcutDialog
import com.tx24.spicyplayer.ui.actions.rememberSongDeleter
import com.tx24.spicyplayer.ui.playlist.AddToPlaylistDialog
import com.tx24.spicyplayer.ui.playlist.rememberAddToPlaylistDialog
import com.tx24.spicyplayer.ui.shortcut.ShortcutDialog
import com.tx24.spicyplayer.ui.songs.SongInfoDialog
import com.tx24.spicyplayer.ui.songs.rememberSongDialog


data class CommonSongsActions(
    val playbackActions: SongPlaybackActions,
    val shareAction: SongShareAction,
    val deleteAction: SongDeleteAction,
    val songInfoDialog: SongInfoDialog,
    val addToPlaylistDialog: AddToPlaylistDialog,
    val openEqualizer: EqualizerOpener,
    val setRingtoneAction: SetRingtoneAction,
    val openTagEditorAction: OpenTagEditorAction,
    val createShortcutDialog: ShortcutDialog,
    val goToAlbumAction: GoToAlbumAction
)

val LocalCommonSongsAction = staticCompositionLocalOf<CommonSongsActions>
{ throw IllegalArgumentException("not implemented") }

@Composable
fun rememberCommonSongsActions(
    playbackManager: PlaybackManager,
    mediaRepository: MediaRepository,
    openTagEditorAction: OpenTagEditorAction,
    goToAlbumAction: GoToAlbumAction
): CommonSongsActions {

    val context = LocalContext.current
    val songPlaybackActions = SongPlaybackActionsImpl(context, playbackManager)
    val shareAction = SongsSharer
    val deleteAction = rememberSongDeleter(mediaRepository = mediaRepository)
    val songInfoDialog = rememberSongDialog()
    val addToPlaylistDialog = rememberAddToPlaylistDialog()
    val openEqualizer = remember { EqualizerOpener(context as Activity) }
    val setRingtoneAction = remember { SetRingtone(context) }
    val shortcutDialog = rememberCreatePlaylistShortcutDialog()

    return remember {
        CommonSongsActions(
            songPlaybackActions,
            shareAction,
            deleteAction,
            songInfoDialog,
            addToPlaylistDialog,
            openEqualizer,
            setRingtoneAction,
            openTagEditorAction,
            shortcutDialog,
            goToAlbumAction
        )
    }
}