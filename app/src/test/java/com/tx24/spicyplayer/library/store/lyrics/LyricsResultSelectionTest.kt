package com.tx24.spicyplayer.library.store.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsResultSelectionTest {

    @Test
    fun `returns synchronized lyrics when both formats are available`() {
        val result = lyricsResultFromStrings(
            plainLyrics = "Plain lyrics",
            syncedLyrics = "[00:01.00]Synced lyrics"
        )

        assertTrue(result is LyricsResult.FoundSyncedLyrics)
    }

    @Test
    fun `returns plain lyrics when synchronized lyrics are unavailable`() {
        val result = lyricsResultFromStrings(
            plainLyrics = "First line\nSecond line",
            syncedLyrics = null
        )

        assertTrue(result is LyricsResult.FoundPlainLyrics)
        assertEquals(
            listOf("First line", "Second line"),
            (result as LyricsResult.FoundPlainLyrics).plainLyrics.lines
        )
    }

    @Test
    fun `falls back to plain lyrics when synchronized lyrics are invalid`() {
        val result = lyricsResultFromStrings(
            plainLyrics = "Plain lyrics",
            syncedLyrics = "Not synchronized"
        )

        assertTrue(result is LyricsResult.FoundPlainLyrics)
    }

    @Test
    fun `returns no result when both formats are empty`() {
        assertNull(lyricsResultFromStrings(plainLyrics = null, syncedLyrics = null))
        assertNull(lyricsResultFromStrings(plainLyrics = "  ", syncedLyrics = ""))
    }
}
