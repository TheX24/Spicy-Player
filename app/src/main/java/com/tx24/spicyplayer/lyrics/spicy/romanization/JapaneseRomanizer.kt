package com.tx24.spicyplayer.lyrics.spicy.romanization

import com.atilika.kuromoji.ipadic.Tokenizer

/**
 * Dictionary-backed Japanese romanizer using Kuromoji (IPADIC). Tokenizes text, reads each
 * token's katakana reading (covering kanji), and converts it to Hepburn romaji via
 * [KanaRomanizer]. Falls back to kana-only conversion on any failure.
 *
 * The tokenizer loads a bundled dictionary lazily on first use (off the main thread).
 */
object JapaneseRomanizer : Romanizer {
    override val script = Script.JAPANESE

    private val tokenizer: Tokenizer? by lazy {
        try { Tokenizer() } catch (t: Throwable) { null }
    }

    override fun isAvailable(): Boolean = tokenizer != null

    override fun romanize(text: String): String {
        val tk = tokenizer ?: return KanaRomanizer.romanize(text)
        return try {
            val sb = StringBuilder(text.length * 2)
            for (token in tk.tokenize(text)) {
                val reading = token.reading
                val source = if (reading != null && reading != "*") reading else token.surface
                sb.append(KanaRomanizer.romanize(source))
            }
            sb.toString()
        } catch (t: Throwable) {
            KanaRomanizer.romanize(text)
        }
    }
}
