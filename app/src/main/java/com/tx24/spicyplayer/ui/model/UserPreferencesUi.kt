package com.tx24.spicyplayer.ui.model

import androidx.compose.runtime.Stable
import com.tx24.spicyplayer.model.prefs.UserPreferences

@Stable
data class UserPreferencesUi(
    val librarySettings: LibrarySettingsUi = LibrarySettingsUi(),
    val uiSettings: UiSettingsUi = UiSettingsUi(),
    val playerSettings: PlayerSettingsUi = PlayerSettingsUi()
)

fun UserPreferences.toUiModel() =
    UserPreferencesUi(
        librarySettings.toLibrarySettingsUi(),
        uiSettings.toUiSettingsUi(),
        playerSettings.toPlayerSettingsUi()
    )