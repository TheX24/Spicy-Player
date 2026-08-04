package com.tx24.spicyplayer.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx24.spicyplayer.library.store.FolderInfo
import com.tx24.spicyplayer.library.store.MediaRepository
import com.tx24.spicyplayer.library.store.preferences.UserPreferencesRepository
import com.tx24.spicyplayer.ui.model.AppThemeUi
import com.tx24.spicyplayer.model.prefs.AppTheme
import com.tx24.spicyplayer.ui.model.PlayerThemeUi
import com.tx24.spicyplayer.ui.model.UserPreferencesUi
import com.tx24.spicyplayer.ui.model.toAppTheme
import com.tx24.spicyplayer.ui.model.toPlayerTheme
import com.tx24.spicyplayer.ui.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val mediaRepository: MediaRepository
) : ViewModel(), ISettingsViewModel {

    private val _discoveredFolders = MutableStateFlow<List<FolderInfo>>(emptyList())
    override val discoveredFolders: StateFlow<List<FolderInfo>> = _discoveredFolders.asStateFlow()

    override fun refreshDiscoveredFolders() {
        viewModelScope.launch { _discoveredFolders.value = mediaRepository.getAudioFolders() }
    }

    override fun rescanLibrary() {
        mediaRepository.rescanLibrary()
    }

    init { refreshDiscoveredFolders() }

    override val state = userPreferencesRepository.userSettingsFlow
        .map { SettingsState.Loaded(it.toUiModel()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState.Loading)

    override val cacheAlbumArt =
        userPreferencesRepository.librarySettingsFlow.map { it.cacheAlbumCoverArt }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    override fun onFolderDeleted(folder: String) {
        viewModelScope.launch {
            userPreferencesRepository.deleteFolderFromBlacklist(folder)
        }
    }

    override fun onToggleCacheAlbumArt() {
        viewModelScope.launch {
            userPreferencesRepository.toggleCacheAlbumArt()
        }
    }

    override fun onFolderAdded(folder: String) {
        viewModelScope.launch {
            userPreferencesRepository.addBlacklistedFolder(folder)
        }
    }

    override fun onThemeSelected(appTheme: AppThemeUi) {
        viewModelScope.launch {
            userPreferencesRepository.changeTheme(appTheme.toAppTheme())
        }
    }

    override fun setPreviousSkipThreshold(durationMillis: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setPreviousSkipThreshold(durationMillis)
        }
    }

    override fun setShowTranslation(show: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowTranslation(show) }
    }

    override fun setReplayGain(gain: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setReplayGain(gain) }
    }

    override fun setVisualizerEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setVisualizerEnabled(enabled) }
    }

    override fun toggleDynamicColorScheme() {
        viewModelScope.launch {
            userPreferencesRepository.toggleDynamicColor()
        }
    }

    override fun onPlayerThemeChanged(playerTheme: PlayerThemeUi) {
        viewModelScope.launch {
            userPreferencesRepository.changePlayerTheme(playerTheme.toPlayerTheme())
        }
    }

    override fun toggleBlackBackgroundForDarkTheme() {
        viewModelScope.launch {
            userPreferencesRepository.toggleBlackBackgroundForDarkTheme()
        }
    }

    override fun togglePauseVolumeZero() {
        viewModelScope.launch {
            userPreferencesRepository.togglePauseVolumeZero()
        }
    }

    override fun toggleResumeVolumeNotZero() {
        viewModelScope.launch {
            userPreferencesRepository.toggleResumeVolumeNotZero()
        }
    }

    override fun setAccentColor(color: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setAccentColor(color)
        }
    }

    override fun toggleShowExtraControls() {
        viewModelScope.launch {
            userPreferencesRepository.toggleMiniPlayerExtraControls()
        }
    }

    override fun setLyricsOffsetMs(offset: Int) {
        viewModelScope.launch { userPreferencesRepository.setLyricsOffsetMs(offset) }
    }

    override fun setLyricsFontSize(size: String) {
        viewModelScope.launch { userPreferencesRepository.setLyricsFontSize(size) }
    }

    override fun setBackgroundBlur(blur: Int) {
        viewModelScope.launch { userPreferencesRepository.setBackgroundBlur(blur) }
    }

    override fun setLyricsQualityMode(mode: String) {
        viewModelScope.launch { userPreferencesRepository.setLyricsQualityMode(mode) }
    }

    override fun setLyricsBackgroundEngine(engine: String) {
        viewModelScope.launch { userPreferencesRepository.setLyricsBackgroundEngine(engine) }
    }

    override fun setLyricsRomanize(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setLyricsRomanize(enabled) }
    }


    override fun resetAll() {
        viewModelScope.launch {
            userPreferencesRepository.clear()
        }
    }

    override fun resetLyrics() {
        viewModelScope.launch {
            userPreferencesRepository.setLyricsOffsetMs(0)
            userPreferencesRepository.setLyricsFontSize("MEDIUM")
            userPreferencesRepository.setLyricsQualityMode("FULL")
            userPreferencesRepository.setLyricsBackgroundEngine("AUTO")
            userPreferencesRepository.setLyricsRomanize(false)
        }
    }

    override fun resetAudio() {
        viewModelScope.launch {
            userPreferencesRepository.setCrossfadeDuration(0)
            userPreferencesRepository.setGaplessPlayback(true)
            userPreferencesRepository.setPreviousSkipThreshold(5)
            userPreferencesRepository.setAudioFocusBehavior("PAUSE")
            userPreferencesRepository.setReplayGain(false)
        }
    }

    override fun resetAppearance() {
        viewModelScope.launch {
            userPreferencesRepository.changeTheme(AppTheme.SYSTEM)
            userPreferencesRepository.setDynamicColor(false)
            userPreferencesRepository.setBlackBackgroundForDarkTheme(false)
            userPreferencesRepository.setBackgroundBlur(60)
        }
    }

    override fun resetPlayer() {
        viewModelScope.launch {
            userPreferencesRepository.setPauseVolumeZero(false)
            userPreferencesRepository.setResumeVolumeNotZero(false)
            userPreferencesRepository.setMiniPlayerExtraControls(false)
            userPreferencesRepository.setVisualizerEnabled(false)
            userPreferencesRepository.setKeepScreenOn(false)
        }
    }

    override fun setKeepScreenOn(keep: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setKeepScreenOn(keep) }
    }

    override fun setCrossfadeDuration(duration: Int) {
        viewModelScope.launch { userPreferencesRepository.setCrossfadeDuration(duration) }
    }

    override fun setGaplessPlayback(gapless: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setGaplessPlayback(gapless) }
    }

    override fun setAudioFocusBehavior(behavior: String) {
        viewModelScope.launch { userPreferencesRepository.setAudioFocusBehavior(behavior) }
    }
}

@Stable
interface ISettingsViewModel {
    val state: StateFlow<SettingsState>
    val cacheAlbumArt: StateFlow<Boolean>
    val discoveredFolders: StateFlow<List<FolderInfo>>
    fun onFolderDeleted(folder: String)

    fun onToggleCacheAlbumArt()

    fun onFolderAdded(folder: String)

    fun refreshDiscoveredFolders()
    fun rescanLibrary()

    fun onThemeSelected(appTheme: AppThemeUi)

    fun setPreviousSkipThreshold(durationMillis: Int)
    fun setShowTranslation(show: Boolean)
    fun setReplayGain(gain: Boolean)
    fun setVisualizerEnabled(enabled: Boolean)

    fun toggleDynamicColorScheme()

    fun onPlayerThemeChanged(playerTheme: PlayerThemeUi)

    fun toggleBlackBackgroundForDarkTheme()

    fun togglePauseVolumeZero()

    fun toggleResumeVolumeNotZero()

    fun setAccentColor(color: Int)

    fun toggleShowExtraControls()

    fun setLyricsOffsetMs(offset: Int)
    fun setLyricsFontSize(size: String)
    fun setBackgroundBlur(blur: Int)
    fun setLyricsQualityMode(mode: String)
    fun setLyricsBackgroundEngine(engine: String)
    fun setLyricsRomanize(enabled: Boolean)
    fun resetAll()
    fun resetLyrics()
    fun resetAudio()
    fun resetAppearance()
    fun resetPlayer()
    fun setKeepScreenOn(keep: Boolean)
    fun setCrossfadeDuration(duration: Int)
    fun setGaplessPlayback(gapless: Boolean)
    fun setAudioFocusBehavior(behavior: String)
}

sealed interface SettingsState {
    data object Loading : SettingsState
    data class Loaded(val userPreferences: UserPreferencesUi) : SettingsState
}
