package com.tx24.spicyplayer.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsOffsetMigrationTest {
    @Test
    fun `legacy offsets are negated exactly once`() {
        assertEquals(-240, migrateLyricsOffset(240, storedVersion = 0))
        assertEquals(240, migrateLyricsOffset(240, storedVersion = LYRICS_OFFSET_CONVENTION_VERSION))
    }
}
