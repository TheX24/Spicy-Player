package com.tx24.spicyplayer.lyrics.spicy.parser

/**
 * Detects right-to-left text (Arabic, Hebrew, Persian, …), ported from
 * `spicy-lyrics/src/utils/Lyrics/isRtl.ts`. Used to right-align lines and to
 * suppress per-letter splitting (RTL scripts are never letter-emphasised).
 */
object RtlDetector {
    // Hebrew, Arabic, Arabic Supplement/Extended, Syriac, Thaana, and related presentation forms.
    private val RTL_REGEX = Regex(
        "[\\u0591-\\u07FF\\uFB1D-\\uFDFD\\uFE70-\\uFEFC]"
    )

    /** True if [text] contains any strong RTL character. */
    fun isRtl(text: String): Boolean = RTL_REGEX.containsMatchIn(text)
}
