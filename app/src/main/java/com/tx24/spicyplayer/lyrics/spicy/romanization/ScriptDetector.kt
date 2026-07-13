package com.tx24.spicyplayer.lyrics.spicy.romanization

/**
 * Detects the dominant non-Latin script of a block of text, ported from the cascade in
 * `spicy-lyrics/src/utils/Lyrics/ProcessLyrics.ts`: kana implies Japanese, otherwise Han
 * implies Chinese; then Hangul, Cyrillic, Greek. Character-class scanning over the whole
 * lyric is sufficient offline (no language-guessing library needed).
 */
object ScriptDetector {

    private fun hasKana(c: Char): Boolean {
        val b = Character.UnicodeBlock.of(c)
        return b == Character.UnicodeBlock.HIRAGANA || b == Character.UnicodeBlock.KATAKANA
    }

    private fun hasHan(c: Char): Boolean {
        val b = Character.UnicodeBlock.of(c)
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
            b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
    }

    private fun isHangul(c: Char): Boolean {
        val b = Character.UnicodeBlock.of(c)
        return b == Character.UnicodeBlock.HANGUL_SYLLABLES ||
            b == Character.UnicodeBlock.HANGUL_JAMO ||
            b == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
    }

    private fun isCyrillic(c: Char): Boolean =
        Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC

    private fun isGreek(c: Char): Boolean =
        Character.UnicodeBlock.of(c) == Character.UnicodeBlock.GREEK

    /**
     * Returns the set of scripts present in [text], most-specific first. Japanese is chosen over
     * Chinese when any kana is present; if only Han is present the text is treated as Chinese.
     */
    fun detect(text: String): Set<Script> {
        var kana = false
        var han = false
        var hangul = false
        var cyr = false
        var greek = false
        for (c in text) {
            when {
                hasKana(c) -> kana = true
                hasHan(c) -> han = true
                isHangul(c) -> hangul = true
                isCyrillic(c) -> cyr = true
                isGreek(c) -> greek = true
            }
        }
        val result = linkedSetOf<Script>()
        if (kana) result.add(Script.JAPANESE)
        else if (han) result.add(Script.CHINESE)
        if (hangul) result.add(Script.KOREAN)
        if (cyr) result.add(Script.CYRILLIC)
        if (greek) result.add(Script.GREEK)
        return result
    }
}
