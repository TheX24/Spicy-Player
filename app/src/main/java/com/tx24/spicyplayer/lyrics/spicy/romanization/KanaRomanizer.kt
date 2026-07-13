package com.tx24.spicyplayer.lyrics.spicy.romanization

/**
 * Converts Japanese kana (hiragana/katakana) to Hepburn romaji. Handles youon (small ya/yu/yo),
 * sokuon (small っ gemination) and the長音 (ー) long-vowel mark. Kanji are left unchanged — a
 * dictionary tokenizer (Kuromoji) is needed for kanji readings and is layered on top when present.
 */
object KanaRomanizer {

    private val BASE: Map<Char, String> = buildMap {
        val entries = listOf(
            "あa","いi","うu","えe","おo",
            "かka","きki","くku","けke","こko",
            "がga","ぎgi","ぐgu","げge","ごgo",
            "さsa","しshi","すsu","せse","そso",
            "ざza","じji","ずzu","ぜze","ぞzo",
            "たta","ちchi","つtsu","てte","とto",
            "だda","ぢji","づzu","でde","どdo",
            "なna","にni","ぬnu","ねne","のno",
            "はha","ひhi","ふfu","へhe","ほho",
            "ばba","びbi","ぶbu","べbe","ぼbo",
            "ぱpa","ぴpi","ぷpu","ぺpe","ぽpo",
            "まma","みmi","むmu","めme","もmo",
            "やya","ゆyu","よyo",
            "らra","りri","るru","れre","ろro",
            "わwa","ゐwi","ゑwe","をwo","んn",
            "ぁa","ぃi","ぅu","ぇe","ぉo",
            "ゃya","ゅyu","ょyo","ゎwa","ー-"
        )
        for (e in entries) put(e[0], e.substring(1))
    }

    // Youon combinations: consonant stem of the base kana + small ya/yu/yo.
    private val YOUON_STEM: Map<Char, String> = mapOf(
        'き' to "k", 'ぎ' to "g", 'し' to "sh", 'じ' to "j", 'ち' to "ch", 'に' to "n",
        'ひ' to "h", 'び' to "b", 'ぴ' to "p", 'み' to "m", 'り' to "r"
    )
    // sh/ch/j stems take a bare vowel (sha, chu, jo); other stems insert a 'y' (kya, ryu, nyo).
    private val YOUON_VOWEL_PLAIN: Map<Char, String> = mapOf('ゃ' to "a", 'ゅ' to "u", 'ょ' to "o")
    private val YOUON_VOWEL_Y: Map<Char, String> = mapOf('ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo")
    private val PALATAL_STEMS = setOf("sh", "ch", "j")

    private fun toHiragana(c: Char): Char =
        if (c.code in 0x30A1..0x30F6) (c.code - 0x60).toChar() else c

    /** Romanizes any kana runs in [text]; non-kana characters pass through unchanged. */
    fun romanize(text: String): String {
        val sb = StringBuilder(text.length * 2)
        var i = 0
        var pendingGemination = false
        while (i < text.length) {
            val h = toHiragana(text[i])

            // Sokuon: small っ doubles the next romaji consonant.
            if (h == 'っ') { pendingGemination = true; i++; continue }

            // Youon: base kana + small ya/yu/yo.
            if (i + 1 < text.length) {
                val next = toHiragana(text[i + 1])
                val stem = YOUON_STEM[h]
                val vowel = if (stem in PALATAL_STEMS) YOUON_VOWEL_PLAIN[next] else YOUON_VOWEL_Y[next]
                if (stem != null && vowel != null) {
                    var syl = stem + vowel
                    if (pendingGemination) { syl = syl.first() + syl; pendingGemination = false }
                    sb.append(syl); i += 2; continue
                }
            }

            val roman = BASE[h]
            if (roman != null) {
                if (h == 'ー' && sb.isNotEmpty()) {
                    // Long-vowel mark: repeat the previous vowel letter.
                    val last = sb.last()
                    if (last in "aeiou") sb.append(last)
                } else {
                    var syl = roman
                    if (pendingGemination && syl.isNotEmpty() && syl.first() !in "aeiou") {
                        syl = syl.first() + syl
                    }
                    pendingGemination = false
                    sb.append(syl)
                }
            } else {
                if (pendingGemination) { sb.append('t'); pendingGemination = false }
                sb.append(text[i])
            }
            i++
        }
        if (pendingGemination) sb.append('t')
        return sb.toString()
    }
}

/** Kana-only Japanese romanizer; kanji pass through. Upgraded by [JapaneseRomanizer] when Kuromoji is present. */
object KanaOnlyJapaneseRomanizer : Romanizer {
    override val script = Script.JAPANESE
    override fun romanize(text: String): String = KanaRomanizer.romanize(text)
}
