package com.tx24.spicyplayer.lyrics.spicy.romanization

/**
 * Chinese (Han → pinyin) romanizer. A pure-Kotlin build has no pinyin dictionary, so this is a
 * passthrough reporting itself unavailable; a TinyPinyin-backed implementation replaces it when
 * that library is on the classpath (see [PinyinRomanizer] once the dependency is added).
 */
object ChineseRomanizer : Romanizer {
    override val script = Script.CHINESE
    override fun isAvailable(): Boolean = false
    override fun romanize(text: String): String = text
}
