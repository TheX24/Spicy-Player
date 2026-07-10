package com.tx24.spicyplayer.lyrics.spicy.parser

import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType
import com.tx24.spicyplayer.lyrics.spicy.models.ParsedLyrics
import com.tx24.spicyplayer.lyrics.spicy.models.Word
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.InputStreamReader

/**
 * A parser for TTML (Timed Text Markup Language) lyric files.
 * This parser extracts lyrics, timing information, songwriter metadata, and interludes.
 */
object TtmlLyricsParser {
    /**
     * Context used during parsing to keep track of nested span timing and metadata.
     */
    data class SpanContext(
        val begin: Long?,
        val end: Long?,
        val isBg: Boolean = false,
        val isRoman: Boolean = false,
    )

    /** Result of parsing one `<p>`: its lines and whether any inner span carried explicit timing. */
    private data class ParagraphResult(
        val lines: List<Line>,
        val sawTimedSpan: Boolean,
    )

    private const val ITUNES_NS = "http://music.apple.com/lyric-ttml-internal"

    private enum class MetaKind { NONE, ROMANIZATION_SPANS, PLAIN_TRANSLATION }

    /** True if an `xml:lang`/`lang` value denotes a romanized (Latin-script) rendering of another script. */
    private fun isLatinTargetLang(lang: String?): Boolean =
        lang != null && lang.contains("Latn", ignoreCase = true)

    /**
     * Parses a TTML input stream into a [ParsedLyrics] object.
     *
     * Malformed or truncated TTML yields an empty [ParsedLyrics] instead of
     * throwing: an uncaught parser exception would cancel the song-change
     * collector and disable lyrics for the rest of the session.
     *
     * @param inputStream The stream containing the TTML content.
     * @return A [ParsedLyrics] object containing the parsed lines and metadata.
     */
    fun parse(inputStream: InputStream): ParsedLyrics =
        try {
            parseInternal(inputStream)
        } catch (e: Exception) {
            ParsedLyrics(emptyList())
        }

    private fun parseInternal(inputStream: InputStream): ParsedLyrics {
        // XmlPullParserFactory resolves to the same KXmlParser as android.util.Xml
        // on device, but is also instantiable in plain JVM unit tests.
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        val reader = inputStream.reader(Charsets.UTF_8)
        parser.setInput(reader)

        val lines = mutableListOf<Line>()
        val songwriters = mutableListOf<String>()
        var eventType = parser.eventType
        var defaultAgent: String? = null

        var inSongwriters = false
        var sawTimedSpan = false
        // Explicit document timing granularity, if declared on <tt itunes:timing="…">.
        var declaredTiming: String? = null

        // Transliteration/translation metadata keyed by the owning <p>'s key.
        val transliterations = mutableMapOf<String, MutableList<String>>()
        val translations = mutableMapOf<String, StringBuilder>()
        var currentMetaKey: String? = null
        // Apple's actual lyric TTML format has no distinct <transliteration> element: BOTH
        // translations and romanizations are delivered as <translation> blocks, disambiguated
        // only by xml:lang (e.g. "ja-Latn" = romanized Japanese, "en" = an actual translation).
        // ROMANIZATION_SPANS reuses the per-span, per-syllable parsing (matched by ordinal
        // position to leadWords, same as a hypothetical dedicated <transliteration> tag would);
        // PLAIN_TRANSLATION just concatenates text (reserved, not yet displayed).
        var metaKind: MetaKind = MetaKind.NONE

        val timingStack = ArrayDeque<Pair<Long, Long?>>()
        timingStack.addLast(0L to null)

        // Main parsing loop.
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "tt" -> {
                            declaredTiming = parser.getAttributeValue(ITUNES_NS, "timing")
                                ?: parser.getAttributeValue(null, "timing")
                        }
                        "body", "div" -> {
                            val b = parser.getAttributeValue(null, "begin")?.let { parseTimeMs(it) }
                                ?: timingStack.last().first
                            val e = parser.getAttributeValue(null, "end")?.let { parseTimeMs(it) }
                                ?: timingStack.last().second
                            timingStack.addLast(b to e)
                        }
                        "songwriter" -> inSongwriters = true
                        // A dedicated <transliteration> element, if one exists in this file.
                        "transliteration" -> metaKind = MetaKind.ROMANIZATION_SPANS
                        // Apple's real format: <translation xml:lang="…"> for both actual
                        // translations AND romanizations, disambiguated by xml:lang.
                        "translation" -> {
                            val lang = parser.getAttributeValue("http://www.w3.org/XML/1998/namespace", "lang")
                                ?: parser.getAttributeValue(null, "lang")
                            metaKind = if (isLatinTargetLang(lang)) MetaKind.ROMANIZATION_SPANS else MetaKind.PLAIN_TRANSLATION
                        }
                        "text" -> {
                            // Inside a transliteration/translation block, <text for="KEY"> groups syllables.
                            if (metaKind != MetaKind.NONE) {
                                currentMetaKey = parser.getAttributeValue(ITUNES_NS, "for")
                                    ?: parser.getAttributeValue(null, "for")
                            }
                        }
                        "p" -> {
                            val parentStart = timingStack.last().first
                            val parentEnd = timingStack.last().second
                            // Paragraph tags represent a block of lyrics.
                            val agent = parser.getAttributeValue(null, "agent")
                                ?: parser.getAttributeValue("http://www.w3.org/ns/ttml#metadata", "agent")
                            if (defaultAgent == null && agent != null) {
                                defaultAgent = agent
                            }
                            val key = parser.getAttributeValue(ITUNES_NS, "key")
                                ?: parser.getAttributeValue(null, "key")
                                ?: parser.getAttributeValue("http://www.w3.org/XML/1998/namespace", "id")
                            // Parse the paragraph into one or more Line objects.
                            val result = parseParagraph(parser, agent, defaultAgent, parentStart, parentEnd, key,
                                transliterations, translations)
                            if (result.sawTimedSpan) sawTimedSpan = true
                            lines.addAll(result.lines)
                        }
                        "agent" -> {
                            val id = parser.getAttributeValue("http://www.w3.org/XML/1998/namespace", "id")
                                ?: parser.getAttributeValue(null, "id")
                            if (id == "v1") {
                                defaultAgent = id
                            }
                        }
                        "span" -> {
                            // A syllable span inside a <text for="KEY"> romanization block.
                            if (metaKind == MetaKind.ROMANIZATION_SPANS && currentMetaKey != null) {
                                val text = readElementText(parser)
                                if (text.isNotEmpty()) {
                                    transliterations.getOrPut(currentMetaKey!!) { mutableListOf() }.add(text)
                                }
                                // readElementText consumed through this span's END_TAG.
                                eventType = parser.eventType
                                continue
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    when {
                        inSongwriters -> parser.text?.trim()?.replace(Regex("\\s+"), " ")?.let {
                            if (it.isNotEmpty()) songwriters.add(it)
                        }
                        metaKind == MetaKind.PLAIN_TRANSLATION && currentMetaKey != null -> parser.text?.let {
                            if (it.isNotBlank()) translations.getOrPut(currentMetaKey!!) { StringBuilder() }.append(it)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "body", "div" -> if (timingStack.size > 1) timingStack.removeLast()
                        "songwriter" -> inSongwriters = false
                        "transliteration", "translation" -> metaKind = MetaKind.NONE
                        "text" -> currentMetaKey = null
                    }
                }
            }
            eventType = parser.next()
        }

        // Create a special line at the end for songwriter credits.
        if (songwriters.isNotEmpty()) {
            val lastLineEnd = lines.maxOfOrNull { it.endMs } ?: 0L
            val text = "Written by ${songwriters.joinToString(", ")}"
            val tokens = text.split(" ")
            
            val words = mutableListOf<Word>()
            for (i in tokens.indices) {
                words.add(Word(tokens[i], lastLineEnd, lastLineEnd + 1000L, isPartOfWord = false))
            }
            
            lines.add(
                Line(
                    words = words,
                    startMs = lastLineEnd,
                    isSongwriter = true
                )
            )
        }

        // Inject instrumental interlude placeholders for significant gaps (>= 3s) between main lines.
        val mainLines = lines.filter { !it.isSongwriter }
            .sortedBy { it.startMs }
        val interludes = mutableListOf<Line>()
        for (i in 0 until mainLines.size - 1) {
            val gapStart = mainLines[i].endMs
            val gapEnd = mainLines[i + 1].startMs
            if (gapEnd - gapStart >= 3000L) {
                interludes.add(Line(
                    words = emptyList(),
                    startMs = gapStart,
                    interludeEndMs = gapEnd,
                    isInterlude = true,
                ))
            }
        }
        // Handle initial gap before the first line.
        if (mainLines.isNotEmpty() && mainLines.first().startMs >= 3000L) {
            interludes.add(Line(
                words = emptyList(),
                startMs = 0L,
                interludeEndMs = mainLines.first().startMs,
                isInterlude = true,
            ))
        }
        lines.addAll(interludes)
        lines.sortBy { it.startMs }

        // Determine synchronization granularity. An explicit itunes:timing wins; otherwise a
        // document whose <p>s have no per-span timing is treated as line-synced.
        val type = when (declaredTiming?.lowercase()) {
            "word" -> LyricsType.Syllable
            "line" -> LyricsType.Line
            "none" -> LyricsType.Static
            else -> if (!sawTimedSpan && lines.any { !it.isInterlude && !it.isSongwriter }) {
                LyricsType.Line
            } else {
                LyricsType.Syllable
            }
        }

        return ParsedLyrics(lines, songwriters, type)
    }

    /** Reads and returns the concatenated text content of the current element, consuming its END_TAG. */
    private fun readElementText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        var evt = parser.next()
        while (depth > 0) {
            when (evt) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.TEXT -> if (depth >= 1) sb.append(parser.text)
                XmlPullParser.END_DOCUMENT -> depth = 0
            }
            if (depth > 0) evt = parser.next()
        }
        return sb.toString().trim()
    }

    /**
     * Parses a <p> tag and its contents into a list of [Line] objects.
     * This handles nested <span> tags for background vocals and word-level timing.
     */
    private fun parseParagraph(
        parser: XmlPullParser,
        agent: String?,
        defaultAgent: String?,
        parentStart: Long,
        parentEnd: Long?,
        key: String?,
        transliterations: Map<String, List<String>>,
        translations: Map<String, StringBuilder>,
    ): ParagraphResult {
        val pBegin = parser.getAttributeValue(null, "begin")?.let { parseTimeMs(it) } ?: parentStart
        val pEnd = parser.getAttributeValue(null, "end")?.let { parseTimeMs(it) } ?: parentEnd ?: (pBegin + 5000L)

        val leadWords = mutableListOf<Word>()
        val backgroundGroups = mutableListOf<MutableList<Word>>()
        var sawTimedSpan = false
        // Apple's real lyric TTML often carries an extra inline `<span ttm:role="x-roman">` at the
        // end of each <p>, holding the WHOLE line's romanization as one untimed text blob. Left
        // unhandled, its text gets tokenized and appended to leadWords like any other span — the
        // line then displays the original lyric words immediately followed by their romanization,
        // concatenated into one line. Captured here instead of appended, as a whole-line fallback.
        val inlineRomanText = StringBuilder()

        // Stack to track nested span timings and metadata.
        val stack = ArrayDeque<SpanContext>()
        stack.addLast(SpanContext(pBegin, pEnd, false))

        var previousEndedMidWord = false
        var currentBgGroup: MutableList<Word>? = null
        var inBgSpan = false
        var bgPreviousEndedMidWord = false

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "p")) {
            // A document truncated inside <p> can yield END_DOCUMENT forever
            // instead of throwing; without this check the loop never exits.
            if (eventType == XmlPullParser.END_DOCUMENT) {
                throw XmlPullParserException("Unexpected end of document inside <p>")
            }
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    // Inherit timing from parent if not specified.
                    val explicitBegin = parser.getAttributeValue(null, "begin")
                    if (explicitBegin != null) sawTimedSpan = true
                    val b = explicitBegin?.let { parseTimeMs(it) } ?: stack.last().begin
                    val e = parser.getAttributeValue(null, "end")?.let { parseTimeMs(it) }
                        ?: stack.last().end

                    // Check if this span represents background vocals.
                    val role = (0 until parser.attributeCount).firstNotNullOfOrNull { i ->
                        val attrName = parser.getAttributeName(i)
                        if (attrName == "role" || attrName.endsWith(":role")) {
                            parser.getAttributeValue(i)
                        } else null
                    }
                    val isBg = role == "x-bg" || stack.last().isBg
                    val isRoman = role == "x-roman" || stack.last().isRoman

                    if (isBg && currentBgGroup == null) {
                        currentBgGroup = mutableListOf()
                        bgPreviousEndedMidWord = false
                    }
                    inBgSpan = isBg

                    stack.addLast(SpanContext(b, e, isBg, isRoman))
                }
                XmlPullParser.TEXT -> {
                    val rawText = parser.text
                    if (rawText != null) {
                        val ctx = stack.last()
                        if (ctx.isRoman) {
                            inlineRomanText.append(rawText)
                        } else {
                        val isBgToken = ctx.isBg || inBgSpan

                        if (rawText.isBlank()) {
                            // Just whitespace or empty: clear the mid-word flags
                            if (isBgToken) bgPreviousEndedMidWord = false else previousEndedMidWord = false
                        } else {
                            val startsWithSpace = rawText.first().isWhitespace()
                            val endsWithSpace = rawText.last().isWhitespace()
                            var trimmed = rawText.trim()

                            // Background tokens typically have parentheses, which we strip for cleaner UI.
                            if (isBgToken) {
                                trimmed = trimmed.removePrefix("(").removeSuffix(")")
                            }

                            if (trimmed.isNotEmpty()) {
                                // Split into words and sub-tokens (syllables).
                                val spaceTokens = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                                val wordsToAdd = mutableListOf<Pair<String, Boolean>>()
                                
                                for (i in spaceTokens.indices) {
                                    val st = spaceTokens[i]
                                    val subTokens = st.split(Regex("(?<=-)")).filter { it.isNotEmpty() }
                                    for (j in subTokens.indices) {
                                        val isAttached = if (i == 0 && j == 0) {
                                            // Attach only if last text didn't end with space AND this text didn't start with space
                                            val prevEndMid = if (isBgToken) bgPreviousEndedMidWord else previousEndedMidWord
                                            prevEndMid && !startsWithSpace
                                        } else {
                                            j > 0 // Syllable join
                                        }
                                        wordsToAdd.add(Pair(subTokens[j], isAttached))
                                    }
                                }
                                
                                val start = ctx.begin ?: pBegin
                                val end = ctx.end ?: (start + 1000L)
                                val duration = (end - start).coerceAtLeast(0)
                                val chunkDuration = if (wordsToAdd.isNotEmpty()) duration / wordsToAdd.size else duration

                                wordsToAdd.forEachIndexed { index, pair ->
                                    val (token, isAttached) = pair
                                    val wordStart = start + (index * chunkDuration)
                                    val wordEnd = start + ((index + 1) * chunkDuration)

                                    // Letter emphasis is synthesized later by LetterSynthesizer using
                                    // the active RenderConfig; the parser only produces plain word tokens.
                                    val word = Word(token, wordStart, wordEnd, isPartOfWord = isAttached)
                                    if (isBgToken) currentBgGroup?.add(word) else leadWords.add(word)
                                }
                            }
                            // If text ended with a space, then the next span shouldn't be attached
                            if (isBgToken) bgPreviousEndedMidWord = !endsWithSpace else previousEndedMidWord = !endsWithSpace
                        }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    // Close the current span context.
                    if (stack.isNotEmpty()) {
                        val popped = stack.removeLast()
                        if (popped.isBg && (stack.isEmpty() || !stack.last().isBg)) {
                            // If we finished a background span block, save the group.
                            currentBgGroup?.let { if (it.isNotEmpty()) backgroundGroups.add(it) }
                            currentBgGroup = null
                            inBgSpan = stack.isNotEmpty() && stack.last().isBg
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        // Compare agent to default agent to determine alignment.
        val isOppositeAligned = agent != null && defaultAgent != null && agent != defaultAgent

        // Apply TTML-supplied romanization to the lead words, matched by ordinal position.
        // Only applied when the counts line up exactly: a partial match would leave some words
        // romanized and others not, producing a garbled line mixing scripts. When the counts
        // disagree, leave every word's romanizedText null so RomanizationService's on-device
        // fallback romanizes the whole line uniformly instead.
        val romanizedTokens = key?.let { transliterations[it] }
        if (romanizedTokens != null && romanizedTokens.size == leadWords.size) {
            for (i in leadWords.indices) {
                leadWords[i] = leadWords[i].copy(romanizedText = romanizedTokens[i])
            }
        }
        val translated = key?.let { translations[it]?.toString()?.trim()?.takeIf { t -> t.isNotEmpty() } }
        val inlineRoman = inlineRomanText.toString().trim().takeIf { it.isNotEmpty() }

        val result = mutableListOf<Line>()
        // Add the primary lead line.
        result.add(Line(leadWords, pBegin, agent = agent, isBackground = false,
            oppositeAligned = isOppositeAligned, translatedText = translated, romanizedFull = inlineRoman))
        // Add any associated background lines.
        for (bgGroup in backgroundGroups) {
            val bgStart = bgGroup.firstOrNull()?.startMs ?: pBegin
            result.add(Line(bgGroup, bgStart, agent = agent, isBackground = true, oppositeAligned = isOppositeAligned))
        }

        return ParagraphResult(result, sawTimedSpan)
    }

    /**
     * Parses a TTML time format string into milliseconds.
     * Supports mm:ss.ms and hh:mm:ss.ms formats.
     */
    private fun parseTimeMs(time: String?): Long {
        if (time == null) return 0L
        val trimmed = time.trim().removeSuffix("s")
        val parts = trimmed.split(":")
        try {
            return when (parts.size) {
                1 -> {
                    // ss.ms or ss
                    val secParts = parts[0].replace(',', '.').split(".")
                    val sec = secParts[0].toLong()
                    val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
                    sec * 1000 + ms
                }
                2 -> {
                    // mm:ss.ms
                    val min = parts[0].toLong()
                    val secParts = parts[1].replace(',', '.').split(".")
                    val sec = secParts[0].toLong()
                    val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
                    (min * 60 + sec) * 1000 + ms
                }
                3 -> {
                    // hh:mm:ss.ms
                    val hrs = parts[0].toLong()
                    val min = parts[1].toLong()
                    val secParts = parts[2].replace(',', '.').split(".")
                    val sec = secParts[0].toLong()
                    val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
                    (hrs * 3600 + min * 60 + sec) * 1000 + ms
                }
                else -> 0L
            }
        } catch (e: Exception) {
            return 0L
        }
    }
}
