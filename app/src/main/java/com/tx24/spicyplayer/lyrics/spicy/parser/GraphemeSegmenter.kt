package com.tx24.spicyplayer.lyrics.spicy.parser

import android.os.Build

object GraphemeSegmenter {
    fun segment(text: String, sdkInt: Int = Build.VERSION.SDK_INT): List<String> {
        if (text.isEmpty()) return emptyList()
        if (sdkInt >= 24) {
            val iterator = android.icu.text.BreakIterator.getCharacterInstance()
            iterator.setText(text)
            val result = ArrayList<String>()
            var start = iterator.first()
            var end = iterator.next()
            while (end != android.icu.text.BreakIterator.DONE) {
                result += text.substring(start, end)
                start = end
                end = iterator.next()
            }
            return result
        }
        return fallback(text)
    }

    internal fun fallback(text: String): List<String> {
        val result = ArrayList<String>()
        var start = 0
        var index = 0
        var regionalCount = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val width = Character.charCount(codePoint)
            val joinsPrevious = index > start && (
                isCombining(codePoint) || isVariationSelector(codePoint) || isEmojiModifier(codePoint) ||
                    codePoint == 0x20E3 || text.codePointBefore(index) == 0x200D || codePoint == 0x200D ||
                    (isRegionalIndicator(codePoint) && regionalCount % 2 == 1)
                )
            if (!joinsPrevious && index > start) {
                result += text.substring(start, index)
                start = index
                regionalCount = 0
            }
            if (isRegionalIndicator(codePoint)) regionalCount++ else if (codePoint != 0x200D) regionalCount = 0
            index += width
        }
        result += text.substring(start)
        return result
    }

    private fun isCombining(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
        Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt() -> true
        else -> false
    }

    private fun isVariationSelector(codePoint: Int) = codePoint in 0xFE00..0xFE0F || codePoint in 0xE0100..0xE01EF
    private fun isEmojiModifier(codePoint: Int) = codePoint in 0x1F3FB..0x1F3FF
    private fun isRegionalIndicator(codePoint: Int) = codePoint in 0x1F1E6..0x1F1FF
}
