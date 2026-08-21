package com.tx24.spicyplayer.lyrics.spicy.canvas

internal enum class LyricScriptFont { DEFAULT, VAZIRMATN, NOTO_SANS_GEORGIAN }

internal object ScriptFontSelector {
    fun select(text: String): LyricScriptFont {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (codePoint in 0x10A0..0x10FF || codePoint in 0x1C90..0x1CBF || codePoint in 0x2D00..0x2D2F) {
                return LyricScriptFont.NOTO_SANS_GEORGIAN
            }
            if (codePoint in 0x0600..0x08FF || codePoint in 0xFB50..0xFDFF || codePoint in 0xFE70..0xFEFF) {
                return LyricScriptFont.VAZIRMATN
            }
            index += Character.charCount(codePoint)
        }
        return LyricScriptFont.DEFAULT
    }
}
