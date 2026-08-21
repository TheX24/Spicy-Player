package com.tx24.spicyplayer.model.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SynchronizedLyricsTest {

    @Test
    fun `fractional lrc timestamps use decimal precision`() {
        val lyrics = SynchronizedLyrics.fromString(
            """
            [00:01.5]one digit
            [00:02.05]two digits
            [00:03.500]three digits
            [00:04]no fraction
            """.trimIndent()
        )

        assertNotNull(lyrics)
        assertEquals(listOf(1_500, 2_050, 3_500, 4_000), lyrics!!.segments.map { it.durationMillis })
    }
}
