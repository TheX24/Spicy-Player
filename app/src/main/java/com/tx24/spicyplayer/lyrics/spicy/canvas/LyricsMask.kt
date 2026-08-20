package com.tx24.spicyplayer.lyrics.spicy.canvas

internal data class LyricsMaskStops(
    val outerTop: Float,
    val innerTop: Float,
    val innerBottom: Float,
    val outerBottom: Float,
)

internal fun lyricsMaskStops(heightPx: Float, density: Float): LyricsMaskStops {
    if (heightPx <= 0f) return LyricsMaskStops(0f, 0f, 1f, 1f)
    val outerPx = (16f * density).coerceAtMost(heightPx / 2f)
    val innerPx = (64f * density).coerceAtMost(heightPx / 2f)
    return LyricsMaskStops(
        outerTop = outerPx / heightPx,
        innerTop = innerPx / heightPx,
        innerBottom = 1f - innerPx / heightPx,
        outerBottom = 1f - outerPx / heightPx,
    )
}
