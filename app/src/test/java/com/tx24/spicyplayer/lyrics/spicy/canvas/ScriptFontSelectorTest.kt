package com.tx24.spicyplayer.lyrics.spicy.canvas

import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptFontSelectorTest {
    @Test
    fun `routes Arabic Extended A and Georgian to bundled fallbacks`() {
        assertEquals(LyricScriptFont.VAZIRMATN, ScriptFontSelector.select("English ࢠ"))
        assertEquals(LyricScriptFont.NOTO_SANS_GEORGIAN, ScriptFontSelector.select("ქართული"))
        assertEquals(LyricScriptFont.DEFAULT, ScriptFontSelector.select("English 日本語"))
    }
}
