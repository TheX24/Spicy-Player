package com.tx24.spicyplayer.lyrics.spicy.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LyricsDocumentIdTest {
    @Test
    fun `document id combines identity fields with deterministic raw hash`() {
        val first = lyricsDocumentId("content://song/1", "LRCLIB", "[00:01]hello")
        assertEquals(first, lyricsDocumentId("content://song/1", "LRCLIB", "[00:01]hello"))
        assertNotEquals(first, lyricsDocumentId("content://song/1", "LRCLIB", "[00:02]hello"))
        assertNotEquals(first, lyricsDocumentId("content://song/2", "LRCLIB", "[00:01]hello"))
    }
}
