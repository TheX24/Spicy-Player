package com.tx24.spicyplayer.lyrics.spicy.models

import java.security.MessageDigest

fun lyricsDocumentId(songUri: String, source: String, rawLyrics: String): String {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(rawLyrics.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
    return "$songUri|$source|$hash"
}
