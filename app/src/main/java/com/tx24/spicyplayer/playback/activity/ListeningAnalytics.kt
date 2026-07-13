package com.tx24.spicyplayer.playback.activity

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.tx24.spicyplayer.model.activity.ListeningSession
import com.tx24.spicyplayer.library.store.AnalyticsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.Date


class ListeningAnalytics @AssistedInject constructor(
    private val analyticsRepository: AnalyticsRepository,
    @Assisted private val player: Player,
) : Player.Listener {


    init {
        player.addListener(this)
    }

    private var currentListeningSessionInfo: CurrentListeningSessionInfo? = null

    private val currentTimeSeconds: Long
        get() = System.currentTimeMillis() / 1000


    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            val item = player.currentMediaItem
            currentListeningSessionInfo = CurrentListeningSessionInfo(
                startDate = Date(),
                songUri = item?.requestMetadata?.mediaUri?.toString().orEmpty(),
                songName = item?.mediaMetadata?.title?.toString().orEmpty(),
                albumName = item?.mediaMetadata?.albumTitle?.toString().orEmpty(),
            )
        } else {
            val l = currentListeningSessionInfo ?: return
            flushSession(l)
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {

    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {

    }

    /**
     * Calculates session length and stores it in the repository
     */
    private fun flushSession(l: CurrentListeningSessionInfo) {
        val listeningSession = ListeningSession(
            l.startDate,
            (currentTimeSeconds - l.startDate.timeSeconds).toInt(),
            songUri = l.songUri,
            songName = l.songName,
            albumName = l.albumName,
        )
        analyticsRepository.insertListeningSession(listeningSession)
    }

    private val Date.timeSeconds get() = (this.time / 1000)

    private data class CurrentListeningSessionInfo(
        val startDate: Date,
        val songUri: String,
        val songName: String,
        val albumName: String,
    )

    @AssistedFactory
    interface Factory {
        fun create(player: Player): ListeningAnalytics
    }

}