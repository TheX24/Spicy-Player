package com.tx24.spicyplayer.lyrics.spicy.canvas

import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsLayoutMetricsTest {
    @Test
    fun `equal logical widths produce equal type sizes at every density`() {
        for (density in listOf(1f, 2f, 3f)) {
            val synced = LyricsLayoutMetrics(360f * density, density, LyricsType.Syllable, 1f)
            val static = LyricsLayoutMetrics(360f * density, density, LyricsType.Static, 1f)
            assertEquals(29.6f, synced.baseFontSizeSp, 0.001f)
            assertEquals(18f, static.baseFontSizeSp, 0.001f)
            assertEquals(3.6f * density, synced.lineGapPx, 0.001f)
        }
    }

    @Test
    fun `reference clamps and duet content slots are exact`() {
        assertEquals(56f, LyricsLayoutMetrics(840f, 1f, LyricsType.Line, 1f).baseFontSizeSp, 0f)
        assertEquals(40f, LyricsLayoutMetrics(840f, 1f, LyricsType.Static, 1f).baseFontSizeSp, 0f)
        val metrics = LyricsLayoutMetrics(600f, 1f, LyricsType.Syllable, 1f)
        assertEquals(ContentSlot(30f, 540f), metrics.contentSlot(false, false, false))
        assertEquals(ContentSlot(90f, 480f), metrics.contentSlot(true, false, true))
        assertEquals(ContentSlot(30f, 480f), metrics.contentSlot(true, true, true))
        assertEquals(1.1818182f, metrics.lineHeightMultiplier, 0f)
    }
}
