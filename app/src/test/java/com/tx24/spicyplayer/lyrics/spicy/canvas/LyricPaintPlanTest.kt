package com.tx24.spicyplayer.lyrics.spicy.canvas

import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import com.tx24.spicyplayer.lyrics.spicy.animation.ElementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LyricPaintPlanTest {
    @Test
    fun `active is gradient while upcoming and completed are shadow only`() {
        assertSame(LyricPaintPlan.ActiveGradient, lyricPaintPlan(ElementState.Active, false, 1f, 0f, RenderConfig.FULL))
        assertEquals(
            LyricPaintPlan.InactiveShadow(0.25f, 3f),
            lyricPaintPlan(ElementState.NotSung, false, 0.5f, 3f, RenderConfig.FULL),
        )
        assertEquals(
            LyricPaintPlan.InactiveShadow(0.425f, 3f),
            lyricPaintPlan(ElementState.Sung, false, 0.5f, 3f, RenderConfig.FULL),
        )
    }

    @Test
    fun `background shadows use their reference alphas`() {
        assertEquals(0.3f, (lyricPaintPlan(ElementState.NotSung, true, 1f, 0f, RenderConfig.FULL) as LyricPaintPlan.InactiveShadow).alpha, 0f)
        assertEquals(0.6f, (lyricPaintPlan(ElementState.Sung, true, 1f, 0f, RenderConfig.FULL) as LyricPaintPlan.InactiveShadow).alpha, 0f)
    }
}
