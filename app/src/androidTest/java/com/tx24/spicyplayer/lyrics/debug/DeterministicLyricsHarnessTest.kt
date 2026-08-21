package com.tx24.spicyplayer.lyrics.debug

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.tx24.spicyplayer.lyrics.spicy.SimpleAnimationStyle
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsDocument
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsFooter
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsProvenance
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType
import com.tx24.spicyplayer.lyrics.spicy.models.Word
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeterministicLyricsHarnessTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun paddedLyricsAndLongFooterStayInsideViewportAndWrap() {
        composeRule.mainClock.autoAdvance = false
        val document = LyricsDocument(
            lines = listOf(
                Line(listOf(Word("A deterministic lyric line", 0L, 5_000L)), 0L, 5_000L),
            ),
            footer = LyricsFooter(
                songwriters = listOf(
                    "Jaylah Ji'mya Hickmon", "Jatavia Shakara Johnson",
                    "Za'Miya Lyrica Smith", "A deliberately long final songwriter name",
                ),
                provenance = LyricsProvenance("FROM_LOCAL_FILE"),
            ),
            type = LyricsType.Static,
            documentId = "golden:footer-wrap",
        )
        composeRule.setContent {
            DeterministicLyricsHarness(
                DebugLyricsHarnessState(
                    document = document,
                    timestampMs = 2_500L,
                    viewportWidthDp = 360,
                    viewportHeightDp = 800,
                    simpleLyricsMode = true,
                    minimalLyricsMode = true,
                    simpleAnimationStyle = SimpleAnimationStyle.CALCULATE,
                ),
            )
        }
        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.waitForIdle()

        val image = composeRule.onRoot().captureToImage()
        val pixels = image.toPixelMap()
        var minX = image.width
        var maxX = 0
        val occupiedRows = BooleanArray(image.height)
        for (y in 0 until image.height) {
            var rowPixels = 0
            for (x in 0 until image.width) {
                val color = pixels[x, y]
                if (color.red + color.green + color.blue > 0.18f) {
                    minX = minOf(minX, x)
                    maxX = maxOf(maxX, x)
                    rowPixels++
                }
            }
            occupiedRows[y] = rowPixels >= 4
        }
        var bands = 0
        var occupied = false
        var occupiedRowCount = 0
        for (row in occupiedRows) {
            if (row && !occupied) bands++
            if (row) occupiedRowCount++
            occupied = row
        }

        assertTrue("leftmost pixel $minX", minX >= image.width * 0.03f)
        assertTrue("rightmost pixel $maxX", maxX <= image.width * 0.97f)
        assertTrue("expected lyric, writer, and provenance blocks, found $bands", bands >= 3)
        assertTrue(
            "wrapped footer did not occupy enough vertical pixels: $occupiedRowCount",
            occupiedRowCount >= image.height * 0.035f,
        )
    }
}
