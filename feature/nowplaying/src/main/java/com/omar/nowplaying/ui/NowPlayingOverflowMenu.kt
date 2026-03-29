package com.omar.nowplaying.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.hilt.navigation.compose.hiltViewModel
import com.omar.musica.store.model.song.Song
import com.omar.musica.ui.common.LocalCommonSongsAction
import com.omar.musica.ui.menu.MenuActionItem
import com.omar.musica.ui.menu.SpicyActionMenu
import com.omar.musica.ui.menu.SongBottomSheetMenu
import com.omar.musica.ui.menu.addToPlaylists
import com.omar.musica.ui.menu.delete
import com.omar.musica.ui.menu.equalizer
import com.omar.musica.ui.menu.playbackSpeed
import com.omar.musica.ui.menu.setAsRingtone
import com.omar.musica.ui.menu.share
import com.omar.musica.ui.menu.sleepTimer
import com.omar.musica.ui.menu.songInfo
import com.omar.musica.ui.menu.tagEditor
import com.omar.musica.ui.showShortToast
import com.omar.musica.ui.topbar.OverflowMenu
import com.omar.nowplaying.speed.rememberPlaybackSpeedDialog
import com.omar.nowplaying.timer.SleepTimerViewModel
import com.omar.nowplaying.timer.rememberSleepTimerDialog


interface NowPlayingOptions {
    fun addToPlaylist()
    fun sleepTimer()
    fun playbackSpeed()
    fun setAsRingtone()
    fun share()
    fun editTags()
    fun songInfo()
    fun equalizer()
    fun deleteFromDevice()
}


@Composable
fun NowPlayingOverflowMenu(
    options: NowPlayingOptions
) {

    val sleepTimerViewModel: SleepTimerViewModel = hiltViewModel()

    val context = LocalContext.current
    val sleepTimerDialog = rememberSleepTimerDialog(
        onSetTimer = { minutes, finishLastSong ->
            sleepTimerViewModel.schedule(minutes, finishLastSong)
            context.showShortToast("Sleep timer set")
        },
        onDeleteTimer = {
            sleepTimerViewModel.deleteTimer()
            context.showShortToast("Sleep timer deleted")
        }
    )


    val playbackSpeedDialog = rememberPlaybackSpeedDialog(viewModel = hiltViewModel())

    OverflowMenu(
        icon = Icons.Rounded.MoreHoriz,
        contentPaddingValues = PaddingValues(start = 16.dp, end = 36.dp, top = 4.dp, bottom = 4.dp),
        actionItems = mutableListOf<MenuActionItem>().apply {
            sleepTimer { sleepTimerDialog.launch() }
            addToPlaylists(options::addToPlaylist)
            playbackSpeed { playbackSpeedDialog.launch() }
            setAsRingtone(options::setAsRingtone)
            share(options::share)
            tagEditor(options::editTags)
            equalizer(options::equalizer)
            songInfo(options::songInfo)
            delete(options::deleteFromDevice)
        }

    )

}

@Composable
fun NowPlayingOverflowChip(
    options: NowPlayingOptions
) {
    val sleepTimerViewModel: SleepTimerViewModel = hiltViewModel()
    val context = LocalContext.current
    val sleepTimerDialog = rememberSleepTimerDialog(
        onSetTimer = { minutes, finishLastSong ->
            sleepTimerViewModel.schedule(minutes, finishLastSong)
            context.showShortToast("Sleep timer set")
        },
        onDeleteTimer = {
            sleepTimerViewModel.deleteTimer()
            context.showShortToast("Sleep timer deleted")
        }
    )
    val playbackSpeedDialog = rememberPlaybackSpeedDialog(viewModel = hiltViewModel())

    val actionItems = remember {
        mutableListOf<MenuActionItem>().apply {
            sleepTimer { sleepTimerDialog.launch() }
            addToPlaylists(options::addToPlaylist)
            playbackSpeed { playbackSpeedDialog.launch() }
            setAsRingtone(options::setAsRingtone)
            share(options::share)
            tagEditor(options::editTags)
            equalizer(options::equalizer)
            songInfo(options::songInfo)
            delete(options::deleteFromDevice)
        }
    }

    var visible by remember { mutableStateOf(false) }

    Box {
        SuggestionChip(
            onClick = { visible = !visible },
            label = { Text("More", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) },
            icon = { Icon(Icons.Rounded.MoreHoriz, contentDescription = null, modifier = Modifier.size(20.dp)) },
            shape = CircleShape,
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )

        SpicyActionMenu(
            visible = visible,
            onDismiss = { visible = false },
            items = actionItems
        )
    }
}

@Composable
fun rememberNowPlayingOptions(
    songUi: Song
): NowPlayingOptions {

    val commonSongsActions = LocalCommonSongsAction.current
    val context = LocalContext.current

    return remember(songUi) {
        object : NowPlayingOptions {
            override fun addToPlaylist() {
                commonSongsActions.addToPlaylistDialog.launch(listOf(songUi))
            }

            override fun sleepTimer() {

            }

            override fun editTags() {
                commonSongsActions.openTagEditorAction.open(songUi.uri)
            }

            override fun playbackSpeed() {
                TODO("Not yet implemented")
            }

            override fun setAsRingtone() {
                commonSongsActions.setRingtoneAction.setRingtone(songUi.uri)
            }

            override fun share() {
                commonSongsActions.shareAction.share(context, listOf(songUi))
            }

            override fun songInfo() {
                commonSongsActions.songInfoDialog.open(songUi)
            }

            override fun equalizer() {
                commonSongsActions.openEqualizer.open()
            }

            override fun deleteFromDevice() {
                commonSongsActions.deleteAction.deleteSongs(listOf(songUi))
            }
        }
    }
}
