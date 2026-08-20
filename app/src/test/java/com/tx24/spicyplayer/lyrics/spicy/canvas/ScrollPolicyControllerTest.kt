package com.tx24.spicyplayer.lyrics.spicy.canvas

import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.LineRole
import com.tx24.spicyplayer.lyrics.spicy.models.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollPolicyControllerTest {
    private fun line(start: Long, end: Long, role: LineRole = LineRole.LEAD, group: Int? = null) =
        Line(listOf(Word("x", start, end)), start, end, role = role, groupId = group)

    @Test
    fun `background activity anchors its lead line`() {
        val lines = listOf(line(0, 1_000, group = 1), line(500, 2_000, LineRole.BACKGROUND, 1))
        assertEquals(0, ScrollPolicyController.selectTargetIndex(lines, 1_500))
    }

    @Test
    fun `initial positioning and large seeks snap while ordinary line changes smooth`() {
        val lines = listOf(line(0, 900, group = 0), line(1_000, 1_900, group = 1), line(5_000, 6_000, group = 2))
        val policy = ScrollPolicyController()
        assertEquals(ScrollMotion.SNAP, policy.decide(lines, 100).motion)
        assertEquals(ScrollMotion.SMOOTH, policy.decide(lines, 1_100).motion)
        assertEquals(ScrollMotion.SNAP, policy.decide(lines, 5_100).motion)
        assertEquals(ScrollMotion.SNAP, policy.decide(lines, 0).motion)
    }

    @Test
    fun `post interlude target is held for 240 milliseconds`() {
        val lines = listOf(
            Line(emptyList(), 0, 3_000, role = LineRole.INTERLUDE),
            line(3_000, 4_000, group = 1),
        )
        assertEquals(0, ScrollPolicyController.selectTargetIndex(lines, 3_200))
        assertEquals(1, ScrollPolicyController.selectTargetIndex(lines, 3_240))
    }

    @Test
    fun `fling decay is refresh rate independent`() {
        val at60 = ScrollPolicyController.flingDecayMultiplier(1f / 60f)
        val at120Twice = ScrollPolicyController.flingDecayMultiplier(1f / 120f).let { it * it }
        assertTrue(kotlin.math.abs(at60 - at120Twice) < 0.0001f)
        assertEquals(0.95f, at60, 0.0001f)
    }

    @Test
    fun `active lead anchor is viewport center minus thirty logical dp`() {
        assertEquals(570f, ScrollPolicyController.anchorY(1_200f, density = 1f), 0f)
        assertEquals(510f, ScrollPolicyController.anchorY(1_200f, density = 3f), 0f)
    }
}
