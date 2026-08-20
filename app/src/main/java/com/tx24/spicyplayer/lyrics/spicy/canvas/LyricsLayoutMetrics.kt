package com.tx24.spicyplayer.lyrics.spicy.canvas

import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType

internal data class ContentSlot(val startPx: Float, val widthPx: Float)

internal data class LyricsLayoutMetrics(
    val viewportWidthPx: Float,
    val density: Float,
    val lyricsType: LyricsType,
    val appFontScale: Float,
) {
    val viewportWidthDp = viewportWidthPx / density.coerceAtLeast(0.01f)
    val baseFontSizeSp = when (lyricsType) {
        LyricsType.Static -> (viewportWidthDp * 0.05f).coerceIn(12.8f, 40f)
        else -> (viewportWidthDp * 0.07f).coerceIn(29.6f, 56f)
    } * appFontScale
    val lineGapPx = viewportWidthPx * 0.01f
    val lineHeightMultiplier = 1.1818182f

    fun lineHeightPx(fontSizeSp: Float): Float = fontSizeSp * density * lineHeightMultiplier

    fun contentSlot(hasDuet: Boolean, isRtl: Boolean, oppositeAligned: Boolean): ContentSlot {
        if (!hasDuet) {
            return if (isRtl) ContentSlot(viewportWidthPx * 0.05f, viewportWidthPx * 0.95f)
            else ContentSlot(0f, viewportWidthPx * 0.95f)
        }
        val startsAtInset = if (isRtl) !oppositeAligned else oppositeAligned
        return ContentSlot(if (startsAtInset) viewportWidthPx * 0.15f else 0f, viewportWidthPx * 0.85f)
    }
}
