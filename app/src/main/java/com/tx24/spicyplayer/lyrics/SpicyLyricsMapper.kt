package com.tx24.spicyplayer.lyrics

import com.tx24.spicyplayer.model.lyrics.SynchronizedLyrics
import com.tx24.spicyplayer.uiNowPlaying.spicy.models.Line
import com.tx24.spicyplayer.uiNowPlaying.spicy.models.Word

/**
 * Maps the app's LRC-based [SynchronizedLyrics] to the spicy-player [Line] model.
 * Since LRC only provides line-level timing, each line is mapped as a single Word.
 */
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
            val prevLine = spicyLines.last()
            val prevEnd = prevLine.endMs
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
        
        // Each LRC segment becomes one Line containing one Word that covers the whole duration.
        val word = Word(
            text = segment.text,
            startMs = startTime,
            endMs = endTime
        )
        
        spicyLines.add(
            Line(
                words = listOf(word),
                startMs = startTime
            )
        )
    }
    
    return spicyLines
}
