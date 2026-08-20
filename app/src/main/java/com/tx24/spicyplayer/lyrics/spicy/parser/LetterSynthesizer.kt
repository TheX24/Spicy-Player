package com.tx24.spicyplayer.lyrics.spicy.parser

import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import com.tx24.spicyplayer.lyrics.spicy.models.Letter
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.Word

/**
 * Splits sufficiently long syllables into per-letter timings for the "held word"
 * letter-by-letter emphasis, mirroring `spicy-lyrics/.../Emphasize.ts`.
 *
 * This runs at render time (not parse time) because letter capability depends on the
 * active [RenderConfig] (thresholds differ per quality mode) and on whether romanized
 * text is currently displayed (letters are split over the *displayed* string).
 */
object LetterSynthesizer {

    /**
     * @param romanized when true, letters are split over each word's romanized text (if present).
     */
    fun apply(lines: List<Line>, config: RenderConfig, romanized: Boolean): List<Line> {
        return lines.map { line ->
            if (line.isInterlude || line.isSongwriter) {
                line
            } else {
                line.copy(words = line.words.map { synthesize(it, config, romanized) })
            }
        }
    }

    private fun synthesize(word: Word, config: RenderConfig, romanized: Boolean): Word {
        val display = if (romanized) (word.romanizedText ?: word.text) else word.text
        val graphemes = GraphemeSegmenter.segment(display)
        val len = graphemes.size

        // Reference (IsLetterCapable.ts) has no lower length bound — even a single-character
        // held word (e.g. "I", "oh") gets the letter treatment if held long enough.
        val capable = config.lettersEnabled &&
            word.duration >= config.letterDurationThresholdMs &&
            len in 1..config.letterMaxLength &&
            !RtlDetector.isRtl(display)

        if (!capable) {
            return if (word.isLetterGroup) word.copy(isLetterGroup = false, letters = emptyList()) else word
        }

        // Reference (Emphasize.ts) Subtractions: the emphasized WORD's own window is shrunk too,
        // not just the letters — normal mode trims 250ms off the tail (the "breather"); simple
        // mode nudges the window by {Start:-21, End:-40} (i.e. +21ms/+40ms).
        val subStartMs = if (config.isSimple) -21L else 0L
        val subEndMs = if (config.isSimple) -40L else 250L
        val windowStart = word.startMs - subStartMs
        val windowEnd = (word.endMs - subEndMs).coerceAtLeast(windowStart + 1L)
        val span = (windowEnd - windowStart).toFloat() / len

        // Every character — including punctuation — is split into its own timed letter and
        // animated identically; the reference (Emphasize.ts) has no exclusion for punctuation.
        val letters = graphemes.mapIndexed { i, grapheme ->
            Letter(
                char = grapheme,
                startMs = windowStart + (i * span).toLong(),
                endMs = windowStart + ((i + 1) * span).toLong(),
            )
        }
        return word.copy(startMs = windowStart, endMs = windowEnd, isLetterGroup = true, letters = letters)
    }
}
