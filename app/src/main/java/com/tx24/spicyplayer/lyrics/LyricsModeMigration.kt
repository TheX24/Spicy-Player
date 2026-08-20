package com.tx24.spicyplayer.lyrics

import com.tx24.spicyplayer.lyrics.spicy.SimpleAnimationStyle

internal const val LYRICS_MODE_MIGRATION_VERSION = 1

internal data class LyricsModeOptions(
    val simple: Boolean,
    val minimal: Boolean,
    val animationStyle: SimpleAnimationStyle = SimpleAnimationStyle.CALCULATE,
)

internal fun migrateLegacyLyricsMode(mode: String?): LyricsModeOptions = when (mode?.uppercase()) {
    "SIMPLE" -> LyricsModeOptions(simple = true, minimal = false)
    "MINIMAL" -> LyricsModeOptions(simple = false, minimal = true)
    else -> LyricsModeOptions(simple = false, minimal = false)
}
