package com.tx24.spicyplayer.lyrics.spicy.parser

import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterSynthesizerTest {

    private fun lineOf(word: Word) = Line(words = listOf(word), startMs = word.startMs)

    @Test
    fun `long word becomes a letter group with the 250ms breather`() {
        // "Yeah" held 1500ms; letters span [16000, 17250] (end - 250) split evenly.
        val word = Word("Yeah", 16_000L, 17_500L)
        val result = LetterSynthesizer.apply(listOf(lineOf(word)), RenderConfig.FULL, romanized = false)
        val out = result.single().words.single()

        assertTrue(out.isLetterGroup)
        assertEquals(4, out.letters.size)
        assertEquals("Y", out.letters[0].char)
        assertEquals(16_000L, out.letters[0].startMs)
        // (17250 - 16000) / 4 = 312.5ms per letter.
        assertEquals(16_312L, out.letters[0].endMs)
        assertEquals(17_250L, out.letters[3].endMs)
    }

    @Test
    fun `short word stays plain`() {
        val word = Word("world", 0L, 800L) // under the 1000ms threshold
        val out = LetterSynthesizer.apply(listOf(lineOf(word)), RenderConfig.FULL, romanized = false)
            .single().words.single()
        assertFalse(out.isLetterGroup)
        assertTrue(out.letters.isEmpty())
    }

    @Test
    fun `punctuation is included as its own animated letter, not excluded`() {
        // The reference (Emphasize.ts) gives every character Emphasis:true with no exclusion
        // for punctuation, so a held word's punctuation should still occupy its own letter slot.
        val word = Word("hey!", 0L, 2000L)
        val out = LetterSynthesizer.apply(listOf(lineOf(word)), RenderConfig.FULL, romanized = false)
            .single().words.single()
        assertTrue(out.isLetterGroup)
        assertEquals(4, out.letters.size)
        assertEquals("!", out.letters[3].char)
    }

    @Test
    fun `simple mode caps letter groups at 12 characters`() {
        val word = Word("supercalifragi", 0L, 2000L) // 14 chars
        val full = LetterSynthesizer.apply(listOf(lineOf(word)), RenderConfig.FULL, romanized = false)
            .single().words.single()
        val simple = LetterSynthesizer.apply(listOf(lineOf(word)), RenderConfig.SIMPLE, romanized = false)
            .single().words.single()
        assertTrue(full.isLetterGroup)
        assertFalse(simple.isLetterGroup)
    }

    @Test
    fun `minimal mode disables letters entirely`() {
        val word = Word("Yeah", 16_000L, 17_500L)
        val out = LetterSynthesizer.apply(listOf(lineOf(word)), RenderConfig.MINIMAL, romanized = false)
            .single().words.single()
        assertFalse(out.isLetterGroup)
    }

    @Test
    fun `romanized display drives letter splitting`() {
        // Original single CJK char, romanized to a multi-letter reading.
        val word = Word("君", 0L, 2000L, romanizedText = "kimi")
        val out = LetterSynthesizer.apply(listOf(lineOf(word)), RenderConfig.FULL, romanized = true)
            .single().words.single()
        assertTrue(out.isLetterGroup)
        assertEquals(4, out.letters.size)
        assertEquals("k", out.letters[0].char)
    }
}
