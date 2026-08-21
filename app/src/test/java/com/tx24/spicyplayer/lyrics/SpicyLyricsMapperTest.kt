package com.tx24.spicyplayer.lyrics

import com.tx24.spicyplayer.lyrics.spicy.models.buildDisplayTimeline
import com.tx24.spicyplayer.model.lyrics.SynchronizedLyrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpicyLyricsMapperTest {
    @Test
    fun `bare lrc holds until next timestamp without fabricated mid-song interludes`() {
        val lyrics = SynchronizedLyrics.fromString("[00:04.00]One\n[00:08.00]Two")!!
        val lines = lyrics.toSpicyLines()

        assertEquals(8_000L, lines.first().endMs)
        assertEquals(8_000L, lines.last().endMs)
        assertFalse(lines.any { it.isInterlude })
        assertTrue(buildDisplayTimeline(lines, minimalMode = false).single { it.isInterlude }.endMs == 4_000L)
    }
}
