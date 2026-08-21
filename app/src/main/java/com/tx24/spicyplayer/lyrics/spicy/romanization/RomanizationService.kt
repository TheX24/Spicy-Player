package com.tx24.spicyplayer.lyrics.spicy.romanization

import com.tx24.spicyplayer.lyrics.spicy.models.Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves the best available [Romanizer] for a script. Japanese prefers the dictionary-backed
 * [JapaneseRomanizer] (Kuromoji) when present, falling back to kana-only conversion.
 */
object Romanizers {
    fun forScript(script: Script): Romanizer? = when (script) {
        Script.JAPANESE -> JapaneseRomanizerProvider.get()
        Script.CHINESE -> PinyinRomanizer
        Script.KOREAN -> KoreanRomanizer
        Script.CYRILLIC -> CyrillicRomanizer
        Script.GREEK -> GreekRomanizer
        Script.LATIN -> null
    }
}

/**
 * Populates [Line]/word `romanizedText` using on-device romanizers, mirroring
 * `spicy-lyrics/.../ProcessLyrics.ts`. TTML/API-supplied romanizations are never overwritten —
 * only gaps are filled. Runs off the main thread.
 */
object RomanizationService {

    /** True if any word already carries romanization or contains a script we can romanize. */
    fun isAvailable(lines: List<Line>): Boolean {
        for (line in lines) {
            for (word in line.words) {
                if (word.romanizedText != null) return true
                val script = ScriptDetector.detect(word.text).firstOrNull() ?: continue
                val r = Romanizers.forScript(script) ?: continue
                if (r.isAvailable()) return true
            }
        }
        return false
    }

    suspend fun romanize(lines: List<Line>): List<Line> = withContext(Dispatchers.Default) {
        lines.map { line ->
            var changed = false
            val words = line.words.map { word ->
                if (word.romanizedText != null) return@map word  // supplied romanization wins
                var romanized = word.text
                for (script in ScriptDetector.detect(word.text)) {
                    val romanizer = Romanizers.forScript(script) ?: continue
                    if (romanizer.isAvailable()) romanized = romanizer.romanize(romanized)
                }
                if (romanized != word.text && romanized.isNotBlank()) {
                    changed = true
                    word.copy(romanizedText = romanized)
                } else word
            }
            if (changed) line.copy(words = words) else line
        }
    }
}
