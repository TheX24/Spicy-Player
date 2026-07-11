package com.tx24.spicyplayer.model.prefs



data class UserPreferences(

    val librarySettings: LibrarySettings,

    val uiSettings: UiSettings,

    val playerSettings: PlayerSettings

)



enum class AppTheme {
    LIGHT, DARK, SYSTEM
}