package com.tx24.spicyplayer.lyrics

import com.tx24.spicyplayer.model.lyrics.PlainLyrics
import com.tx24.spicyplayer.model.lyrics.SynchronizedLyrics
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType
import com.tx24.spicyplayer.lyrics.spicy.models.ParsedLyrics
import com.tx24.spicyplayer.lyrics.spicy.models.Word

/**
 * Maps unsynced [PlainLyrics] to [LyricsType.Static] lines so plain text renders through the
 * same Spicy canvas (full-white, non-interactive) instead of a separate list.
 */
fun PlainLyrics.toSpicyStaticParsed(): ParsedLyrics =
    ParsedLyrics(
        lines = lines.mapIndexed { i, text ->
            val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val words = if (tokens.isEmpty()) listOf(Word(text.ifBlank { " " }, i.toLong(), i.toLong()))
                else tokens.map { Word(it, i.toLong(), i.toLong()) }
            Line(words = words, startMs = i.toLong())
        },
        type = LyricsType.Static,
    )

/**
 * Maps the app's LRC-based [SynchronizedLyrics] to the spicy-player model as line-synced
 * ([LyricsType.Line]) lyrics. Each LRC line is split into word tokens that share the line's
 * time window, so the renderer can sweep a single gradient across the whole line.
 */
fun SynchronizedLyrics.toSpicyParsedLyrics(): ParsedLyrics =
    ParsedLyrics(lines = toSpicyLines(), type = LyricsType.Line)

fun SynchronizedLyrics.toSpicyLines(): List<Line> {
    val spicyLines = mutableListOf<Line>()

    for (i in segments.indices) {
        val segment = segments[i]
        val startTime = segment.durationMillis.toLong()

        // Determine end time from the next segment's start time.
        // If it's the last segment, we assume a reasonable 5s duration.
        val endTime = if (i < segments.size - 1) {
            segments[i + 1].durationMillis.toLong()
        } else {
            startTime + 5000L
        }

        // Insert interlude if there is a gap of 3 seconds or more between segments.
        if (i > 0) {
            val prevEnd = spicyLines.last().endMs
            if (startTime - prevEnd >= 3000L) {
                spicyLines.add(
                    Line(
                        words = emptyList(),
                        startMs = prevEnd + 250L,
                        isInterlude = true,
                        interludeEndMs = startTime - 250L
                    )
                )
            }
        }

        // Split the line into word tokens; all share the line's time window (line-level sync).
        val tokens = segment.text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val words = if (tokens.isEmpty()) {
            listOf(Word(text = segment.text, startMs = startTime, endMs = endTime))
        } else {
            tokens.map { Word(text = it, startMs = startTime, endMs = endTime) }
        }

        spicyLines.add(Line(words = words, startMs = startTime))
    }

    return spicyLines
}
