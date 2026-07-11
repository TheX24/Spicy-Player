package com.tx24.spicyplayer.model.prefs

/**
 * Settings related to Ui of the Application
 */
data class UiSettings(
    /**
     * Light or Dark or System
     */
    val theme: AppTheme,

    /**
     * Android 12+ dynamic theming
     */
    val isUsingDynamicColor: Boolean,

    /**
     * Solid or blur player theme
     */
    val playerTheme: PlayerTheme,

    /**
     * Useful for Amoled Screens
     */
    val blackBackgroundForDarkTheme: Boolean,

    /**
     * Whether to pin MiniPlayer or show it as a FAB
     */
    val miniPlayerMode: MiniPlayerMode = MiniPlayerMode.PINNED,

    /**
     * Color used as a primary color in the application.
     * The most significant byte is ignored. 0xIIRRGGBB
     */
    val accentColor: Int = DEFAULT_ACCENT_COLOR,

    /**
     * Show next and previous buttons in MiniPlayer
     */
    val showMiniPlayerExtraControls: Boolean = false,

    val lyricsOffsetMs: Int = 0,
    val lyricsFontSize: String = "MEDIUM",
    val backgroundBlur: Int = 60,
    val keepScreenOn: Boolean = false,
)

enum class PlayerTheme {
    SOLID, BLUR
}

const val DEFAULT_ACCENT_COLOR = (0x002f64ba).or(0xFF shl 24)