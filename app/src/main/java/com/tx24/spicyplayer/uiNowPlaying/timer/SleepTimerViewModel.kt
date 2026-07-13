package com.tx24.spicyplayer.uiNowPlaying.timer

import androidx.lifecycle.ViewModel
import com.tx24.spicyplayer.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class SleepTimerViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
): ViewModel() {

    fun schedule(
        minutes: Int,
        finishLastSong: Boolean = false
    ) {
        playbackManager.setSleepTimer(minutes, finishLastSong)
    }

    fun deleteTimer() {
        playbackManager.deleteSleepTimer()
    }


}