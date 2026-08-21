package com.tx24.spicyplayer.lyrics.spicy.canvas

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutGenerationGateTest {
    @Test
    fun `only the newest asynchronous layout generation is accepted`() {
        val gate = LayoutGenerationGate()
        val stale = gate.next()
        val current = gate.next()
        assertFalse(gate.isCurrent(stale))
        assertTrue(gate.isCurrent(current))
    }
}
