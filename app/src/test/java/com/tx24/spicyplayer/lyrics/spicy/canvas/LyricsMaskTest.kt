package com.tx24.spicyplayer.lyrics.spicy.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsMaskTest {
    @Test
    fun `mask uses symmetric sixteen to sixty four dp ramps`() {
        val one = lyricsMaskStops(400f, 1f)
        assertEquals(0.04f, one.outerTop, 0.0001f)
        assertEquals(0.16f, one.innerTop, 0.0001f)
        assertEquals(0.84f, one.innerBottom, 0.0001f)
        assertEquals(0.96f, one.outerBottom, 0.0001f)
        val two = lyricsMaskStops(400f, 2f)
        assertEquals(0.08f, two.outerTop, 0.0001f)
        assertEquals(0.32f, two.innerTop, 0.0001f)
        assertEquals(0.68f, two.innerBottom, 0.0001f)
        assertEquals(0.92f, two.outerBottom, 0.0001f)
    }

    @Test
    fun `short panes keep monotonic symmetric stops`() {
        val stops = lyricsMaskStops(100f, 3f)
        assertTrue(stops.outerTop <= stops.innerTop)
        assertEquals(stops.innerTop, stops.innerBottom, 0f)
        assertEquals(1f - stops.outerTop, stops.outerBottom, 0f)
    }
}
