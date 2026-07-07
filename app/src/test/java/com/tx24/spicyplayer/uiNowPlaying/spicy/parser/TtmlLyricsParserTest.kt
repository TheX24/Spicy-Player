package com.tx24.spicyplayer.uiNowPlaying.spicy.parser

import com.tx24.spicyplayer.uiNowPlaying.spicy.models.ParsedLyrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtmlLyricsParserTest {

    private fun parse(ttml: String): ParsedLyrics =
        TtmlLyricsParser.parse(ttml.byteInputStream())

    private val duetTtml = """
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
          <head>
            <metadata>
              <ttm:agent type="person" xml:id="v1"/>
              <ttm:agent type="other" xml:id="v2"/>
              <songwriters>
                <songwriter>Jane Doe</songwriter>
                <songwriter>John Smith</songwriter>
              </songwriters>
            </metadata>
          </head>
          <body>
            <div begin="0:10.000" end="0:30.000">
              <p begin="0:10.000" end="0:12.000" ttm:agent="v1"><span begin="0:10.000" end="0:10.500">Hel-</span><span begin="0:10.500" end="0:11.000">lo</span> <span begin="0:11.200" end="0:12.000">world</span></p>
              <p begin="0:16.000" end="0:18.000" ttm:agent="v2"><span begin="0:16.000" end="0:17.500">Yeah</span></p>
            </div>
          </body>
        </tt>
    """.trimIndent()

    @Test
    fun `parses lines words and timings from duet ttml`() {
        val result = parse(duetTtml)
        val vocalLines = result.lines.filter { !it.isInterlude && !it.isSongwriter }
        assertEquals(2, vocalLines.size)

        val (first, second) = vocalLines
        assertEquals(10_000L, first.startMs)
        assertEquals(12_000L, first.endMs)
        assertEquals(listOf("Hel-", "lo", "world"), first.words.map { it.text })
        assertEquals(10_000L, first.words[0].startMs)
        assertEquals(10_500L, first.words[0].endMs)

        assertEquals(16_000L, second.startMs)
        assertEquals(17_500L, second.endMs)
    }

    @Test
    fun `syllable continuation is attached to the previous token`() {
        val words = parse(duetTtml).lines.first { it.words.isNotEmpty() && !it.isSongwriter }.words
        // "Hel-" starts the word, "lo" continues it mid-word, "world" follows a space.
        assertFalse(words[0].isPartOfWord)
        assertTrue(words[1].isPartOfWord)
        assertFalse(words[2].isPartOfWord)
    }

    @Test
    fun `guest agent lines are opposite aligned and primary lines are not`() {
        val vocalLines = parse(duetTtml).lines.filter { !it.isInterlude && !it.isSongwriter }
        val v1Line = vocalLines.first { it.agent == "v1" }
        val v2Line = vocalLines.first { it.agent == "v2" }
        assertFalse(v1Line.oppositeAligned)
        assertTrue(v2Line.oppositeAligned)
    }

    @Test
    fun `words held one second or longer become letter groups`() {
        val vocalLines = parse(duetTtml).lines.filter { !it.isInterlude && !it.isSongwriter }
        // "Yeah" is held 1500ms -> letter-by-letter timing, evenly split at 375ms.
        val yeah = vocalLines.first { it.agent == "v2" }.words.single()
        assertTrue(yeah.isLetterGroup)
        assertEquals(4, yeah.letters.size)
        assertEquals("Y", yeah.letters[0].char)
        assertEquals(16_000L, yeah.letters[0].startMs)
        assertEquals(16_375L, yeah.letters[0].endMs)
        assertEquals(17_500L, yeah.letters[3].endMs)

        // "world" is held only 800ms -> plain word.
        val world = vocalLines.first { it.agent == "v1" }.words[2]
        assertFalse(world.isLetterGroup)
        assertTrue(world.letters.isEmpty())
    }

    @Test
    fun `interludes are injected for gaps of three seconds or more`() {
        val lines = parse(duetTtml).lines
        val interludes = lines.filter { it.isInterlude }
        assertEquals(2, interludes.size)

        // Initial gap: nothing sung until 10s.
        assertEquals(0L, interludes[0].startMs)
        assertEquals(10_000L, interludes[0].endMs)
        // Mid-song gap: 12s -> 16s.
        assertEquals(12_000L, interludes[1].startMs)
        assertEquals(16_000L, interludes[1].endMs)

        // Lines come back sorted by start time.
        assertEquals(lines.map { it.startMs }.sorted(), lines.map { it.startMs })
    }

    @Test
    fun `songwriter credits become a trailing songwriter line`() {
        val result = parse(duetTtml)
        assertEquals(listOf("Jane Doe", "John Smith"), result.songwriters)

        val credits = result.lines.single { it.isSongwriter }
        assertEquals(17_500L, credits.startMs)
        assertEquals(
            "Written by Jane Doe, John Smith",
            credits.words.joinToString(" ") { it.text }
        )
    }

    @Test
    fun `background vocals split into their own line with parentheses stripped`() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body><div begin="0:01.000" end="0:03.000">
                <p begin="0:01.000" end="0:03.000" ttm:agent="v1"><span begin="0:01.000" end="0:02.000">Lead</span> <span ttm:role="x-bg" begin="0:02.000" end="0:03.000">(Echo)</span></p>
              </div></body>
            </tt>
        """.trimIndent()

        val lines = parse(ttml).lines
        assertEquals(2, lines.size)

        val lead = lines.single { !it.isBackground }
        val background = lines.single { it.isBackground }
        assertEquals(listOf("Lead"), lead.words.map { it.text })
        assertEquals(listOf("Echo"), background.words.map { it.text })
        assertEquals(2_000L, background.startMs)
    }

    @Test
    fun `supports all ttml time formats`() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body><div>
                <p begin="7" end="8">A</p>
                <p begin="65.25" end="66">B</p>
                <p begin="1:05.5" end="1:06">C</p>
                <p begin="01:02:03.004" end="01:02:04">D</p>
                <p begin="2.5s" end="3s">E</p>
              </div></body>
            </tt>
        """.trimIndent()

        val byText = parse(ttml).lines
            .filter { !it.isInterlude }
            .associate { it.words.single().text to it.startMs }

        assertEquals(7_000L, byText["A"])
        assertEquals(65_250L, byText["B"])
        assertEquals(65_500L, byText["C"])
        assertEquals(3_723_004L, byText["D"])
        assertEquals(2_500L, byText["E"])
    }

    @Test
    fun `unparseable timestamps fall back to zero instead of throwing`() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body><div>
                <p begin="abc" end="def">X</p>
              </div></body>
            </tt>
        """.trimIndent()

        val line = parse(ttml).lines.single { !it.isInterlude }
        assertEquals(0L, line.startMs)
        assertEquals(listOf("X"), line.words.map { it.text })
    }

    @Test
    fun `empty document yields no lines`() {
        val result = parse("""<tt xmlns="http://www.w3.org/ns/ttml"><body/></tt>""")
        assertTrue(result.lines.isEmpty())
        assertTrue(result.songwriters.isEmpty())
    }
}
