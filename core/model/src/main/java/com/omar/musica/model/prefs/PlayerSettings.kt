package com.omar.musica.model.prefs


/**
 * Settings applied to the player
 */
data class PlayerSettings(
    val previousSkipThreshold: Int = 5,
    /**
     * Pause when volume reaches zero?
     */
    val pauseOnVolumeZero: Boolean,

    /**
     * Should we start playback when volume increases
     * if it was paused before due to zero volume
     */
    val resumeWhenVolumeIncreases: Boolean,

    val crossfadeDuration: Int = 0,
    val gaplessPlayback: Boolean = true,
    val audioFocusBehavior: String = "PAUSE",
    val showTranslation: Boolean = false,
    val replayGain: Boolean = false,
    val visualizerEnabled: Boolean = false
) {
    sealed class VolumeAction {
        object Pause : VolumeAction()
        object Lower : VolumeAction()
        object None : VolumeAction()
    }

    fun getVolumeAction(level: Int): VolumeAction {
        return if (level < 1 && pauseOnVolumeZero) {
            VolumeAction.Pause
        } else {
            VolumeAction.None
        }
    }
}



const val DEFAULT_JUMP_DURATION_MILLIS = 10_000