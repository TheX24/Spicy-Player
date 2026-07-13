package com.tx24.spicyplayer.lyrics.spicy.romanization

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType

/**
 * Chinese (Han → pinyin) romanizer backed by pinyin4j. Produces toneless lowercase pinyin with
 * syllables space-separated; non-Han characters pass through unchanged.
 */
object PinyinRomanizer : Romanizer {
    override val script = Script.CHINESE

    private val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
    }

    override fun isAvailable(): Boolean = true

    override fun romanize(text: String): String {
        val sb = StringBuilder(text.length * 2)
        for (c in text) {
            val pinyin = try {
                PinyinHelper.toHanyuPinyinStringArray(c, format)?.firstOrNull()
            } catch (t: Throwable) {
                null
            }
            if (pinyin != null) {
                if (sb.isNotEmpty() && sb.last() != ' ') sb.append(' ')
                sb.append(pinyin).append(' ')
            } else {
                sb.append(c)
            }
        }
        return sb.toString().trim().replace(Regex(" +"), " ")
    }
}
