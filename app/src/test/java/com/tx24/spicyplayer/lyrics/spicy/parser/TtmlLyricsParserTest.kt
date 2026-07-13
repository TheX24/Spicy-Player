package com.tx24.spicyplayer.lyrics.spicy.parser

import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType
import com.tx24.spicyplayer.lyrics.spicy.models.ParsedLyrics
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
    fun `parser emits plain words - letter synthesis is deferred to render time`() {
        val vocalLines = parse(duetTtml).lines.filter { !it.isInterlude && !it.isSongwriter }
        // The parser no longer synthesizes letter groups; that is done by LetterSynthesizer
        // using the active RenderConfig. Every word should be plain here.
        vocalLines.flatMap { it.words }.forEach {
            assertFalse(it.isLetterGroup)
            assertTrue(it.letters.isEmpty())
        }
    }

    @Test
    fun `word-timed ttml is detected as syllable type`() {
        assertEquals(LyricsType.Syllable, parse(duetTtml).type)
    }

    @Test
    fun `ttml without span timing is detected as line type`() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body><div>
                <p begin="0:01.000" end="0:03.000">Just a whole line</p>
                <p begin="0:03.000" end="0:05.000">And another line</p>
              </div></body>
            </tt>
        """.trimIndent()
        assertEquals(LyricsType.Line, parse(ttml).type)
    }

    @Test
    fun `apple-style translation with a Latn language tag is treated as romanization`() {
        // Apple's real lyric TTML has no dedicated transliteration element — both actual
        // translations and romanizations arrive as <translation xml:lang="…">, disambiguated
        // only by whether the language tag targets a Latin script (e.g. "ja-Latn").
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
              <head><metadata>
                <itunes:translations>
                  <itunes:translation type="replacement" xml:lang="ja-Latn">
                    <itunes:text for="L1">
                      <itunes:span begin="0:01.000" end="0:01.500">Hito</itunes:span>
                      <itunes:span begin="0:01.500" end="0:02.000">ni</itunes:span>
                    </itunes:text>
                  </itunes:translation>
                  <itunes:translation type="replacement" xml:lang="en">
                    <itunes:text for="L1">There are people</itunes:text>
                  </itunes:translation>
                </itunes:translations>
              </metadata></head>
              <body><div begin="0:01.000" end="0:02.000">
                <p begin="0:01.000" end="0:02.000" itunes:key="L1"><span begin="0:01.000" end="0:01.500">人</span> <span begin="0:01.500" end="0:02.000">に</span></p>
              </div></body>
            </tt>
        """.trimIndent()

        val line = parse(ttml).lines.first { !it.isInterlude && !it.isSongwriter }
        assertEquals("Hito", line.words[0].romanizedText)
        assertEquals("ni", line.words[1].romanizedText)
        // The real ("en") translation is parsed separately and must not leak into romanizedText.
        assertEquals("There are people", line.translatedText)
    }

    @Test
    fun `inline x-roman span is not appended as extra lyric words`() {
        // Real-world Apple lyric TTML (confirmed against an actual file) appends a whole-line
        // romanization as an extra untimed <span ttm:role="x-roman"> inside the SAME <p> as the
        // original lyrics — distinct from the <transliterations> metadata block, which times
        // romanization per-syllable. Left unhandled, this span's text gets tokenized and appended
        // to leadWords, showing the original words immediately followed by their own romanization
        // concatenated into the same line.
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" itunes:timing="Word">
              <body><div begin="0:04.363" end="0:05.141">
                <p begin="0:04.363" end="0:05.141" ttm:agent="v2" itunes:key="L1"><span begin="0:04.363" end="0:04.642">ねぇ、</span><span begin="0:04.642" end="0:04.768">知</span><span begin="0:04.642" end="0:04.885">って</span><span begin="0:04.885" end="0:05.141">る?</span><span ttm:role="x-roman">Ne~e, shitteru?</span></p>
              </div></body>
            </tt>
        """.trimIndent()

        val line = parse(ttml).lines.first { !it.isInterlude && !it.isSongwriter }
        assertEquals(listOf("ねぇ、", "知", "って", "る?"), line.words.map { it.text })
        assertEquals("Ne~e, shitteru?", line.romanizedFull)
    }

    @Test
    fun `explicit itunes timing overrides the heuristic`() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" itunes:timing="Line">
              <body><div>
                <p begin="0:01.000" end="0:03.000"><span begin="0:01.000" end="0:02.000">Word</span> <span begin="0:02.000" end="0:03.000">timed</span></p>
              </div></body>
            </tt>
        """.trimIndent()
        assertEquals(LyricsType.Line, parse(ttml).type)
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

    @Test
    fun `truncated ttml returns empty lyrics instead of throwing`() {
        val result = parse(
            """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="0:01.000" end="0:02.000">Hel"""
        )
        assertTrue(result.lines.isEmpty())
    }

    @Test
    fun `garbage input returns empty lyrics instead of throwing`() {
        assertTrue(parse("this is not xml at all").lines.isEmpty())
    }

    @Test
    fun `mismatched tags return empty lyrics instead of throwing`() {
        val result = parse(
            """<tt><body><div><p begin="1" end="2">X</span></p></div></body></tt>"""
        )
        assertTrue(result.lines.isEmpty())
    }

    @Test
    fun `empty stream returns empty lyrics instead of throwing`() {
        assertTrue(parse("").lines.isEmpty())
    }
}
