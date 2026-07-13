package com.tx24.spicyplayer.lyrics.spicy.romanization

/**
 * Romanizes Cyrillic (Russian-focused) with a per-character BGN/PCGN-style table.
 */
object CyrillicRomanizer : Romanizer {
    override val script = Script.CYRILLIC

    private val MAP: Map<Char, String> = buildMap {
        val pairs = listOf(
            'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo",
            'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
            'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
            'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "shch",
            'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya"
        )
        for ((lower, roman) in pairs) {
            put(lower, roman)
            val upper = lower.uppercaseChar()
            put(upper, if (roman.isEmpty()) "" else roman.replaceFirstChar { it.uppercaseChar() })
        }
    }

    override fun romanize(text: String): String {
        val sb = StringBuilder(text.length * 2)
        for (c in text) sb.append(MAP[c] ?: c.toString())
        return sb.toString()
    }
}
