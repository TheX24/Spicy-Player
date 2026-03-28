package com.omar.musica.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omar.musica.settings.BuildConfig
import com.omar.musica.settings.components.GitHubUpdateChecker
import com.omar.musica.settings.components.UpdateStatus
import com.omar.musica.store.ScanProgress
import com.omar.musica.store.ScanStateRepository
import com.omar.musica.store.preferences.UserPreferencesRepository
import com.omar.musica.ui.model.AppThemeUi
import com.omar.musica.ui.model.PlayerThemeUi
import com.omar.musica.ui.model.UserPreferencesUi
import com.omar.musica.ui.model.toAppTheme
import com.omar.musica.ui.model.toPlayerTheme
import com.omar.musica.ui.model.toUiModel
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
    private val scanStateRepository: ScanStateRepository
) : ViewModel(), ISettingsViewModel {

    override val state = userPreferencesRepository.userSettingsFlow
        .map { SettingsState.Loaded(it.toUiModel()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState.Loading)

    override val cacheAlbumArt =
        userPreferencesRepository.librarySettingsFlow.map { it.cacheAlbumCoverArt }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    override val scanDirectory =
        userPreferencesRepository.librarySettingsFlow.map { it.scanDirectory }
            .stateIn(viewModelScope, SharingStarted.Eagerly, "/sdcard/Music/")

    override val scanProgress = scanStateRepository.scanProgress
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val scanHistory = scanStateRepository.scanHistory
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    override val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    override fun checkForUpdates(isManual: Boolean) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            val release = GitHubUpdateChecker.getLatestRelease()
            if (release != null) {
                val currentVersion = BuildConfig.VERSION_NAME
                if ("v\$currentVersion" != release.tagName) {
                    _updateStatus.value = UpdateStatus.NewVersion(release)
                } else {
                    _updateStatus.value = UpdateStatus.UpToDate(isManual)
                }
            } else {
                _updateStatus.value = UpdateStatus.Error(isManual, "Failed to check for updates")
            }
        }
    }

    override fun clearUpdateStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

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

    override fun setScanDirectory(dir: String) {
        viewModelScope.launch { userPreferencesRepository.setScanDirectory(dir) }
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

    override fun setContrastLevel(level: Float) {
        viewModelScope.launch {
            userPreferencesRepository.onContrastLevelChanged(level)
        }
    }

    override fun resetAll() {
        viewModelScope.launch {
            userPreferencesRepository.clear()
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
    val scanDirectory: StateFlow<String>
    val scanProgress: StateFlow<ScanProgress?>
    val scanHistory: StateFlow<List<String>>
    val updateStatus: StateFlow<UpdateStatus>

    fun checkForUpdates(isManual: Boolean)
    fun clearUpdateStatus()

    fun onFolderDeleted(folder: String)

    fun onToggleCacheAlbumArt()

    fun onFolderAdded(folder: String)

    fun onThemeSelected(appTheme: AppThemeUi)

    fun setPreviousSkipThreshold(durationMillis: Int)
    fun setShowTranslation(show: Boolean)
    fun setReplayGain(gain: Boolean)
    fun setVisualizerEnabled(enabled: Boolean)
    fun setScanDirectory(dir: String)

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
    fun setContrastLevel(level: Float)
    fun resetAll()
    fun setKeepScreenOn(keep: Boolean)
    fun setCrossfadeDuration(duration: Int)
    fun setGaplessPlayback(gapless: Boolean)
    fun setAudioFocusBehavior(behavior: String)
}

sealed interface SettingsState {
    data object Loading : SettingsState
    data class Loaded(val userPreferences: UserPreferencesUi) : SettingsState
}