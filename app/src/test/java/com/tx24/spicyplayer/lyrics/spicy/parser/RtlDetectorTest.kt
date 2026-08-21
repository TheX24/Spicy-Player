package com.tx24.spicyplayer.lyrics.spicy.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RtlDetectorTest {
    @Test
    fun `direction follows the first strong code point`() {
        assertTrue(RtlDetector.isRtl("... العربية English"))
        assertFalse(RtlDetector.isRtl("... English العربية"))
        assertTrue(RtlDetector.isRtl("ࢠ test")) // Arabic Extended-A U+08A0
        assertTrue(RtlDetector.isRtl("עברית"))
        assertFalse(RtlDetector.isRtl("1234!?"))
    }
}
