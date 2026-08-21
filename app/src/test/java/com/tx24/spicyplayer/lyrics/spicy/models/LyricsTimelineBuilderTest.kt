package com.tx24.spicyplayer.lyrics.spicy.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTimelineBuilderTest {

    private fun line(
        start: Long,
        end: Long,
        role: LineRole = LineRole.LEAD,
        groupId: Int,
    ) = Line(
        words = listOf(Word("line", start, end)),
        startMs = start,
        endMs = end,
        role = role,
        groupId = groupId,
    )

    @Test
    fun `interludes use lead group lifetime and ignore background timing`() {
        val lines = listOf(
            line(1_000, 2_000, groupId = 0),
            line(1_500, 5_500, role = LineRole.BACKGROUND, groupId = 0),
            line(6_000, 7_000, groupId = 1),
        )

        val timeline = buildDisplayTimeline(lines, minimalMode = false)
        val interlude = timeline.single { it.role == LineRole.INTERLUDE }

        assertEquals(2_000L, interlude.startMs)
        assertEquals(6_000L, interlude.endMs)
    }

    @Test
    fun `minimal mode raises interlude threshold from three to five seconds`() {
        val lines = listOf(
            line(1_000, 2_000, groupId = 0),
            line(6_000, 7_000, groupId = 1),
        )

        assertTrue(buildDisplayTimeline(lines, minimalMode = false).any { it.isInterlude })
        assertFalse(buildDisplayTimeline(lines, minimalMode = true).any { it.isInterlude })
    }

    @Test
    fun `leading interlude is synthesized from zero to first lead`() {
        val timeline = buildDisplayTimeline(
            listOf(line(5_000, 6_000, groupId = 0)),
            minimalMode = false,
        )

        val interlude = timeline.single { it.isInterlude }
        assertEquals(0L, interlude.startMs)
        assertEquals(5_000L, interlude.endMs)
    }
}
