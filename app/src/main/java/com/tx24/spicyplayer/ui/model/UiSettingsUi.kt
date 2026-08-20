package com.tx24.spicyplayer.ui.model


import androidx.compose.runtime.Stable
import com.tx24.spicyplayer.model.prefs.AppTheme
import com.tx24.spicyplayer.model.prefs.DEFAULT_ACCENT_COLOR
import com.tx24.spicyplayer.model.prefs.PlayerTheme
import com.tx24.spicyplayer.model.prefs.UiSettings
import com.tx24.spicyplayer.lyrics.spicy.SimpleAnimationStyle


@Stable
data class UiSettingsUi(
    val theme: AppThemeUi = AppThemeUi.SYSTEM,
    val isUsingDynamicColor: Boolean = false,
    val playerThemeUi: PlayerThemeUi = PlayerThemeUi.SOLID,
    val blackBackgroundForDarkTheme: Boolean = false,
    val accentColor: Int = DEFAULT_ACCENT_COLOR,
    val showMiniPlayerExtraControls: Boolean = false,
    val lyricsOffsetMs: Int = 0,
    val lyricsFontSize: String = "MEDIUM",
    val backgroundBlur: Int = 60,
    val keepScreenOn: Boolean = false,
    val simpleLyricsMode: Boolean = false,
    val minimalLyricsMode: Boolean = false,
    val simpleAnimationStyle: SimpleAnimationStyle = SimpleAnimationStyle.CALCULATE,
    val lyricsBackgroundEngine: String = "AUTO",
    val lyricsRomanize: Boolean = false,
)

@Stable
enum class PlayerThemeUi {
    SOLID, BLUR
}

fun PlayerThemeUi.toPlayerTheme() =
    PlayerTheme.valueOf(this.toString())

fun PlayerTheme.toPlayerThemeUi() =
    PlayerThemeUi.valueOf(this.toString())



@Stable
enum class AppThemeUi {
    SYSTEM, LIGHT, DARK
}

fun AppTheme.toAppThemeUi() =
    AppThemeUi.valueOf(this.toString())

fun AppThemeUi.toAppTheme() =
    AppTheme.valueOf(this.toString())

fun UiSettings.toUiSettingsUi() =
    UiSettingsUi(
        theme.toAppThemeUi(),
        isUsingDynamicColor,
        playerTheme.toPlayerThemeUi(),
        blackBackgroundForDarkTheme,
        accentColor,
        showMiniPlayerExtraControls,
        lyricsOffsetMs,
        lyricsFontSize,
        backgroundBlur,
        keepScreenOn,
        simpleLyricsMode,
        minimalLyricsMode,
        simpleAnimationStyle,
        lyricsBackgroundEngine,
        lyricsRomanize
    )
