package com.tx24.spicyplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tx24.spicyplayer.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.tx24.spicyplayer.BuildConfig
import com.tx24.spicyplayer.util.GitHubUpdateChecker
import com.tx24.spicyplayer.util.GitHubRelease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpToDate(val isManual: Boolean) : UpdateStatus()
    data class NewVersion(val release: GitHubRelease) : UpdateStatus()
    data class Error(val message: String, val isManual: Boolean) : UpdateStatus()
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus = _updateStatus.asStateFlow()

    // ── Lyrics ────────────────────────────────────────────────────────────
    val lyricsOffsetMs   = repo.lyricsOffsetMs.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val lyricsFontSize   = repo.lyricsFontSize.stateIn(viewModelScope, SharingStarted.Eagerly, "MEDIUM")

    // ── Audio ─────────────────────────────────────────────────────────────
    val eqPreset         = repo.eqPreset.stateIn(viewModelScope, SharingStarted.Eagerly, "FLAT")
    val bassBoostEnabled = repo.bassBoostEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val bassBoostStrength = repo.bassBoostStrength.stateIn(viewModelScope, SharingStarted.Eagerly, 800)
    val loudnessEnabled  = repo.loudnessEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val loudnessStrength = repo.loudnessStrength.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val crossfadeDuration = repo.crossfadeDuration.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val gaplessPlayback  = repo.gaplessPlayback.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val backSkipThreshold = repo.backSkipThreshold.stateIn(viewModelScope, SharingStarted.Eagerly, 5)
    val customEqBands    = repo.customEqBands.stateIn(viewModelScope, SharingStarted.Eagerly, listOf(0f, 0f, 0f, 0f, 0f))

    // ── Appearance ────────────────────────────────────────────────────────
    val appTheme         = repo.appTheme.stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")
    val materialYou      = repo.materialYou.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val backgroundBlur   = repo.backgroundBlur.stateIn(viewModelScope, SharingStarted.Eagerly, 60)
    val contrastLevel    = repo.contrastLevel.stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    // ── General ───────────────────────────────────────────────────────────
    val keepScreenOn     = repo.keepScreenOn.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val audioFocus       = repo.audioFocus.stateIn(viewModelScope, SharingStarted.Eagerly, "PAUSE")
    val scanDirectory    = repo.scanDirectory.stateIn(viewModelScope, SharingStarted.Eagerly, "/sdcard/Music/")

    // ── Setters ───────────────────────────────────────────────────────────
    fun setLyricsOffsetMs(v: Int)        = viewModelScope.launch { repo.setLyricsOffsetMs(v) }
    fun setLyricsFontSize(v: String)     = viewModelScope.launch { repo.setLyricsFontSize(v) }

    fun setEqPreset(v: String)           = viewModelScope.launch { repo.setEqPreset(v) }
    fun setBassBoost(v: Boolean)         = viewModelScope.launch { repo.setBassBoost(v) }
    fun setBassBoostStrength(v: Int)     = viewModelScope.launch { repo.setBassBoostStrength(v) }
    fun setLoudnessEnabled(v: Boolean)   = viewModelScope.launch { repo.setLoudnessEnabled(v) }
    fun setLoudnessStrength(v: Int)      = viewModelScope.launch { repo.setLoudnessStrength(v) }
    fun setCrossfadeDuration(v: Int)     = viewModelScope.launch { repo.setCrossfadeDuration(v) }
    fun setGaplessPlayback(v: Boolean)   = viewModelScope.launch { repo.setGaplessPlayback(v) }
    fun setBackSkipThreshold(v: Int)     = viewModelScope.launch { repo.setBackSkipThreshold(v) }
    fun setCustomEqBands(v: List<Float>) = viewModelScope.launch { repo.setCustomEqBands(v) }

    fun setAppTheme(v: String)           = viewModelScope.launch { repo.setAppTheme(v) }
    fun setMaterialYou(v: Boolean)       = viewModelScope.launch { repo.setMaterialYou(v) }
    fun setBackgroundBlur(v: Int)        = viewModelScope.launch { repo.setBackgroundBlur(v) }
    fun setContrastLevel(v: Float)       = viewModelScope.launch { repo.setContrastLevel(v) }

    fun setKeepScreenOn(v: Boolean)      = viewModelScope.launch { repo.setKeepScreenOn(v) }
    fun setAudioFocus(v: String)         = viewModelScope.launch { repo.setAudioFocus(v) }
    fun setScanDirectory(v: String)      = viewModelScope.launch { repo.setScanDirectory(v) }

    // ── Resets ────────────────────────────────────────────────────────────
    fun resetLyrics() = viewModelScope.launch {
        repo.setLyricsOffsetMs(0)
        repo.setLyricsFontSize("MEDIUM")
    }

    fun resetAudio() = viewModelScope.launch {
        repo.setEqPreset("FLAT")
        repo.setBassBoost(false)
        repo.setBassBoostStrength(800)
        repo.setLoudnessEnabled(false)
        repo.setLoudnessStrength(0)
        repo.setCrossfadeDuration(0)
        repo.setGaplessPlayback(true)
        repo.setBackSkipThreshold(5)
        repo.setCustomEqBands(listOf(0f, 0f, 0f, 0f, 0f))
        repo.setAudioFocus("PAUSE")
    }

    fun resetAppearance() = viewModelScope.launch {
        repo.setAppTheme("SYSTEM")
        repo.setMaterialYou(false)
        repo.setBackgroundBlur(60)
        repo.setContrastLevel(0f)
    }

    fun resetLibrary() = viewModelScope.launch {
        repo.setKeepScreenOn(false)
        repo.setScanDirectory("/sdcard/Music/")
    }

    fun resetAll() {
        resetLyrics()
        resetAudio()
        resetAppearance()
        resetLibrary()
    }

    // ── Updates ───────────────────────────────────────────────────────────
    fun checkForUpdates(isManual: Boolean) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            val release = GitHubUpdateChecker.getLatestRelease()
            if (release != null) {
                val currentVersion = BuildConfig.VERSION_NAME.trim()
                if (release.tagName.trim() != currentVersion) {
                    _updateStatus.value = UpdateStatus.NewVersion(release)
                } else {
                    _updateStatus.value = UpdateStatus.UpToDate(isManual)
                }
            } else {
                _updateStatus.value = UpdateStatus.Error("Failed to fetch update info", isManual)
            }
        }
    }

    fun clearUpdateStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }
}
