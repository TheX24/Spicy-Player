package com.tx24.spicyplayer.lyrics

import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsModeMigrationTest {
    @Test
    fun `legacy enum maps to independent switches`() {
        assertEquals(LyricsModeOptions(false, false), migrateLegacyLyricsMode("FULL"))
        assertEquals(LyricsModeOptions(true, false), migrateLegacyLyricsMode("SIMPLE"))
        assertEquals(LyricsModeOptions(false, true), migrateLegacyLyricsMode("MINIMAL"))
    }

    @Test
    fun `simple and minimal combine while compact scope suppresses minimal visuals`() {
        val combined = RenderConfig.create(true, true)
        assertTrue(combined.isSimple)
        assertTrue(combined.isMinimal)
        assertTrue(combined.distanceBlurEnabled)
        assertTrue(combined.lettersEnabled)
        val compact = RenderConfig.create(true, true, compact = true)
        assertTrue(compact.isSimple)
        assertFalse(compact.isMinimal)
    }
}
