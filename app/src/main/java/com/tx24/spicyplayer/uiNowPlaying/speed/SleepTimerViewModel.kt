package com.tx24.spicyplayer.uiNowPlaying.speed

import androidx.lifecycle.ViewModel
import com.tx24.spicyplayer.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class PlaybackSpeedViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
): ViewModel() {

    val playbackParameters: Pair<Float, Float>
        get() = playbackManager.playbackParameters

    fun setParameters(speed: Float, pitch: Float) {
        playbackManager.setPlaybackParameters(speed, pitch)
    }

}