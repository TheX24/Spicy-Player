package com.tx24.spicyplayer.lyrics.spicy.romanization

/**
 * Romanizes Greek with a per-character ELOT-743-style table (no digraph/accent context rules).
 */
object GreekRomanizer : Romanizer {
    override val script = Script.GREEK

    private val MAP: Map<Char, String> = buildMap {
        val pairs = listOf(
            'α' to "a", 'β' to "v", 'γ' to "g", 'δ' to "d", 'ε' to "e", 'ζ' to "z", 'η' to "i",
            'θ' to "th", 'ι' to "i", 'κ' to "k", 'λ' to "l", 'μ' to "m", 'ν' to "n", 'ξ' to "x",
            'ο' to "o", 'π' to "p", 'ρ' to "r", 'σ' to "s", 'ς' to "s", 'τ' to "t", 'υ' to "y",
            'φ' to "f", 'χ' to "ch", 'ψ' to "ps", 'ω' to "o",
            // Accented vowels (monotonic).
            'ά' to "a", 'έ' to "e", 'ή' to "i", 'ί' to "i", 'ό' to "o", 'ύ' to "y", 'ώ' to "o",
            'ϊ' to "i", 'ϋ' to "y", 'ΐ' to "i", 'ΰ' to "y"
        )
        for ((lower, roman) in pairs) {
            put(lower, roman)
            val upper = lower.uppercaseChar()
            if (upper != lower) put(upper, roman.replaceFirstChar { it.uppercaseChar() })
        }
    }

    override fun romanize(text: String): String {
        val sb = StringBuilder(text.length * 2)
        for (c in text) sb.append(MAP[c] ?: c.toString())
        return sb.toString()
    }
}
