package com.tx24.spicyplayer.lyrics.spicy.romanization

/**
 * Romanizes Hangul using the Revised Romanization of Korean by decomposing each syllable
 * block into initial/medial/final jamo. Cross-syllable liaison is not applied (the common
 * approach for per-syllable lyric display), so results are a close, readable approximation.
 */
object KoreanRomanizer : Romanizer {
    override val script = Script.KOREAN

    private const val BASE = 0xAC00
    private const val LAST = 0xD7A3

    private val INITIALS = arrayOf(
        "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"
    )
    private val MEDIALS = arrayOf(
        "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo",
        "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
    )
    private val FINALS = arrayOf(
        "", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "p", "l", "l", "p", "l",
        "m", "p", "p", "t", "t", "ng", "t", "t", "k", "t", "p", "t"
    )

    override fun romanize(text: String): String {
        val sb = StringBuilder(text.length * 2)
        for (c in text) {
            val code = c.code
            if (code in BASE..LAST) {
                val offset = code - BASE
                val initial = offset / (21 * 28)
                val medial = (offset % (21 * 28)) / 28
                val final = offset % 28
                sb.append(INITIALS[initial]).append(MEDIALS[medial]).append(FINALS[final])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }
}
