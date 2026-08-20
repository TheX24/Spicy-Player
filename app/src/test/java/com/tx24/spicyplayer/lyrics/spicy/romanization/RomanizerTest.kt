package com.tx24.spicyplayer.lyrics.spicy.romanization

import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.Word
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RomanizerTest {

    @Test
    fun `korean revised romanization`() {
        assertEquals("annyeonghaseyo", KoreanRomanizer.romanize("안녕하세요"))
        assertEquals("sarang", KoreanRomanizer.romanize("사랑"))
    }

    @Test
    fun `cyrillic transliteration`() {
        assertEquals("privet", CyrillicRomanizer.romanize("привет"))
        assertEquals("Moskva", CyrillicRomanizer.romanize("Москва"))
    }

    @Test
    fun `greek transliteration`() {
        assertEquals("kalimera", GreekRomanizer.romanize("καλημέρα"))
    }

    @Test
    fun `kana to romaji handles youon and sokuon`() {
        assertEquals("konnichiha", KanaRomanizer.romanize("こんにちは"))
        assertEquals("kya", KanaRomanizer.romanize("きゃ"))
        assertEquals("kitto", KanaRomanizer.romanize("きっと"))
    }

    @Test
    fun `script detection prefers japanese over chinese when kana present`() {
        assertEquals(Script.JAPANESE, ScriptDetector.detect("君と歩いた").firstOrNull())
        assertEquals(Script.CHINESE, ScriptDetector.detect("我爱你").firstOrNull())
        assertEquals(Script.KOREAN, ScriptDetector.detect("사랑해").firstOrNull())
    }

    @Test
    fun `service fills gaps but never overwrites supplied romanization`() = runBlocking {
        val lines = listOf(
            Line(
                words = listOf(
                    Word("안녕", 0L, 1000L),
                    Word("사랑", 1000L, 2000L, romanizedText = "SUPPLIED"),
                ),
                startMs = 0L,
            )
        )
        val result = RomanizationService.romanize(lines)
        val words = result.single().words
        assertEquals("annyeong", words[0].romanizedText)
        assertEquals("SUPPLIED", words[1].romanizedText) // supplied value preserved
    }

    @Test
    fun `latin text is left unchanged`() {
        assertTrue(ScriptDetector.detect("hello world").isEmpty())
    }

    @Test
    fun `service composes every detected romanizer in priority order`() = runBlocking {
        val line = Line(
            words = listOf(Word("Привет κόσμος", 0L, 1_000L)),
            startMs = 0L,
        )
        val out = RomanizationService.romanize(listOf(line)).single().words.single().romanizedText
        assertTrue(out != null && out.none { it in '\u0370'..'\u052F' })
    }
}
