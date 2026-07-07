package com.tx24.spicyplayer.ui.shortcut

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.tx24.spicyplayer.playback.PlaybackManager
import com.tx24.spicyplayer.library.store.AlbumsRepository
import com.tx24.spicyplayer.library.store.PlaylistsRepository
import com.tx24.spicyplayer.ui.showShortToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class ShortcutActivity : ComponentActivity() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var albumsRepository: AlbumsRepository

    @Inject
    lateinit var playlistsRepository: PlaylistsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.extras ?: return finish()

        val command = data.getString(KEY_COMMAND, PLAY_COMMAND)
        val type = data.getString(KEY_TYPE, PLAYLIST_TYPE)

        // This is an invisible shortcut trampoline: do the async waits on the
        // lifecycle scope instead of blocking the main thread in onCreate (the
        // old runBlocking { waitUntilReady() } could hang the UI thread until
        // the MediaController connected), then finish once dispatched.
        lifecycleScope.launch {
            try {
                if (type == ALBUM_TYPE)
                    handleAlbum(command, data.getInt(KEY_ID, -1))
                else if (type == PLAYLIST_TYPE)
                    handlePlaylist(command, data.getInt(KEY_ID, -1))
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle shortcut")
            } finally {
                finish()
            }
        }
    }

    private suspend fun handleAlbum(command: String, albumId: Int) {

        if (command == VIEW_COMMAND) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "spicyplayer://albums/$albumId".toUri()
            }
            startActivity(intent)
            return
        }

        withTimeoutOrNull(READY_TIMEOUT_MS) { albumsRepository.waitUntilAlbumsReady() }
        val albumSongs = albumsRepository.albums
            .value.find { it.albumInfo.id == albumId } ?: return

        val songs = albumSongs.songs.sortedBy { it.trackNumber }.map { it.song }

        withTimeoutOrNull(READY_TIMEOUT_MS) { playbackManager.waitUntilReady() }
        if (command == SHUFFLE_COMMAND)
            playbackManager.shuffle(songs)
        else if (command == PLAY_COMMAND)
            playbackManager.setPlaylistAndPlayAtIndex(songs)

        showShortToast("${albumSongs.albumInfo.name} started playing")
    }

    private suspend fun handlePlaylist(command: String, playlist: Int) {

        if (playlist == -1) return

        if (command == VIEW_COMMAND) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "spicyplayer://playlists/$playlist".toUri()
            }
            startActivity(intent)
            return
        }

        val playlistInfo = playlistsRepository.getPlaylistWithSongsFlow(playlist)
            .firstOrNull()

        withTimeoutOrNull(READY_TIMEOUT_MS) { playbackManager.waitUntilReady() }
        if (command == SHUFFLE_COMMAND)
            playbackManager.shufflePlaylist(playlist)
        else if (command == PLAY_COMMAND)
            playbackManager.playPlaylist(playlist)

        val name = playlistInfo?.playlistInfo?.name ?: "Playlist"
        showShortToast("$name started playing")
    }

    companion object {
        private const val READY_TIMEOUT_MS = 5_000L

        const val KEY_COMMAND = "COMMAND"
        const val KEY_TYPE = "TYPE"
        const val KEY_ID = "NAME"

        const val PLAY_COMMAND = "PLAY"
        const val SHUFFLE_COMMAND = "SHUFFLE"
        const val VIEW_COMMAND = "VIEW"

        const val ALBUM_TYPE = "ALBUM"
        const val PLAYLIST_TYPE = "PLAYLIST"
    }

}