package com.tx24.spicyplayer.lyrics.spicy.romanization

/**
 * Chooses the Japanese romanizer implementation. Prefers the dictionary-backed
 * [JapaneseRomanizer] (Kuromoji) when its tokenizer initializes; otherwise falls back to
 * kana-only conversion (kanji left unchanged). The result is memoized.
 */
object JapaneseRomanizerProvider {
    @Volatile private var cached: Romanizer? = null

    fun get(): Romanizer {
        cached?.let { return it }
        val chosen = try {
            val kuromoji = JapaneseRomanizer
            if (kuromoji.isAvailable()) kuromoji else KanaOnlyJapaneseRomanizer
        } catch (t: Throwable) {
            KanaOnlyJapaneseRomanizer
        }
        cached = chosen
        return chosen
    }
}
