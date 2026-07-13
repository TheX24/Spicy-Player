package com.tx24.spicyplayer.lyrics.spicy.romanization

/** A writing system the romanizer can detect and transliterate. */
enum class Script { JAPANESE, CHINESE, KOREAN, CYRILLIC, GREEK, LATIN }

/**
 * Converts text of one [script] to a Latin transliteration. Implementations may be heavy
 * (dictionary-backed) and are created lazily and used off the main thread.
 */
interface Romanizer {
    val script: Script

    /** True if this romanizer's backing resources are available (e.g. optional library present). */
    fun isAvailable(): Boolean = true

    /** Romanizes [text]; returns the input unchanged if it cannot be converted. */
    fun romanize(text: String): String
}
