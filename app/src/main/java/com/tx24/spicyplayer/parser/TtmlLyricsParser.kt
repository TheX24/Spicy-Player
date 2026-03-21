package com.tx24.spicyplayer.parser

import android.util.Xml
import com.tx24.spicyplayer.models.Letter
import com.tx24.spicyplayer.models.Line
import com.tx24.spicyplayer.models.ParsedLyrics
import com.tx24.spicyplayer.models.Word
import org.xmlpull.v1.XmlPullParser
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
    )

    /**
     * Parses a TTML input stream into a [ParsedLyrics] object.
     *
     * @param inputStream The stream containing the TTML content.
     * @return A [ParsedLyrics] object containing the parsed lines and metadata.
     */
    fun parse(inputStream: InputStream): ParsedLyrics {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(InputStreamReader(inputStream))

        val lines = mutableListOf<Line>()
        val songwriters = mutableListOf<String>()
        var eventType = parser.eventType
        var defaultAgent: String? = null

        var inSongwriters = false

        // Main parsing loop.
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "songwriter" -> inSongwriters = true
                        "p" -> {
                            // Paragraph tags represent a block of lyrics.
                            val agent = parser.getAttributeValue(null, "agent")
                                ?: parser.getAttributeValue("http://www.w3.org/ns/ttml#metadata", "agent")
                            if (defaultAgent == null && agent != null) {
                                defaultAgent = agent
                            }
                            // Parse the paragraph into one or more Line objects.
                            val parsedLines = parseParagraph(parser, agent, defaultAgent)
                            lines.addAll(parsedLines)
                        }
                        "agent" -> {
                            val id = parser.getAttributeValue("http://www.w3.org/XML/1998/namespace", "id")
                                ?: parser.getAttributeValue(null, "id")
                            if (id == "v1") {
                                defaultAgent = id
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    // Extract songwriter names from the <songwriter> tag.
                    if (inSongwriters) {
                        parser.text?.trim()?.let {
                            if (it.isNotEmpty()) songwriters.add(it)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "songwriter") {
                        inSongwriters = false
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
        val mainLines = lines.filter { !it.isBackground && !it.isSongwriter }
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

        return ParsedLyrics(lines, songwriters)
    }

    /**
     * Parses a <p> tag and its contents into a list of [Line] objects.
     * This handles nested <span> tags for background vocals and word-level timing.
     */
    private fun parseParagraph(
        parser: XmlPullParser,
        agent: String?,
        defaultAgent: String?,
    ): List<Line> {
        val pBegin = parser.getAttributeValue(null, "begin")?.let { parseTimeMs(it) } ?: 0L
        val pEnd = parser.getAttributeValue(null, "end")?.let { parseTimeMs(it) } ?: 0L

        val leadWords = mutableListOf<Word>()
        val backgroundGroups = mutableListOf<MutableList<Word>>()

        // Stack to track nested span timings and metadata.
        val stack = ArrayDeque<SpanContext>()
        stack.addLast(SpanContext(pBegin, pEnd, false))

        var previousEndedMidWord = false
        var currentBgGroup: MutableList<Word>? = null
        var inBgSpan = false
        var bgPreviousEndedMidWord = false

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "p")) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    // Inherit timing from parent if not specified.
                    val b = parser.getAttributeValue(null, "begin")?.let { parseTimeMs(it) }
                        ?: stack.last().begin
                    val e = parser.getAttributeValue(null, "end")?.let { parseTimeMs(it) }
                        ?: stack.last().end

                    // Check if this span represents background vocals.
                    val role = parser.getAttributeValue(null, "role")
                        ?: parser.getAttributeValue("http://www.w3.org/ns/ttml#metadata", "role")
                    val isBg = role == "x-bg" || stack.last().isBg

                    if (isBg && currentBgGroup == null) {
                        currentBgGroup = mutableListOf()
                        bgPreviousEndedMidWord = false
                    }
                    inBgSpan = isBg

                    stack.addLast(SpanContext(b, e, isBg))
                }
                XmlPullParser.TEXT -> {
                    val rawText = parser.text
                    val ctx = stack.last()
                    val isBgToken = ctx.isBg || inBgSpan

                    if (rawText.isNullOrBlank()) {
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
                                        if (isBgToken) bgPreviousEndedMidWord && !startsWithSpace else previousEndedMidWord && !startsWithSpace
                                    } else {
                                        j > 0
                                    }
                                    wordsToAdd.add(Pair(subTokens[j], isAttached))
                                }
                            }
                            
                            val start = ctx.begin ?: pBegin
                            val end = ctx.end ?: (start + 1000L)
                            val duration = (end - start).coerceAtLeast(0)
                            // Distribute total span duration equally across all tokens within it.
                            val chunkDuration = if (wordsToAdd.isNotEmpty()) duration / wordsToAdd.size else duration

                            wordsToAdd.forEachIndexed { index, pair ->
                                val (token, isAttached) = pair
                                val wordStart = start + (index * chunkDuration)
                                val wordEnd = start + ((index + 1) * chunkDuration)
                                val wordDuration = wordEnd - wordStart

                                // Decide if this word should have granular letter-level animation.
                                val isLetterGroup = wordDuration >= 1000L && token.length <= 12 && token.length > 1
                                val letters = if (isLetterGroup) {
                                    val letterDuration = wordDuration.toFloat() / token.length
                                    token.mapIndexed { li, ch ->
                                        Letter(
                                            char = ch.toString(),
                                            startMs = wordStart + (li * letterDuration).toLong(),
                                            endMs = wordStart + ((li + 1) * letterDuration).toLong(),
                                        )
                                    }
                                } else emptyList()

                                val word = Word(token, wordStart, wordEnd, isPartOfWord = isAttached, isLetterGroup = isLetterGroup, letters = letters)
                                if (isBgToken) {
                                    currentBgGroup?.add(word)
                                } else {
                                    leadWords.add(word)
                                }
                            }
                            if (isBgToken) bgPreviousEndedMidWord = !endsWithSpace else previousEndedMidWord = !endsWithSpace
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

        val result = mutableListOf<Line>()
        // Add the primary lead line.
        result.add(Line(leadWords, pBegin, agent = agent, isBackground = false, oppositeAligned = isOppositeAligned))
        // Add any associated background lines.
        for (bgGroup in backgroundGroups) {
            val bgStart = bgGroup.firstOrNull()?.startMs ?: pBegin
            result.add(Line(bgGroup, bgStart, agent = agent, isBackground = true, oppositeAligned = isOppositeAligned))
        }

        return result
    }

    /**
     * Parses a TTML time format string into milliseconds.
     * Supports mm:ss.ms and hh:mm:ss.ms formats.
     */
    private fun parseTimeMs(time: String): Long {
        val parts = time.split(":")
        if (parts.size == 2) {
            val min = parts[0].toLong()
            val secParts = parts[1].split(".")
            val sec = secParts[0].toLong()
            val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
            return (min * 60 + sec) * 1000 + ms
        } else if (parts.size == 3) {
            val hrs = parts[0].toLong()
            val min = parts[1].toLong()
            val secParts = parts[2].split(".")
            val sec = secParts[0].toLong()
            val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
            return (hrs * 3600 + min * 60 + sec) * 1000 + ms
        }
        return 0L
    }
}
