package com.tx24.spicyplayer.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx24.spicyplayer.settings.components.GitHubUpdateChecker
import com.tx24.spicyplayer.settings.components.UpdateStatus
import com.tx24.spicyplayer.settings.components.getInstalledAppVersion
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel(), IUpdateViewModel {
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private var hasCheckedOnStartup = false

    fun checkForUpdatesOnStartup() {
        if (hasCheckedOnStartup) return
        hasCheckedOnStartup = true
        checkForUpdates(isManual = false)
    }

    override fun checkForUpdates(isManual: Boolean) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            val release = GitHubUpdateChecker.getLatestRelease()
            if (release != null) {
                val currentVersion = getInstalledAppVersion(context).name
                val latestTag = release.tagName.lowercase().removePrefix("v")
                if (currentVersion.lowercase().removePrefix("v") != latestTag) {
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
}

interface IUpdateViewModel {
    fun checkForUpdates(isManual: Boolean)
    fun clearUpdateStatus()
}
