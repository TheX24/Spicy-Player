package com.tx24.spicyplayer.lyrics.spicy.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class GraphemeSegmenterTest {
    @Test
    fun `fallback preserves combining marks variation selectors and zwj emoji`() {
        assertEquals(listOf("e\u0301"), GraphemeSegmenter.segment("e\u0301", sdkInt = 23))
        assertEquals(listOf("✌️"), GraphemeSegmenter.segment("✌️", sdkInt = 23))
        assertEquals(listOf("👩🏽‍🎤"), GraphemeSegmenter.segment("👩🏽‍🎤", sdkInt = 23))
        assertEquals(listOf("🇮🇹"), GraphemeSegmenter.segment("🇮🇹", sdkInt = 23))
    }
}
