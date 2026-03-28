package com.omar.musica.ui.model

import androidx.compose.runtime.Stable
import com.omar.musica.model.prefs.PlayerSettings


@Stable
data class PlayerSettingsUi(
    val previousSkipThreshold: Int = 5000,
    val pauseOnVolumeZero: Boolean = true,
    val resumeWhenVolumeIncreases: Boolean = true,
    val crossfadeDuration: Int = 0,
    val gaplessPlayback: Boolean = true,
    val audioFocusBehavior: String = "PAUSE",
    val showTranslation: Boolean = false,
    val replayGain: Boolean = false,
    val visualizerEnabled: Boolean = false
)



fun PlayerSettings.toPlayerSettingsUi() =
    PlayerSettingsUi(
        previousSkipThreshold,
        pauseOnVolumeZero,
        resumeWhenVolumeIncreases,
        crossfadeDuration,
        gaplessPlayback,
        audioFocusBehavior,
        showTranslation,
        replayGain,
        visualizerEnabled
    )